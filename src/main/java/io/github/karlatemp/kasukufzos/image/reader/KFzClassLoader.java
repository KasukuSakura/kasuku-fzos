/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.image.reader;

import io.github.karlatemp.kasukufzos.utils.IteratorE;
import io.github.karlatemp.kasukufzos.utils.TransferKit;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;

public class KFzClassLoader extends SecureClassLoader {
    final KFzClassLoaderData image;
    final Options options;

    public static class Options {
        public SignAction signAction = SignAction.SKIP_SIGN;

        public enum SignAction {
            SKIP_SIGN,
            CONTINUE_WHEN_ERROR,
            THROW_ON_FAILURE;
        }
    }

    public KFzClassLoader(ClassLoader parent, KFzReader image) throws Exception {
        this(parent, image, new Options());
    }

    public KFzClassLoader(ClassLoader parent, KFzReader image, Options options) throws Exception {
        super(parent);
        if (options == null) options = new Options();
        this.options = options;
        this.image = new KFzClassLoaderData(image, this);
    }

    void definePackage0(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) {
        super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
    }

    @Override
    protected URL findResource(String name) {
        KFzReaderImpl.Node node = image.findResource(name);
        if (node != null) {
            try {
                return new URL(image.rootURL, "/" + node.parent.name + '/' + node.name);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        ArrayList<URL> urlC = new ArrayList<>();
        try {
            for (KFzReaderImpl.Node module : image.reader.root.children.values()) {
                KFzReaderImpl.Node res = module.children.get(name);
                if (res != null) {
                    URL u = new URL(image.rootURL, "/" + module.name + '/' + res.name);
                    urlC.add(u);
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return new IteratorE<>(urlC.iterator());
    }

    @SuppressWarnings("deprecation")
    Package pkg(String pkg) {
        return getPackage(pkg);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        try {
            return image.resource(image.findResource(name));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        KFzReaderImpl.Node node = image.findResAndPreDefinePackage(name.replace('.', '/') + ".class");
        if (node != null) {
            KFzClassLoaderData.KClData data = node.data;
            try {
                return defineClass(name, TransferKit.read(image.resource(node)), data == null ? node.parent.data.cs : data.cs);
            } catch (Exception e) {
                throw new ClassNotFoundException(name, e);
            }
        }
        return super.findClass(name);
    }
}
