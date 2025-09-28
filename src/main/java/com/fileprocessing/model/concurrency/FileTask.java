package com.fileprocessing.model.concurrency;

import com.fileprocessing.model.FileModel;
import com.fileprocessing.model.FileOperation;
import com.fileprocessing.model.FileOperationResultModel;
import lombok.Getter;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a single asynchronous task to process a file with a specific operation.
 * Immutable except for the future result, which is completed asynchronously.
 */
@Getter
public final class FileTask {

    private final FileModel file;
    private final FileOperation operation;
    private final CompletableFuture<FileOperationResultModel> futureResult;

    /**
     * Constructor with validation.
     *
     * @param file      The file to process (required)
     * @param operation The operation to perform on the file (required)
     */
    public FileTask(FileModel file, FileOperation operation) {
        this.file = Objects.requireNonNull(file, "file cannot be null");
        this.operation = Objects.requireNonNull(operation, "operation cannot be null");
        this.futureResult = new CompletableFuture<>();
    }

    /**
     * Convenience method to complete the task successfully.
     */
    public void complete(FileOperationResultModel result) {
        futureResult.complete(Objects.requireNonNull(result, "result cannot be null"));
    }

    /**
     * Convenience method to complete the task exceptionally.
     */
    public void completeExceptionally(Throwable ex) {
        futureResult.completeExceptionally(Objects.requireNonNull(ex, "ex cannot be null"));
    }

    @Override
    public String toString() {
        return "FileTask[fileId=" + file.fileId() + ", operation=" + operation.operationType() + "]";
    }
}
