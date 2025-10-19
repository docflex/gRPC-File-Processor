package com.fileprocessing.concurrency;

import com.fileprocessing.config.ThreadPoolProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

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
@Component
public final class ThreadPoolManager {

    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService monitor;
    private final ThreadPoolProperties properties;

    public ThreadPoolManager(ThreadPoolProperties properties) {
        this.properties = properties;

        log.info("Initializing ThreadPoolManager with coreSize={}, maxSize={}, queueCapacity={}, resizeThreshold={}, keepAliveSeconds={}, monitorIntervalSeconds={}",
                properties.getCoreSize(), properties.getMaxSize(), properties.getQueueCapacity(), properties.getResizeThreshold(),
                properties.getKeepAliveSeconds(), properties.getMonitorIntervalSeconds());

        // Use a bounded queue if queueCapacity > 0, otherwise SynchronousQueue
        BlockingQueue<Runnable> workQueue = properties.getQueueCapacity() > 0 ?
                new LinkedBlockingQueue<>(properties.getQueueCapacity()) :
                new SynchronousQueue<>();

        this.executor = new ThreadPoolExecutor(
                properties.getCoreSize(),
                properties.getMaxSize(),
                properties.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                workQueue,
                new FileProcessingThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // Keep core threads alive
        executor.allowCoreThreadTimeOut(false);

        this.monitor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "ThreadPoolMonitor")
        );
        monitor.scheduleAtFixedRate(this::adjustPoolSize,
                0,
                100,
                TimeUnit.MILLISECONDS
        );
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
     * Get current stats.
     */
    public int getQueueSize() {
        return executor.getQueue().size();
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public int getPoolSize() {
        return executor.getPoolSize();
    }

    public int getCorePoolSize() {
        return executor.getCorePoolSize();
    }

    public int getMaximumPoolSize() {
        return executor.getMaximumPoolSize();
    }

    public double getUtilization() {
        int max = executor.getMaximumPoolSize();
        return max == 0 ? 0 : ((double) executor.getActiveCount() / max);
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public int getLargestPoolSize() {
        return executor.getLargestPoolSize();
    }

    public long getTaskCount() {
        return executor.getTaskCount();
    }

    public long getCompletedTaskCount() {
        return executor.getCompletedTaskCount();
    }



    /**
     * Gracefully shutdown executor and monitor.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ThreadPoolManager...");
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
     * Ensures pool never drops below coreSize.
     */
    private void adjustPoolSize() {
        int active = executor.getActiveCount();
        int poolSize = executor.getPoolSize();
        int queueSize = executor.getQueue().size();
        int coreSize = properties.getCoreSize();
        int maxSize = properties.getMaxSize();
        int resizeThreshold = properties.getResizeThreshold();

        log.info("Active: {}, PoolSize: {}, Queue: {}", active, poolSize, queueSize);


        // Increase pool size if queue is getting full
        if (queueSize > resizeThreshold) {
            int newMax = Math.min(maxSize, executor.getMaximumPoolSize() + 2);
            executor.setMaximumPoolSize(newMax);
            executor.setCorePoolSize(Math.max(coreSize, newMax / 2));
            log.info("[ThreadPoolManager] Increased pool size to {}", newMax);
        }

        // Decrease pool size if queue is very low, but never below coreSize
        else if (queueSize < resizeThreshold / 2 && executor.getCorePoolSize() > coreSize) {
            int newCore = Math.max(coreSize, executor.getCorePoolSize() - 1);
            executor.setCorePoolSize(newCore);
            executor.setMaximumPoolSize(Math.max(newCore, executor.getMaximumPoolSize() - 1));
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
            Thread t = new Thread(r, "task-thread-" + counter.incrementAndGet());
            t.setDaemon(false);
            return t;
        }
    }
}
