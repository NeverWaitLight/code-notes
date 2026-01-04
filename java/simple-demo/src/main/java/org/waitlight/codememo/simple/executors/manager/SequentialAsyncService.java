package org.waitlight.codememo.simple.executors.manager;

import java.util.concurrent.CompletableFuture;

/**
 * 严格顺序性多线程服务
 *
 * @param <T> key type
 * @param <R> result type
 */
public interface SequentialAsyncService<T, R> {
    void execute(T key, Runnable r);

    CompletableFuture<R> submit(T key, Runnable r);
}
