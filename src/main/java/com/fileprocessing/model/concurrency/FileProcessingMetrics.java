package com.fileprocessing.model.concurrency;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks metrics for the file processing service.
 * Thread-safe and safe to update concurrently.
 * Metrics are separated into:
 * - Task-level metrics: per individual file operation
 * - Request-level metrics: per gRPC request / workflow
 */
@Service
public class FileProcessingMetrics {

    // -------------------------
    // Task-level metrics
    // -------------------------
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicLong totalTaskDurationMillis = new AtomicLong(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger failedTasks = new AtomicInteger(0);

    // -------------------------
    // Request-level metrics
    // -------------------------
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    private final AtomicLong totalRequestDurationMillis = new AtomicLong(0);
    private final AtomicInteger completedRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);

    // -------------------------
    // Task-level methods
    // -------------------------
    public void incrementActiveTasks() {
        activeTasks.incrementAndGet();
    }

    public void decrementActiveTasks() {
        activeTasks.decrementAndGet();
    }

    public void addTaskDuration(long durationMillis) {
        totalTaskDurationMillis.addAndGet(durationMillis);
        completedTasks.incrementAndGet();
    }

    public void incrementFailedTasks() {
        failedTasks.incrementAndGet();
    }

    public int getActiveTasks() {
        return activeTasks.get();
    }

    public int getCompletedTasks() {
        return completedTasks.get();
    }

    public int getFailedTasks() {
        return failedTasks.get();
    }

    public long getAverageTaskDurationMillis() {
        int completed = completedTasks.get();
        return completed == 0 ? 0 : totalTaskDurationMillis.get() / completed;
    }

    // -------------------------
    // Request-level methods
    // -------------------------
    public void incrementActiveRequests() {
        activeRequests.incrementAndGet();
    }

    public void decrementActiveRequests() {
        activeRequests.decrementAndGet();
    }

    public void addRequestDuration(long durationMillis) {
        totalRequestDurationMillis.addAndGet(durationMillis);
        completedRequests.incrementAndGet();
    }

    public void incrementFailedRequests() {
        failedRequests.incrementAndGet();
    }

    public int getActiveRequests() {
        return activeRequests.get();
    }

    public int getCompletedRequests() {
        return completedRequests.get();
    }

    public int getFailedRequests() {
        return failedRequests.get();
    }

    public long getAverageRequestDurationMillis() {
        int completed = completedRequests.get();
        return completed == 0 ? 0 : totalRequestDurationMillis.get() / completed;
    }

    /**
     * Reset all metrics to 0. Useful for testing or dashboard refresh.
     */
    public void reset() {
        activeTasks.set(0);
        totalTaskDurationMillis.set(0);
        completedTasks.set(0);
        failedTasks.set(0);
        activeRequests.set(0);
        totalRequestDurationMillis.set(0);
        completedRequests.set(0);
        failedRequests.set(0);
    }

    /**
     * Returns all metrics as a map for REST endpoints or logging.
     */
    public Map<String, Object> asMap() {
        return Map.of(
                "activeTasks", getActiveTasks(),
                "completedTasks", getCompletedTasks(),
                "failedTasks", getFailedTasks(),
                "averageTaskDurationMillis", getAverageTaskDurationMillis(),
                "activeRequests", getActiveRequests(),
                "completedRequests", getCompletedRequests(),
                "failedRequests", getFailedRequests(),
                "averageRequestDurationMillis", getAverageRequestDurationMillis()
        );
    }

    @Override
    public String toString() {
        return "FileProcessingMetrics" + asMap();
    }
}
