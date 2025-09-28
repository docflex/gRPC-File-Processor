package com.fileprocessing.service.grpc;

import com.fileprocessing.FileSpec.OperationStatus;
import com.fileprocessing.FileSpec.OperationType;
import com.fileprocessing.model.FileModel;
import com.fileprocessing.model.FileOperationResultModel;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.model.FileProcessingSummaryModel;
import com.fileprocessing.util.FileOperations;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for handling unary file processing requests (gRPC ProcessFile).
 *
 * <p>NOTE:
 * - This service processes all files and operations synchronously.
 * - For the unary RPC, WorkflowExecutorService is NOT required because we process everything
 *   in the call thread and return a summary directly.
 * - WorkflowExecutorService becomes useful when:
 *      - Asynchronous or parallel execution of multiple files is needed
 *      - You want to reuse task orchestration for streaming/bidirectional RPCs
 *      - Metrics tracking, backpressure, or futures management is required
 *
 * <p>This design keeps the unary RPC simple, blocking until all files are processed.
 */
@Slf4j
@Service
public class ProcessFileService {

    /**
     * Process the files in the given request model.
     *
     * <p>NOTE:
     * Currently this executes synchronously for all files and operations.
     * To scale or run operations in parallel, you can:
     * 1. Wrap each file/operation in a FileTask.
     * 2. Submit tasks to WorkflowExecutorService or ThreadPoolManager.
     * 3. Collect CompletableFutures and wait for completion.
     * 4. Build the FileProcessingSummaryModel once all tasks complete.
     * </p>
     * This allows parallel execution without changing the gRPC unary interface.
     *
     * @param requestModel The internal request containing files and operations
     * @return Summary of processing results
     */
    public FileProcessingSummaryModel processFiles(FileProcessingRequestModel requestModel) {
        List<FileOperationResultModel> results = new ArrayList<>();
        int successful = 0;
        int failed = 0;

        for (FileModel file : requestModel.files()) {
            // Get operations: either file-specific or default
            List<OperationType> operations = getOperationsForFile(requestModel, file.fileId());

            for (OperationType operation : operations) {
                FileOperationResultModel result = executeOperation(file, operation);
                results.add(result);

                if (result.status() == OperationStatus.SUCCESS) {
                    successful++;
                } else if (result.status() == OperationStatus.FAILED) {
                    failed++;
                }
            }
        }

        return FileProcessingSummaryModel.builder()
                .totalFiles(requestModel.files().size())
                .successfulFiles(successful)
                .failedFiles(failed)
                .results(results)
                .build();
    }

    /**
     * Get the list of operations to apply to a given file.
     */
    private List<OperationType> getOperationsForFile(FileProcessingRequestModel request, String fileId) {
        Map<String, List<OperationType>> fileSpecific = request.fileSpecificOperations();
        if (fileSpecific.containsKey(fileId)) {
            return fileSpecific.get(fileId);
        }
        return request.defaultOperations();
    }

    /**
     * Execute an operation on a file.
     * For now this is mocked; real implementations should delegate to dedicated operation services.
     */
    private FileOperationResultModel executeOperation(FileModel file, OperationType operation) {
        Instant start = Instant.now();

        try {
            log.info("Executing {} on file {}", operation, file.fileName());

            switch (operation) {
                case VALIDATE -> FileOperations.validateFile(file);
                case METADATA_EXTRACTION -> FileOperations.extractMetadata(file);
                case OCR_TEXT_EXTRACTION -> FileOperations.performOcr(file);
                case IMAGE_RESIZE -> FileOperations.resizeImage(file);
                case FILE_COMPRESSION -> FileOperations.compressFile(file);
                case FORMAT_CONVERSION -> FileOperations.convertFormat(file);
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
}
