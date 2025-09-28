package com.fileprocessing.model.concurrency;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a workflow of multiple file processing tasks.
 * Immutable except for tasks themselves which are inherently asynchronous.
 */
public record FileWorkflow(String workflowId, List<FileTask> tasks, Instant submittedAt) {

    /**
     * Constructor with validation and defensive copy.
     *
     * @param workflowId  Unique workflow identifier. If null, a random UUID is generated.
     * @param tasks       List of file tasks to process (required, non-empty)
     * @param submittedAt Submission timestamp. Defaults to now if null.
     */
    public FileWorkflow(String workflowId, List<FileTask> tasks, Instant submittedAt) {
        Objects.requireNonNull(tasks, "tasks cannot be null");
        if (tasks.isEmpty()) {
            throw new IllegalArgumentException("tasks cannot be empty");
        }

        this.workflowId = (workflowId != null) ? workflowId : UUID.randomUUID().toString();
        this.tasks = List.copyOf(tasks);
        this.submittedAt = (submittedAt != null) ? submittedAt : Instant.now();
    }

    /**
     * Convenience factory for creating a workflow with a list of tasks.
     */
    public static FileWorkflow of(List<FileTask> tasks) {
        return new FileWorkflow(null, tasks, null);
    }

    /**
     * Returns the workflow ID.
     */
    @Override
    public String workflowId() {
        return workflowId;
    }

    /**
     * Returns the immutable list of tasks in this workflow.
     */
    @Override
    public List<FileTask> tasks() {
        return tasks;
    }

    /**
     * Returns the submission timestamp of this workflow.
     */
    @Override
    public Instant submittedAt() {
        return submittedAt;
    }

    @Override
    @NotNull
    public String toString() {
        return "FileWorkflow[workflowId=" + workflowId + ", tasks=" + tasks.size() + ", submittedAt=" + submittedAt + "]";
    }
}
