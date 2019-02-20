package com.icthh.xm.uaa.util;

import lombok.SneakyThrows;

import java.util.function.Consumer;

public final class GeneralUtils {

    private GeneralUtils() {
        throw new UnsupportedOperationException();
    }

    public static <T> Consumer<T> sneakyThrows(Task<T> consumer) {
        return t -> doWork(consumer, t);
    }

    @SneakyThrows
    private static <T> void doWork(Task<T> consumer, T t) {
        consumer.doWork(t);
    }

    @FunctionalInterface
    public interface Task<T> {
        void doWork(T t) throws Exception;
    }

}
