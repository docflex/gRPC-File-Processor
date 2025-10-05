package com.fileprocessing.model.concurrency;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a workflow of multiple file processing tasks.
 * Integrates with metrics for monitoring progress.
 */
public record FileWorkflow(
        String workflowId,
        List<FileTask> tasks,
        Instant submittedAt
) {

    public FileWorkflow(String workflowId, List<FileTask> tasks, Instant submittedAt) {
        Objects.requireNonNull(tasks, "tasks cannot be null");
        if (tasks.isEmpty()) throw new IllegalArgumentException("tasks cannot be empty");

        this.workflowId = workflowId != null ? workflowId : UUID.randomUUID().toString();
        this.tasks = List.copyOf(tasks);
        this.submittedAt = submittedAt != null ? submittedAt : Instant.now(Clock.systemUTC());
    }

    public static FileWorkflow of(List<FileTask> tasks) {
        return new FileWorkflow(null, tasks, Instant.now(Clock.systemUTC()));
    }

    /** Returns number of tasks in this workflow */
    public int totalTasks() {
        return tasks.size();
    }

    /** Returns number of completed tasks */
    public long completedTasks() {
        return tasks.stream().filter(FileTask::isDone).count();
    }

    /** Returns number of failed tasks */
    public long failedTasks() {
        return tasks.stream()
                .filter(t -> t.isDone() && t.futureResult().isCompletedExceptionally())
                .count();
    }

    @Override
    public String toString() {
        return String.format(
                "FileWorkflow[id=%s, tasks=%d, completed=%d, submittedAt=%s]",
                workflowId, totalTasks(), completedTasks(), submittedAt
        );
    }
}
