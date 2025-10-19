package com.fileprocessing.service.monitoring;

import com.fileprocessing.concurrency.ThreadPoolManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Getter
@Service
public class ThreadPoolMetrics {

    private final ThreadPoolManager threadPoolManager;

    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicInteger poolSize = new AtomicInteger(0);
    private final AtomicInteger largestPoolSize = new AtomicInteger(0);
    private final AtomicLong completedTasks = new AtomicLong(0);
    private final AtomicLong totalTasksSubmitted = new AtomicLong(0);

    public ThreadPoolMetrics(ThreadPoolManager threadPoolManager, MeterRegistry registry) {
        this.threadPoolManager = threadPoolManager;

        Gauge.builder("fileprocessing.threadpool.active", activeThreads, AtomicInteger::get)
                .description("Active threads in the file processing pool")
                .register(registry);

        Gauge.builder("fileprocessing.threadpool.queue", queueSize, AtomicInteger::get)
                .description("Current queue size for the file processing thread pool")
                .register(registry);

        Gauge.builder("fileprocessing.threadpool.size", poolSize, AtomicInteger::get)
                .description("Current pool size of the file processing thread pool")
                .register(registry);

        Gauge.builder("fileprocessing.threadpool.largest", largestPoolSize, AtomicInteger::get)
                .description("Largest pool size reached by the file processing thread pool")
                .register(registry);

        Gauge.builder("fileprocessing.threadpool.completed", completedTasks, AtomicLong::get)
                .description("Number of completed tasks in the file processing thread pool")
                .register(registry);

        Gauge.builder("fileprocessing.threadpool.submitted_total", totalTasksSubmitted, AtomicLong::get)
                .description("Total number of tasks submitted to the file processing thread pool")
                .register(registry);
    }

    // ----------- Update Methods (called periodically or from ThreadPoolManager) -----------

    public void refresh() {
        activeThreads.set(threadPoolManager.getActiveCount());
        queueSize.set(threadPoolManager.getQueueSize());
        poolSize.set(threadPoolManager.getPoolSize());
        largestPoolSize.set(threadPoolManager.getLargestPoolSize());
        completedTasks.set(threadPoolManager.getCompletedTaskCount());
        totalTasksSubmitted.set(threadPoolManager.getTaskCount());
    }

    // ----------- Metric Accessors -----------

    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("activeThreads", activeThreads.get());
        map.put("queueSize", queueSize.get());
        map.put("poolSize", poolSize.get());
        map.put("largestPoolSize", largestPoolSize.get());
        map.put("completedTasks", completedTasks.get());
        map.put("totalTasksSubmitted", totalTasksSubmitted.get());
        return map;
    }

    @Override
    public String toString() {
        return "ThreadPoolMetrics{" +
                "activeThreads=" + activeThreads.get() +
                ", queueSize=" + queueSize.get() +
                ", poolSize=" + poolSize.get() +
                ", largestPoolSize=" + largestPoolSize.get() +
                ", completedTasks=" + completedTasks.get() +
                ", totalTasksSubmitted=" + totalTasksSubmitted.get() +
                '}';
    }
}
