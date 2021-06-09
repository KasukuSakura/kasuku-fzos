/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.utils;

import io.github.karlatemp.kasukufzos.image.reader.KFzReader;
import io.github.karlatemp.kasukufzos.image.writer.KFzWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TransferKit {
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj) {
        return (T) obj;
    }

    public static <T> Set<T> cast(Set<?> e) {
        return cast((Object) e);
    }

    public static <T> List<T> cast(List<?> e) {
        return cast((Object) e);
    }

    public static void transfer(ZipFile zip, String moduleName, KFzWriter writer) throws Exception {
        writer.writeModule(moduleName, moduleWriter -> {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                long size = entry.getSize();
                try (InputStream inputStream = zip.getInputStream(entry)) {
                    moduleWriter.addResource(entry.getName(), size >= 1024 ? KFzReader.COMPRESS_TYPE_INF_ : 0, ew -> {
                        OutputStream os;
                        if (size >= 1024) {
                            os = new DeflaterOutputStream(ew);
                        } else {
                            os = ew;
                        }
                        try (OutputStream owx = os) {
                            transfer(inputStream, owx);
                        }
                    });
                }
            }
        });
    }

    public static void transfer(InputStream inputStream, OutputStream out) throws IOException {
        byte[] buffer = new byte[2048];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
    }

    public static ByteBuffer read(InputStream resource) throws IOException {
        class BOX extends ByteArrayOutputStream {
            ByteBuffer a() {
                return ByteBuffer.wrap(this.buf, 0, this.count);
            }
        }
        BOX box = new BOX();
        transfer(resource, box);
        return box.a();
    }

    public static byte[] readAndClose(InputStream resource) throws IOException {
        try (InputStream os = resource) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            transfer(os, baos);
            return baos.toByteArray();
        }
    }
}
