package com.fileprocessing.service.grpc;

import com.fileprocessing.concurrency.WorkflowExecutorService;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.model.FileProcessingSummaryModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for handling unary file processing requests (gRPC ProcessFile).
 *
 * <p>This delegates actual file operation execution to {@link WorkflowExecutorService},
 * which handles concurrency, task isolation, and metrics tracking.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessFileService {

    private final WorkflowExecutorService workflowExecutorService;

    /**
     * Processes the files in the given request model.
     *
     * @param requestModel Internal request containing files and operations
     * @return Summary of processing results
     */
    public FileProcessingSummaryModel processFiles(FileProcessingRequestModel requestModel) {
        return workflowExecutorService.processWorkflow(requestModel);
    }
}
