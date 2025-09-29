package com.fileprocessing.service.grpc;

import com.fileprocessing.FileSpec.FileOperationResult;
import com.fileprocessing.concurrency.WorkflowExecutorService;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.service.monitoring.FileProcessingMetrics;
import com.fileprocessing.util.ProtoConverter;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for handling server-streaming gRPC file operations.
 * <p>
 * Instead of returning a single batch summary, this service streams each
 * {@link FileOperationResult} to the client as soon as it completes.
 * Each result is delivered in real-time, allowing clients to process
 * large workflows incrementally.
 * </p>
 *
 * <p><b>Metrics:</b> Tracks active requests, failed requests, and request duration
 * for observability and monitoring purposes.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamFileOperationsService {

    private final WorkflowExecutorService workflowExecutorService;
    private final FileProcessingMetrics processingMetrics;

    /**
     * Streams file operation results to the client in real-time.
     * <p>
     * Each file in the request is processed concurrently. As soon as an operation
     * completes, its result is converted to a proto object and pushed to the client
     * using the provided {@link StreamObserver}.
     * </p>
     *
     * <p><b>Completion:</b></p>
     * <ul>
     *     <li>If all operations succeed, the observer is completed normally.</li>
     *     <li>If any exception occurs during processing, the observer receives an
     *     INTERNAL gRPC error with details.</li>
     * </ul>
     *
     * <p><b>Metrics:</b> Updates active request count and duration, and increments
     * failed request count in case of errors.</p>
     *
     * @param fileProcessingRequestModel the internal request model containing files and operations
     * @param responseObserver           the gRPC stream observer to deliver each {@link FileOperationResult}
     * @param startTime                  the request start timestamp used for metrics calculation
     */
    public void streamFileOperations(FileProcessingRequestModel fileProcessingRequestModel,
                                     StreamObserver<FileOperationResult> responseObserver,
                                     long startTime) {

        workflowExecutorService.processWorkflowStreamed(
                fileProcessingRequestModel,
                fileProcessingResponseModel -> {
                    FileOperationResult protoResult = ProtoConverter.toProto(fileProcessingResponseModel);
                    responseObserver.onNext(protoResult);
                }
        ).whenComplete((ignored, throwable) -> {
            try {
                if (throwable != null) {
                    log.error("Streaming workflow failed", throwable);
                    processingMetrics.incrementFailedRequests();
                    responseObserver.onError(
                            Status.INTERNAL
                                    .withDescription("Streaming failed: " + throwable.getMessage())
                                    .withCause(throwable)
                                    .asRuntimeException()
                    );
                } else {
                    responseObserver.onCompleted();
                }
            } finally {
                processingMetrics.decrementActiveRequests();
                processingMetrics.addRequestDuration(System.currentTimeMillis() - startTime);
                log.info("Current Metrics: {}", processingMetrics);
            }
        });
    }
}
