package com.fileprocessing.model.concurrency;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks metrics for the file processing service.
 * Thread-safe and safe to update concurrently.
 */
@Service
public class FileProcessingMetrics {

    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final AtomicLong totalTaskDurationMillis = new AtomicLong(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);

    /**
     * Increment the number of active tasks.
     */
    public void incrementActiveTasks() {
        activeTasks.incrementAndGet();
    }

    /**
     * Decrement the number of active tasks.
     */
    public void decrementActiveTasks() {
        activeTasks.decrementAndGet();
    }

    /**
     * Add a completed task duration (in milliseconds) to the metrics.
     */
    public void addTaskDuration(long durationMillis) {
        totalTaskDurationMillis.addAndGet(durationMillis);
        completedTasks.incrementAndGet();
    }

    /**
     * Returns the current number of active tasks.
     */
    public int getActiveTasks() {
        return activeTasks.get();
    }

    /**
     * Returns the average task duration in milliseconds.
     * Returns 0 if no tasks have completed yet.
     */
    public long getAverageTaskDurationMillis() {
        int completed = completedTasks.get();
        if (completed == 0) return 0;
        return totalTaskDurationMillis.get() / completed;
    }

    @Override
    public String toString() {
        return "FileProcessingMetrics[activeTasks=" + getActiveTasks() +
                ", averageTaskDurationMillis=" + getAverageTaskDurationMillis() + "]";
    }
}
