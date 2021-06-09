/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.image.reader;

import io.github.karlatemp.kasukufzos.utils.EmptyStream;
import io.github.karlatemp.kasukufzos.utils.LimitedInputStream;
import io.github.karlatemp.kasukufzos.utils.RAFInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

class KFzReaderImpl extends KFzReader {

    private final File file;

    static class Node {
        String name;
        Node parent;
        Map<String, Node> children;
        int modifiers;

        long pointer, endPointer;
        KFzClassLoaderData.KClData data;
    }

    final Node root = new Node();

    KFzReaderImpl(File file) throws Exception {
        this.file = file;
        try (RandomAccessFile dis = new RandomAccessFile(file, "r")) {
            init(dis);
        }
    }

    void init(RandomAccessFile dataInput) throws Exception {
        root.name = "<root>";
        root.children = new HashMap<>();

        int moduleCount = dataInput.readInt();
        while (moduleCount-- > 0) {
            initModule(dataInput);
        }
    }

    void initModule(RandomAccessFile raf) throws Exception {
        String moduleName = raf.readUTF();
        if (root.children.containsKey(moduleName)) {
            throw new IllegalStateException("module " + moduleName + " duplicated in " + file);
        }
        int resourceCount = raf.readInt();
        Node module = new Node();
        module.name = moduleName;
        module.parent = root;
        module.children = new HashMap<>(resourceCount);
        root.children.put(moduleName, module);
        List<Node> resources = new ArrayList<>(resourceCount);
        while (resourceCount-- > 0) {
            Node res = new Node();
            res.name = raf.readUTF();

            while (!res.name.isEmpty() && res.name.charAt(0) == '/')
                res.name = res.name.substring(1);

            res.pointer = raf.readLong();
            res.endPointer = raf.readLong();
            res.modifiers = raf.readInt();
            resources.add(res);
        }
        long contentSize = raf.readLong();
        long baseOffset = raf.getFilePointer();
        for (Node res : resources) {
            res.pointer += baseOffset;
            res.endPointer += baseOffset;
            module.children.put(res.name, res);
            res.parent = module;
        }
        raf.seek(raf.getFilePointer() + contentSize);
    }

    Node rsNode(String path) {
        int index;
        if ((index = path.indexOf('/')) == -1) return null;
        if (path.charAt(0) == '/') return null;
        String module = path.substring(0, index);
        String path0 = path.substring(index + 1);
        return rsNode(module, path0);
    }

    @Override
    public InputStream getResource(String path) throws Exception {
        return resource(rsNode(path));
    }

    @Override
    public List<String> modules() {
        return new ArrayList<>(root.children.keySet());
    }

    @Override
    public List<String> paths(String module) {
        Node node = root.children.get(module);
        if (node == null) return null;
        return new ArrayList<>(node.children.keySet());
    }

    Node rsNode(String module, String path) {
        Node modules = root.children.get(module);
        if (modules == null) return null;
        return modules.children.get(path);
    }

    public InputStream resource(String module, String path) throws Exception {
        return resource(rsNode(module, path));
    }

    InputStream resource(Node res) throws Exception {
        if (res == null) return null;
        if (res.pointer == 0) return null;
        if (res.pointer == res.endPointer) return EmptyStream.EMPTY_INPUT_STREAM;
        RandomAccessFile stream = new RandomAccessFile(file, "r");
        try {
            stream.seek(res.pointer);
            InputStream base = new LimitedInputStream(
                    new RAFInputStream(stream),
                    res.endPointer - res.pointer
            );
            switch (res.modifiers & COMPRESS_TYPE_BITS) {
                case COMPRESS_TYPE_INF_:
                    base = new InflaterInputStream(base);
                    break;
                case COMPRESS_TYPE_GZIP:
                    base = new GZIPInputStream(base);
                    break;
            }
            return new BufferedInputStream(base);
        } catch (Throwable throwable) {
            try {
                stream.close();
            } catch (Throwable t2) {
                throwable.addSuppressed(t2);
            }
            throw throwable;
        }
    }
}
