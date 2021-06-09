/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.image.writer;

import io.github.karlatemp.kasukufzos.utils.ETask;
import io.github.karlatemp.kasukufzos.utils.NonClosableStream;
import io.github.karlatemp.kasukufzos.utils.RAFOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

class KFzWriterImpl extends KFzWriter {
    // private final File out;
    private final File moduleOut;
    private final RandomAccessFile outOs;
    private final RandomAccessFile moduleOs;
    int moduleCount = 0;

    KFzWriterImpl(File out) throws Exception {
        // this.out = out;
        this.moduleOut = new File(out.getPath() + ".module.bin");

        outOs = new RandomAccessFile(out, "rw");
        moduleOs = new RandomAccessFile(moduleOut, "rw");
        outOs.writeInt(0);
    }

    @Override
    public void close() throws Exception {
        complete();
    }

    @Override
    public void writeModule(String name, ETask<KFZModuleWriter> moduleWriter) throws Exception {
        ModuleWriterImpl mwriter = mwriter(name);
        moduleWriter.run(mwriter);
        complete(mwriter);
    }

    static class Res {
        String path;
        long pointer;
        long endPointer;
        int modifiers;
    }

    static class ModuleWriterImpl extends KFZModuleWriter {
        private final RandomAccessFile os;
        private final OutputStream oos;
        List<Res> rsList = new ArrayList<>();

        ModuleWriterImpl(RandomAccessFile os) {
            this.os = os;
            this.oos = NonClosableStream.of(new BufferedOutputStream(new RAFOutputStream(os), 20480));
        }

        void writeFile(String path, int modifiers, ETask<OutputStream> os) throws Exception {
            while (path.charAt(0) == '/') path = path.substring(1);
            Res rs = new Res();
            rs.path = path;
            rs.modifiers = modifiers;
            oos.flush();
            rs.pointer = this.os.getFilePointer();
            os.run(oos);
            oos.flush();
            rs.endPointer = this.os.getFilePointer();
            rsList.add(rs);
        }

        @Override
        public void addResource(String path, int modifier, ETask<OutputStream> task) throws Exception {
            writeFile(path, modifier, task);
        }
    }

    ModuleWriterImpl mwriter(String name) throws Exception {
        moduleCount++;
        outOs.writeUTF(name);
        moduleOs.seek(0);
        return new ModuleWriterImpl(moduleOs);
    }

    void complete(ModuleWriterImpl writer) throws Exception {
        outOs.writeInt(writer.rsList.size());
        for (Res rs : writer.rsList) {
            outOs.writeUTF(rs.path);
            outOs.writeLong(rs.pointer);
            outOs.writeLong(rs.endPointer);
            outOs.writeInt(rs.modifiers);
        }
        copyTo(moduleOs, outOs);
    }

    private void copyTo(RandomAccessFile moduleOs, RandomAccessFile outOs) throws Exception {
        long size = moduleOs.getFilePointer();
        outOs.writeLong(size);
        moduleOs.seek(0);
        byte[] buffer = new byte[10240];
        while (size > 0) {
            int len = moduleOs.read(buffer);
            if (len == -1) break;
            if (len > size) len = (int) size;
            outOs.write(buffer, 0, len);
            size -= len;
        }
    }

    void complete() throws Exception {
        outOs.seek(0);
        outOs.writeInt(moduleCount);
        outOs.close();
        moduleOs.close();
        moduleOut.delete();
    }
}
