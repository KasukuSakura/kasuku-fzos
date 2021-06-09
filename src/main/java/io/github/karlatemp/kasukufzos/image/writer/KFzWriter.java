package io.github.karlatemp.kasukufzos.image.writer;

import io.github.karlatemp.kasukufzos.utils.ETask;

import java.io.File;
import java.io.OutputStream;

public abstract class KFzWriter {
    public static KFzWriter of(File file) throws Exception {
        return new KFzWriterImpl(file);
    }

    public abstract void close() throws Exception;

    public static abstract class KFZModuleWriter {
        public abstract void addResource(String path, int modifier, ETask<OutputStream> task) throws Exception;
    }

    public abstract void writeModule(String name, ETask<KFZModuleWriter> moduleWriter) throws Exception;
}
