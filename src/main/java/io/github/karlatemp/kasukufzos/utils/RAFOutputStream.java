package io.github.karlatemp.kasukufzos.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class RAFOutputStream extends OutputStream {
    private final RandomAccessFile raf;

    public RAFOutputStream(RandomAccessFile rafOs) {
        this.raf = rafOs;
    }

    @Override
    public void write(int b) throws IOException {
        raf.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        raf.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        raf.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
