/*
 * Copyright (c) 2018-2021 Karlatemp and contributors. All rights reserved.
 *
 * Use of this source code is governed by the MIT license that can be found via the following link.
 *
 * https://github.com/KasukuSakura/kasuku-fzos/blob/master/LICENSE
 */

package io.github.karlatemp.kasukufzos.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RAFInputStream extends InputStream {
    private final RandomAccessFile raf;

    public RAFInputStream(RandomAccessFile raf) {
        this.raf = raf;
    }

    @Override
    public int read() throws IOException {
        return raf.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return raf.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return raf.read(b);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
