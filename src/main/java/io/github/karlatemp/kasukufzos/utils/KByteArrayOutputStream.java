package io.github.karlatemp.kasukufzos.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

public class KByteArrayOutputStream extends ByteArrayOutputStream {
    public KByteArrayOutputStream() {
    }

    public KByteArrayOutputStream(int size) {
        super(size);
    }

    public synchronized String toString(Charset charset) {
        return new String(buf, 0, count, charset);
    }
}
