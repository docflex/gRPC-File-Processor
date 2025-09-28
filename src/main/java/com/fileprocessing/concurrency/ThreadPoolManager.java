package com.fileprocessing.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized manager for thread pools in the file processing microservice.
 * Provides scalable, monitored executor services for concurrent task execution.
 * Features:
 * - Adaptive thread pool resizing
 * - Bounded queue with backpressure
 * - Monitoring hooks
 * - Safe shutdown
 */
@Slf4j
public final class ThreadPoolManager {

    // TODO: Make all of these configurable via application properties and hot-reloadable
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_CORES;
    private static final int MAX_POOL_SIZE = CPU_CORES * 4;
    private static final int QUEUE_CAPACITY = 200;
    private static final int QUEUE_THRESHOLD = 50; // triggers pool resize
    private static final long THREAD_KEEP_ALIVE_SECONDS = 60;
    // Singleton instance
    private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService monitor;

    private ThreadPoolManager() {
        this.executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                THREAD_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY), // bounded queue
                new FileProcessingThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy() // backpressure: slow down client
        );

        // Allow core threads to time out when idle
        executor.allowCoreThreadTimeOut(true);

        // Monitor pool and queue for adaptive resizing
        this.monitor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "ThreadPoolMonitor")
        );
        monitor.scheduleAtFixedRate(this::adjustPoolSize, 1, 1, TimeUnit.SECONDS);
    }

    public static ThreadPoolManager getInstance() {
        return INSTANCE;
    }

    /**
     * Submit a task to the thread pool.
     */
    public <T> @NotNull Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    public @NotNull Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    /**
     * Get the underlying executor.
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Get current queue size.
     */
    public int getQueueSize() {
        return executor.getQueue().size();
    }

    /**
     * Get the approximate number of active tasks.
     */
    public int getActiveCount() {
        return executor.getActiveCount();
    }

    /**
     * Gracefully shutdown executor and monitor.
     */
    public void shutdown() {
        monitor.shutdownNow();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Adaptive resizing logic based on queue size.
     */
    private void adjustPoolSize() {
        int queueSize = executor.getQueue().size();

        if (queueSize > QUEUE_THRESHOLD && executor.getMaximumPoolSize() < MAX_POOL_SIZE) {
            int newMax = Math.min(MAX_POOL_SIZE, executor.getMaximumPoolSize() + 2);
            executor.setMaximumPoolSize(newMax);
            executor.setCorePoolSize(newMax / 2);
            log.info("[ThreadPoolManager] Increased pool size to {}", newMax);
        } else if (queueSize < QUEUE_THRESHOLD / 2 && executor.getCorePoolSize() > CORE_POOL_SIZE) {
            int newCore = Math.max(CORE_POOL_SIZE, executor.getCorePoolSize() - 1);
            executor.setCorePoolSize(newCore);
            executor.setMaximumPoolSize(newCore * 2);
            log.info("[ThreadPoolManager] Decreased pool size to {}", newCore);
        }
    }

    /**
     * Custom thread factory for naming threads in this pool.
     */
    private static class FileProcessingThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r, "file-task-thread-" + counter.incrementAndGet());
            t.setDaemon(false);
            return t;
        }
    }
}
