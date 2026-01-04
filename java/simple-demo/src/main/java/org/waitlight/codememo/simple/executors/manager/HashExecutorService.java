package org.waitlight.codememo.simple.executors.manager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.waitlight.codememo.simple.NamedThreadFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HashExecutorService implements SequentialAsyncService<Integer, Void> {
    private static final Logger LOGGER = LogManager.getLogger(HashExecutorService.class);

    private static final int DEF_EXECUTORS_SIZE = 4;
    private static final double LOAD_FACTOR = 0.8D; // Queue满的阈值百分比
    private static final int EXECUTOR_QUEUE_CAPACITY = 10; // 每个线程池队列容量

    private final List<ThreadPoolExecutor> executors;
    private final AtomicInteger executorNum = new AtomicInteger(0);

    private final ReentrantReadWriteLock resizeLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock resizeProcessingLock = resizeLock.writeLock();
    private final ReentrantReadWriteLock.ReadLock executeLock = resizeLock.readLock();

    public HashExecutorService() {
        executors = new ArrayList<>(DEF_EXECUTORS_SIZE);
        for (int i = 0; i < DEF_EXECUTORS_SIZE; i++) {
            executors.add(createExecutor());
        }
    }

    private ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(EXECUTOR_QUEUE_CAPACITY),
                new NamedThreadFactory("sp-" + executorNum.getAndIncrement()),
                new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public void execute(Integer key, Runnable r) {
        boolean locked = false;
        if (resizeLock.isWriteLocked()) {
            executeLock.lock();
            locked = true;
        }

        int i = key & (executors.size() - 1);
        ThreadPoolExecutor executor = executors.get(i);
        if (executor.getQueue().size() > (int) (EXECUTOR_QUEUE_CAPACITY * LOAD_FACTOR)) {
            resize();
        }

        try {
            if (executor.isShutdown() || executor.isTerminating() || executor.isTerminated()) {
                execute(key, r);
            } else {
                executor.execute(r);
            }
        } catch (RejectedExecutionException re) {
            LOGGER.info("executor {} is shutdown {}, isTerminating {}, isTerminated {}", executor.getThreadFactory(),
                    executor.isShutdown(), executor.isTerminating(), executor.isTerminated());
            execute(key, r);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (locked) executeLock.unlock();
        }
    }

    @Override
    public CompletableFuture<Void> submit(Integer key, Runnable r) {
        boolean locked = false;
        if (resizeLock.isWriteLocked()) {
            executeLock.lock();
            locked = true;
        }

        int i = key & (executors.size() - 1);
        ThreadPoolExecutor executor = executors.get(i);
        if (executor.getQueue().size() > (int) (EXECUTOR_QUEUE_CAPACITY * LOAD_FACTOR)) {
            resize();
        }

        try {
            if (executor.isShutdown() || executor.isTerminating() || executor.isTerminated()) {
                execute(key, r);
            } else {
                executor.execute(r);
            }
        } catch (RejectedExecutionException re) {
            LOGGER.info("executor {} is shutdown {}, isTerminating {}, isTerminated {}", executor.getThreadFactory(),
                    executor.isShutdown(), executor.isTerminating(), executor.isTerminated());
            execute(key, r);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (locked) executeLock.unlock();
        }
        return CompletableFuture.completedFuture(null);
    }


    /**
     * {@link #executors} 扩容
     */
    private void resize() {
        if (!resizeProcessingLock.tryLock()) return;

        try {
            for (ThreadPoolExecutor executor : executors) {
                executor.shutdown();
                try {
                    executor.awaitTermination(60L, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            int oldSize = executors.size();
            executors.clear();
            int newSize = oldSize << 1;
            for (int i = 0; i < newSize; i++) {
                executors.add(createExecutor());
            }

            LOGGER.info("扩容完成，目前线程池总数 {}", executors.size());
            for (ThreadPoolExecutor executor : executors) {
                LOGGER.info("{} 线程池, 任务数 {}/{}", executor.getThreadFactory(), executor.getQueue().size(), EXECUTOR_QUEUE_CAPACITY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            resizeProcessingLock.unlock();
        }

    }

    public static void main(String[] args) {
        BitSet bitSet = new BitSet();
        bitSet.set(1, true);
        int total = 200;

        ThreadPoolExecutor producer = new ThreadPoolExecutor(
                10, 10, 0L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100000),
                new NamedThreadFactory("producer"));

        SequentialAsyncService<Integer, Void> service = new HashExecutorService();

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
