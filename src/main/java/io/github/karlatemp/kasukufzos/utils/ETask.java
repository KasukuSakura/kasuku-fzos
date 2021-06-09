package io.github.karlatemp.kasukufzos.utils;

public interface ETask<T> {
    void run(T arg) throws Exception;
}
