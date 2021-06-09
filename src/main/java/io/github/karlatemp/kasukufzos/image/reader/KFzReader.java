package io.github.karlatemp.kasukufzos.image.reader;

import java.io.File;
import java.io.InputStream;
import java.util.List;


public abstract class KFzReader {
    public static final int
            COMPRESS_TYPE_BITS = 0b0000_0000_0000_0011,
            COMPRESS_TYPE_GZIP = 0b0000_0000_0000_0001,
            COMPRESS_TYPE_NONE = 0b0000_0000_0000_0000,
            COMPRESS_TYPE_INF_ = 0b0000_0000_0000_0010,
            VOID = 0;

    public static KFzReader from(File file) throws Exception {
        return new KFzReaderImpl(file);
    }

    public abstract InputStream getResource(String path) throws Exception;

    public abstract List<String> modules();

    public abstract List<String> paths(String module);

    public abstract InputStream resource(String module, String path) throws Exception;
}
