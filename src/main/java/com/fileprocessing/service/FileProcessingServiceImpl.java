package com.fileprocessing.service;

import com.fileprocessing.FileProcessingServiceGrpc.FileProcessingServiceImplBase;
import com.fileprocessing.FileSpec.File;
import com.fileprocessing.FileSpec.FileOperationResult;
import com.fileprocessing.FileSpec.FileProcessingRequest;
import com.fileprocessing.FileSpec.FileProcessingSummary;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.model.FileProcessingSummaryModel;
import com.fileprocessing.service.monitoring.FileProcessingMetrics;
import com.fileprocessing.service.grpc.ProcessFileService;
import com.fileprocessing.util.ProtoConverter;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class FileProcessingServiceImpl extends FileProcessingServiceImplBase {

    private final FileProcessingMetrics processingMetrics;
    private final ProcessFileService processFileService;
//    private final StreamFileOperationsService streamFileOperationsService;
//    private final UploadFilesService uploadFilesService;
//    private final LiveFileProcessingService liveFileProcessingService;

    /**
     * Unary RPC to process a batch of files.
     * Step 1: Convert proto request to internal model
     * Step 2: Delegate to ProcessFileService
     * Step 3: Convert internal summary model to proto response
     * Step 4: Send response via responseObserver
     *
     * @param fileProcessingRequest: The incoming gRPC request containing files and operations.
     * @param responseObserver:      The gRPC stream observer to send the response.
     */
    @Override
    public void processFile(FileProcessingRequest fileProcessingRequest, StreamObserver<FileProcessingSummary> responseObserver) {
        long startTime = System.currentTimeMillis();
        processingMetrics.incrementActiveRequests();

        try {
            FileProcessingRequestModel fileProcessingRequestModel = ProtoConverter.toInternalModel(fileProcessingRequest);
            FileProcessingSummaryModel fileProcessingSummaryModel = processFileService.processFiles(fileProcessingRequestModel);
            FileProcessingSummary response = ProtoConverter.toProto(fileProcessingSummaryModel);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing file workflow", e);
            processingMetrics.incrementFailedRequests();
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("File processing failed: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        } finally {
            processingMetrics.decrementActiveRequests();
            processingMetrics.addRequestDuration(System.currentTimeMillis() - startTime);
            log.info("Current Metrics: {}", processingMetrics);
        }
    }

    @Override
    public void streamFileOperations(FileProcessingRequest fileProcessingRequest, StreamObserver<FileOperationResult> responseObserver) {
        long startTime = System.currentTimeMillis();
        processingMetrics.incrementActiveRequests();
        try {
            responseObserver.onError(
                Status.UNIMPLEMENTED
                    .withDescription("Stream file operations not implemented yet")
                    .asRuntimeException()
            );
            processingMetrics.incrementFailedRequests();
        } finally {
            processingMetrics.decrementActiveRequests();
            processingMetrics.addRequestDuration(System.currentTimeMillis() - startTime);
            log.info("Current Metrics: {}", processingMetrics);
        }
    }

    @Override
    public StreamObserver<File> uploadFiles(StreamObserver<FileProcessingSummary> responseObserver) {
        long startTime = System.currentTimeMillis();
        processingMetrics.incrementActiveRequests();
        try {
            responseObserver.onError(
                Status.UNIMPLEMENTED
                    .withDescription("Upload files not implemented yet")
                    .asRuntimeException()
            );
            processingMetrics.incrementFailedRequests();
        } finally {
            processingMetrics.decrementActiveRequests();
            processingMetrics.addRequestDuration(System.currentTimeMillis() - startTime);
            log.info("Current Metrics: {}", processingMetrics);
        }
        return null;
    }

    @Override
    public StreamObserver<File> liveFileProcessing(StreamObserver<FileOperationResult> responseObserver) {
        long startTime = System.currentTimeMillis();
        processingMetrics.incrementActiveRequests();
        try {
            responseObserver.onError(
                Status.UNIMPLEMENTED
                    .withDescription("Live file processing not implemented yet")
                    .asRuntimeException()
            );
            processingMetrics.incrementFailedRequests();
        } finally {
            processingMetrics.decrementActiveRequests();
            processingMetrics.addRequestDuration(System.currentTimeMillis() - startTime);
            log.info("Current Metrics: {}", processingMetrics);
        }
        return null;
    }
}
