package io.github.karlatemp.kasukufzos.utils;

import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends InputStream {
    private final InputStream delegate;
    private long size;

    public LimitedInputStream(InputStream delegate, long size) {
        this.delegate = delegate;
        this.size = size;
    }

    @Override
    public int read() throws IOException {
        if (size == 0) return -1;
        int rd = delegate.read();
        if (rd == -1) {
            size = 0;
        } else {
            size--;
        }
        return rd;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (size == 0) return -1;

        long lenLong = len;
        if (lenLong < 0) throw new IllegalArgumentException("length < 0");
        if (lenLong > len) {
            len = (int) size;
        }

        int rd = delegate.read(b, off, len);
        if (rd == -1) {
            size = 0;
            return -1;
        }
        size -= rd;
        if (size < 0) {
            rd += (int) size;
            size = 0;
        }
        return rd;
    }

    @Override
    public void close() throws IOException {
        size = 0;
        delegate.close();
    }
}
