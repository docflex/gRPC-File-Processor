package com.fileprocessing.concurrency;

import com.fileprocessing.FileSpec.OperationStatus;
import com.fileprocessing.FileSpec.OperationType;
import com.fileprocessing.model.*;
import com.fileprocessing.model.concurrency.FileProcessingMetrics;
import com.fileprocessing.model.concurrency.FileTask;
import com.fileprocessing.model.concurrency.FileWorkflow;
import com.fileprocessing.util.FileOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Executes file operations concurrently using a ThreadPoolManager.
 * Tracks per-task metrics and ensures error isolation for failed tasks.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WorkflowExecutorService {

    private final ThreadPoolManager threadPoolManager;
    private final FileProcessingMetrics processingMetrics;

    /**
     * Process all files and operations in the workflow request concurrently.
     *
     * @param requestModel The internal request containing files and operations
     * @return Summary of processing results
     */
    public FileProcessingSummaryModel processWorkflow(FileProcessingRequestModel requestModel) {
        List<FileTask> tasks = buildFileTasks(requestModel);

        if (tasks.isEmpty()) {
            return FileProcessingSummaryModel.builder()
                    .totalFiles(0)
                    .successfulFiles(0)
                    .failedFiles(0)
                    .results(Collections.emptyList())
                    .build();
        }

        FileWorkflow workflow = FileWorkflow.of(tasks);
        log.info("Submitting workflow {} with {} tasks", workflow.workflowId(), tasks.size());

        // Submit all tasks and attach error handling
        List<CompletableFuture<FileOperationResultModel>> futures = tasks.stream()
                .map(task -> submitTask(task)
                        .exceptionally(ex -> createFailedResult(
                                task.getFile().fileId(),
                                task.getOperation().operationType(),
                                ex)))
                .toList();


        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Collect results
        List<FileOperationResultModel> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        long successCount = results.stream()
                .filter(r -> r.status() == OperationStatus.SUCCESS)
                .count();

        return FileProcessingSummaryModel.builder()
                .totalFiles(requestModel.files().size())
                .successfulFiles((int) successCount)
                .failedFiles(results.size() - (int) successCount)
                .results(results)
                .build();
    }

    /**
     * Build a list of tasks for all files and their operations.
     */
    private List<FileTask> buildFileTasks(FileProcessingRequestModel req) {
        List<FileTask> tasks = new ArrayList<>();
        for (FileModel file : req.files()) {
            var ops = req.fileSpecificOperations().getOrDefault(file.fileId(), req.defaultOperations());

            if (ops.isEmpty()) {
                // TODO: Decide on default operation for files with no requested operations (e.g., VALIDATE)
            }

            for (var op : ops) {
                FileOperation fileOperation = FileOperation.builder()
                        .operationType(op)
                        .parameters(Map.of()) // TODO: Add configurable parameters per operation if needed
                        .build();
                tasks.add(new FileTask(file, fileOperation));
            }
        }
        return tasks;
    }

    /**
     * Submit a task to the thread pool and return a CompletableFuture for its result.
     * Metrics are tracked, and failures are isolated per task.
     */
    private CompletableFuture<FileOperationResultModel> submitTask(FileTask task) {
        processingMetrics.incrementActiveTasks();
        CompletableFuture<FileOperationResultModel> future = task.getFutureResult();

        threadPoolManager.submit(() -> {
            long start = System.currentTimeMillis();
            try {
                FileOperationResultModel result = executeOperation(
                        task.getFile(),
                        task.getOperation().operationType());
                future.complete(result);
            } catch (Throwable t) {
                log.error("Task failed for file {} op {}: {}",
                        task.getFile().fileId(),
                        task.getOperation().operationType(),
                        t.getMessage(), t);
                processingMetrics.incrementFailedTasks();
                future.completeExceptionally(t);
            } finally {
                long duration = System.currentTimeMillis() - start;
                processingMetrics.addTaskDuration(duration);
                processingMetrics.decrementActiveTasks();
            }
        });

        return future;
    }

    /**
     * Executes a file operation. This currently calls mocked utility methods.
     * TODO: Replace mocks with real services for each operation type.
     */
    private FileOperationResultModel executeOperation(FileModel file, OperationType operation) {
        Instant start = Instant.now();

        try {
            log.info("Executing {} on file {}", operation, file.fileName());

            switch (operation) {
                case VALIDATE -> FileOperations.validateFile(file);
                case METADATA_EXTRACTION -> FileOperations.extractMetadata(file);
                case OCR_TEXT_EXTRACTION -> FileOperations.performOcr(file);
                case IMAGE_RESIZE -> FileOperations.resizeImage(file, 800, 600); // Default max dimensions
                case FILE_COMPRESSION -> FileOperations.compressFile(file);
                case FORMAT_CONVERSION -> FileOperations.convertFormat(file, "jpg"); // Default format
                case STORAGE -> FileOperations.storeFile(file);
                default -> log.warn("Unknown operation: {}, skipping", operation);
            }

            return FileOperationResultModel.builder()
                    .fileId(file.fileId())
                    .operationType(operation)
                    .status(OperationStatus.SUCCESS)
                    .details("Operation completed successfully")
                    .startTime(start)
                    .endTime(Instant.now())
                    .resultLocation("/mock/location/" + file.fileName())
                    .build();

        } catch (Exception e) {
            log.error("Operation {} failed on file {}: {}", operation, file.fileName(), e.getMessage(), e);

            return FileOperationResultModel.builder()
                    .fileId(file.fileId())
                    .operationType(operation)
                    .status(OperationStatus.FAILED)
                    .details("Error: " + e.getMessage())
                    .startTime(start)
                    .endTime(Instant.now())
                    .resultLocation("")
                    .build();
        }
    }

    /**
     * Create a failed result for a task that threw an exception.
     */
    private FileOperationResultModel createFailedResult(String fileId, OperationType op, Throwable cause) {
        return FileOperationResultModel.builder()
                .fileId(fileId)
                .operationType(op != null ? op : OperationType.UNKNOWN)
                .status(OperationStatus.FAILED)
                .details("Error: " + cause.getMessage())
                .startTime(Instant.now())
                .endTime(Instant.now())
                .resultLocation("")
                .build();
    }
}
