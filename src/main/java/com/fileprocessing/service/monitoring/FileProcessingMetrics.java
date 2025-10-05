package com.fileprocessing.service.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import lombok.Getter;
import lombok.ToString;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Getter
public class FileProcessingMetrics {

    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicLong totalTaskDurationMillis = new AtomicLong(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger failedTasks = new AtomicInteger(0);

    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private final AtomicLong totalRequestDurationMillis = new AtomicLong(0);
    private final AtomicInteger completedRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);

    public FileProcessingMetrics(MeterRegistry registry) {
        // Task-level
        Gauge.builder("fileprocessing.tasks.active", activeTasks, AtomicInteger::get).register(registry);
        Gauge.builder("fileprocessing.tasks.completed", completedTasks, AtomicInteger::get).register(registry);
        Gauge.builder("fileprocessing.tasks.failed", failedTasks, AtomicInteger::get).register(registry);
        Gauge.builder("fileprocessing.tasks.avg_duration_ms", this, FileProcessingMetrics::getAverageTaskDurationMillis).register(registry);

        // Request-level
        Gauge.builder("fileprocessing.requests.active", activeRequests, AtomicInteger::get).register(registry);
        Gauge.builder("fileprocessing.requests.completed", completedRequests, AtomicInteger::get).register(registry);
        Gauge.builder("fileprocessing.requests.failed", failedRequests, AtomicInteger::get).register(registry);
        Gauge.builder("fileprocessing.requests.avg_duration_ms", this, FileProcessingMetrics::getAverageRequestDurationMillis).register(registry);
    }

    // ------------ Task methods ------------
    public void incrementActiveTasks() { activeTasks.incrementAndGet(); }
    public void decrementActiveTasks() { if (activeTasks.get() > 0) activeTasks.decrementAndGet(); }
    public void recordTaskCompletion(long durationMillis) {
        totalTaskDurationMillis.addAndGet(durationMillis);
        completedTasks.incrementAndGet();
    }
    public void incrementFailedTasks() { failedTasks.incrementAndGet(); }

    public long getAverageTaskDurationMillis() {
        int completed = completedTasks.get();
        return completed == 0 ? 0 : totalTaskDurationMillis.get() / completed;
    }

    // ------------ Request methods ------------
    public void incrementActiveRequests() { activeRequests.incrementAndGet(); }
    public void decrementActiveRequests() { if (activeRequests.get() > 0) activeRequests.decrementAndGet(); }
    public void recordRequestCompletion(long durationMillis) {
        totalRequestDurationMillis.addAndGet(durationMillis);
        completedRequests.incrementAndGet();
    }
    public void incrementFailedRequests() { failedRequests.incrementAndGet(); }

    public long getAverageRequestDurationMillis() {
        int completed = completedRequests.get();
        return completed == 0 ? 0 : totalRequestDurationMillis.get() / completed;
    }

    // ------------ Utility methods ------------

    // Tasks
    public int getActiveTasks() { return activeTasks.get(); }
    public int getCompletedTasks() { return completedTasks.get(); }
    public int getFailedTasks() { return failedTasks.get(); }

    // Requests
    public int getActiveRequests() { return activeRequests.get(); }
    public int getCompletedRequests() { return completedRequests.get(); }
    public int getFailedRequests() { return failedRequests.get(); }


    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("activeTasks", getActiveTasks());
        map.put("completedTasks", getCompletedTasks());
        map.put("failedTasks", getFailedTasks());
        map.put("averageTaskDurationMillis", getAverageTaskDurationMillis());
        map.put("activeRequests", getActiveRequests());
        map.put("completedRequests", getCompletedRequests());
        map.put("failedRequests", getFailedRequests());
        map.put("averageRequestDurationMillis", getAverageRequestDurationMillis());
        return map;
    }

    public void reset() {
        activeTasks.set(0);
        completedTasks.set(0);
        failedTasks.set(0);
        totalTaskDurationMillis.set(0);

        activeRequests.set(0);
        completedRequests.set(0);
        failedRequests.set(0);
        totalRequestDurationMillis.set(0);
    }

    @Override
    public String toString() {
        return "FileProcessingMetrics{" +
                "activeTasks=" + getActiveTasks() +
                ", completedTasks=" + getCompletedTasks() +
                ", failedTasks=" + getFailedTasks() +
                ", averageTaskDurationMillis=" + getAverageTaskDurationMillis() +
                ", activeRequests=" + getActiveRequests() +
                ", completedRequests=" + getCompletedRequests() +
                ", failedRequests=" + getFailedRequests() +
                ", averageRequestDurationMillis=" + getAverageRequestDurationMillis() +
                '}';
    }

}
