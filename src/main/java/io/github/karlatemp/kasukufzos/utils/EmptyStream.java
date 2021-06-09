package io.github.karlatemp.kasukufzos.utils;

import java.io.IOException;
import java.io.InputStream;

public class EmptyStream {
    public static final InputStream EMPTY_INPUT_STREAM = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }

        public byte[] readAllBytes() throws IOException {
            return new byte[0];
        }

        public byte[] readNBytes(int len) throws IOException {
            return new byte[0];
        }

        @Override
        public int read(byte[] b) throws IOException {
            return -1;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return -1;
        }

        public int readNBytes(byte[] b, int off, int len) throws IOException {
            return -1;
        }
    };
}
