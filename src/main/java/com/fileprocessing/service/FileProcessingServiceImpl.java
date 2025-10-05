package com.fileprocessing.service;

import com.fileprocessing.FileProcessingServiceGrpc.FileProcessingServiceImplBase;
import com.fileprocessing.FileSpec.*;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.model.FileProcessingSummaryModel;
import com.fileprocessing.service.grpc.LiveFileProcessingService;
import com.fileprocessing.service.grpc.ProcessFileService;
import com.fileprocessing.service.grpc.StreamFileOperationsService;
import com.fileprocessing.service.grpc.UploadFilesService;
import com.fileprocessing.service.monitoring.FileProcessingMetrics;
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
    private final StreamFileOperationsService streamFileOperationsService;
    private final UploadFilesService uploadFilesService;
    private final LiveFileProcessingService liveFileProcessingService;

    // TODO: Rule of thumb
    //  Outer service = translate request, delegate, update metrics.
    //  Inner service = owns the lifecycle of the StreamObserver.

    @Override
    public void processFile(FileProcessingRequest fileProcessingRequest,
                            StreamObserver<FileProcessingSummary> responseObserver) {
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
            processingMetrics.recordRequestCompletion(System.currentTimeMillis() - startTime);
            log.info("Current Metrics: {}", processingMetrics);
        }
    }

    @Override
    public void streamFileOperations(FileProcessingRequest request,
                                     StreamObserver<FileOperationResult> responseObserver) {
        long startTime = System.currentTimeMillis();
        processingMetrics.incrementActiveRequests();

        try {
            FileProcessingRequestModel model = ProtoConverter.toInternalModel(request);
            streamFileOperationsService.streamFileOperations(model, responseObserver, startTime);
        } catch (Exception e) {
            log.error("Error processing streamFileOperations", e);
            processingMetrics.incrementFailedRequests();
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Streaming file processing failed: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        } finally {
            processingMetrics.decrementActiveRequests();
            processingMetrics.recordRequestCompletion(System.currentTimeMillis() - startTime);
            log.info("Current Metrics: {}", processingMetrics);
        }
    }


    @Override
    public StreamObserver<FileUploadRequest> uploadFiles(StreamObserver<FileProcessingSummary> responseObserver) {
        long startTime = System.currentTimeMillis();
        processingMetrics.incrementActiveRequests();

        try {
            return uploadFilesService.uploadFiles(
                    responseObserver,
                    () -> {
                    }, // onSuccess, optional extra processing
                    processingMetrics::incrementFailedRequests,  // onFailure
                    () -> { // onCompletion
                        processingMetrics.decrementActiveRequests();
                        processingMetrics.recordRequestCompletion(System.currentTimeMillis() - startTime);
                        log.info("Current Metrics: {}", processingMetrics);
                    }
            );
        } catch (Exception e) {
            log.error("Error handling uploadFiles", e);
            processingMetrics.incrementFailedRequests();
            processingMetrics.decrementActiveRequests();
            processingMetrics.recordRequestCompletion(System.currentTimeMillis() - startTime);
            // Notify the client about the failure
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Upload files failed: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );

            return getNoOpObserver();
        }
    }

    @Override
    public StreamObserver<FileUploadRequest> liveFileProcessing(StreamObserver<FileOperationResult> responseObserver) {
        long startTime = System.currentTimeMillis();
        processingMetrics.incrementActiveRequests();
        processingMetrics.incrementActiveTasks();

        try {
            StreamObserver<FileUploadRequest> observer = liveFileProcessingService.liveFileProcessing(responseObserver);
            if (observer != null) {
                processingMetrics.recordTaskCompletion(System.currentTimeMillis() - startTime);
                return observer;
            }
            return null;
        } catch (Exception e) {
            log.error("Error initializing live file processing", e);
            processingMetrics.incrementFailedRequests();
            processingMetrics.incrementFailedTasks();
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Live file processing failed: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
            return null;
        } finally {
            processingMetrics.decrementActiveRequests();
            processingMetrics.decrementActiveTasks();
            processingMetrics.recordRequestCompletion(System.currentTimeMillis() - startTime);
            log.info("Current Metrics: {}", processingMetrics);
        }
    }

    // Helpers

    private <T> StreamObserver<T> getNoOpObserver() {
        return new StreamObserver<>() {
            @Override public void onNext(T value) {}
            @Override public void onError(Throwable t) {}
            @Override public void onCompleted() {}
        };
    }

}
