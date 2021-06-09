package io.github.karlatemp.kasukufzos.utils;

import java.io.*;

public class NonClosableStream {
    public static InputStream of(InputStream is) {
        return new FilterInputStream(is) {
            @Override
            public void close() throws IOException {
            }
        };
    }

    public static OutputStream of(OutputStream os) {
        return new FilterOutputStream(os) {
            @Override
            public void close() throws IOException {
            }
        };
    }
}
