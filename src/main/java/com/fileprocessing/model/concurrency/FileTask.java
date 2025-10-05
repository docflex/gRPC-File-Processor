package com.fileprocessing.model.concurrency;

import com.fileprocessing.model.FileModel;
import com.fileprocessing.model.FileOperation;
import com.fileprocessing.model.FileOperationResultModel;
import com.fileprocessing.service.monitoring.FileProcessingMetrics;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a single asynchronous file processing task.
 * <p>
 *  Each task tracks its completion state and provides a {@link CompletableFuture} for the result.
 *  Metrics are automatically updated upon successful or exceptional completion.
 * </p>
 * <p>
 *  Immutable except for the completion state and the future result.
 *  Designed for use in concurrent workflows where multiple tasks may complete in any order.
 * </p>
 */
public final class FileTask {

    /** The file to be processed. Cannot be null. */
    private final FileModel file;

    /** The operation to perform on the file. Cannot be null. */
    private final FileOperation operation;

    /** Future representing the asynchronous result of this task. */
    private final CompletableFuture<FileOperationResultModel> futureResult;

    /** Tracks whether the task has already completed to enforce idempotency. */
    private final AtomicBoolean completed = new AtomicBoolean(false);

    /**
     * Constructs a new FileTask.
     *
     * @param file      the file to process; must not be null
     * @param operation the operation to perform; must not be null
     * @throws NullPointerException if file or operation is null
     */
    public FileTask(FileModel file, FileOperation operation) {
        this.file = Objects.requireNonNull(file, "file cannot be null");
        this.operation = Objects.requireNonNull(operation, "operation cannot be null");
        this.futureResult = new CompletableFuture<>();
    }

    /** @return the file associated with this task */
    public FileModel file() {
        return file;
    }

    /** @return the operation associated with this task */
    public FileOperation operation() {
        return operation;
    }

    /**
     * @return a CompletableFuture representing the result of this task
     */
    public CompletableFuture<FileOperationResultModel> futureResult() {
        return futureResult;
    }

    /** @return true if the task has already been completed (successfully or exceptionally) */
    public boolean isDone() {
        return completed.get();
    }

    /**
     * Marks the task as successfully completed and updates metrics.
     * <p>
     * This method is idempotent; subsequent calls after the first completion are ignored.
     * </p>
     *
     * @param result         the result of the task; must not be null
     * @param metrics        the metrics tracker to update
     * @param durationMillis duration of the task in milliseconds
     * @throws NullPointerException if result is null
     */
    public void complete(FileOperationResultModel result, FileProcessingMetrics metrics, long durationMillis) {
        if (completed.compareAndSet(false, true)) {
            futureResult.complete(Objects.requireNonNull(result, "result cannot be null"));
            metrics.recordTaskCompletion(durationMillis);
        }
    }

    /**
     * Marks the task as failed (exceptionally completed) and updates metrics.
     * <p>
     * This method is idempotent; subsequent calls after the first completion are ignored.
     * </p>
     *
     * @param ex             the exception that caused failure; must not be null
     * @param metrics        the metrics tracker to update
     * @param durationMillis duration of the task in milliseconds
     * @throws NullPointerException if ex is null
     */
    public void completeExceptionally(Throwable ex, FileProcessingMetrics metrics, long durationMillis) {
        if (completed.compareAndSet(false, true)) {
            futureResult.completeExceptionally(Objects.requireNonNull(ex, "ex cannot be null"));
            metrics.incrementFailedTasks();
            metrics.recordTaskCompletion(durationMillis);
        }
    }

    @Override
    public String toString() {
        return "FileTask[fileId=" + file.fileId() + ", operation=" + operation + ", done=" + completed + "]";
    }
}
