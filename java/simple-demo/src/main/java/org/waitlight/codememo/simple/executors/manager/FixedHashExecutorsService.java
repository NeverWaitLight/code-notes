package org.waitlight.codememo.simple.executors.manager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.waitlight.codememo.simple.NamedThreadFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class FixedHashExecutorsService implements SequentialAsyncService<Integer, Void> {
    private static final Logger LOGGER = LogManager.getLogger(FixedHashExecutorsService.class);

    /* 线程池等配置都是固定的，这个参数需要压测后调试 */
    private static final int EXECUTORS_SIZE = 2;
    private static final int EXECUTOR_QUEUE_CAPACITY = 2000;

    private final List<ThreadPoolExecutor> executors;
    private final AtomicInteger executorNum = new AtomicInteger(0);

    public FixedHashExecutorsService() {
        executors = new ArrayList<>(EXECUTORS_SIZE);
        for (int i = 0; i < EXECUTORS_SIZE; i++) {
            executors.add(createExecutor());
        }
    }

    private ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(EXECUTOR_QUEUE_CAPACITY),
                new NamedThreadFactory("sp-" + executorNum.getAndIncrement()),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public void execute(Integer key, Runnable r) {
        int i = key & (executors.size() - 1);
        ThreadPoolExecutor executor = executors.get(i);

        try {
            executor.execute(r);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> submit(Integer key, Runnable r) {
        int i = key & (executors.size() - 1);
        ThreadPoolExecutor executor = executors.get(i);

        try {
            return CompletableFuture.runAsync(r, executor);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }

    public static void main(String[] args) {
        int total = 200;

        ThreadPoolExecutor producer = new ThreadPoolExecutor(
                10, 10, 0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100000),
                new NamedThreadFactory("producer"));

        SequentialAsyncService<Integer, Void> service = new FixedHashExecutorsService();

        for (int i = 0; i < total; i++) {
            int finalI = i;
            producer.submit(() -> service.execute(finalI, () -> {
                try {
                    Thread.sleep(Duration.ofMillis(ThreadLocalRandom.current().nextInt(50, 100)));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LOGGER.info("task {} done", finalI);
            }));
        }
    }

}
