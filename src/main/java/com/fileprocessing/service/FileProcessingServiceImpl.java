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
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.concurrent.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class FileProcessingServiceImpl extends FileProcessingServiceImplBase {

    private final FileProcessingMetrics processingMetrics;
    private final ProcessFileService processFileService;
    private final StreamFileOperationsService streamFileOperationsService;
    private final UploadFilesService uploadFilesService;
    private final LiveFileProcessingService liveFileProcessingService;

    // Dedicated executor for gRPC streaming to prevent breaks
    private final ExecutorService streamingExecutor = new ThreadPoolExecutor(
            4, 4, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100), // max 100 pending tasks
            r -> new Thread(r, "grpc-stream-thread"),
            new ThreadPoolExecutor.CallerRunsPolicy() // backpressure: run in caller thread if queue full
    );

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
            StreamObserver<FileOperationResult> safeObserver = wrapWithExecutor(responseObserver, streamingExecutor);
            streamFileOperationsService.streamFileOperations(model, safeObserver, startTime);
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
            // Wrap the responseObserver to use streamingExecutor
            StreamObserver<FileOperationResult> safeObserver = wrapWithExecutor(responseObserver, streamingExecutor);

            StreamObserver<FileUploadRequest> observer = liveFileProcessingService.liveFileProcessing(safeObserver);
            if (observer != null) {
                processingMetrics.recordTaskCompletion(System.currentTimeMillis() - startTime);
                return observer;
            }
            return getNoOpObserver();
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
            return getNoOpObserver();
        } finally {
            processingMetrics.decrementActiveRequests();
            processingMetrics.decrementActiveTasks();
            processingMetrics.recordRequestCompletion(System.currentTimeMillis() - startTime);
            log.info("Current Metrics: {}", processingMetrics);
        }
    }

    // Helpers

    /**
     * Wraps a StreamObserver to always execute onNext/onCompleted/onError on the given executor.
     */
    private <T> StreamObserver<T> wrapWithExecutor(StreamObserver<T> observer, ExecutorService executor) {
        if (observer instanceof ServerCallStreamObserver) {
            ((ServerCallStreamObserver<?>) observer).setOnCancelHandler(() -> {
                log.info("Client cancelled the stream");
            });
        }

        return new StreamObserver<>() {
            @Override
            public void onNext(T value) {
                executor.submit(() -> observer.onNext(value));
            }

            @Override
            public void onError(Throwable t) {
                executor.submit(() -> observer.onError(t));
            }

            @Override
            public void onCompleted() {
                executor.submit(observer::onCompleted);
            }
        };
    }

    private <T> StreamObserver<T> getNoOpObserver() {
        return new StreamObserver<>() {
            @Override public void onNext(T value) {}
            @Override public void onError(Throwable t) {}
            @Override public void onCompleted() {}
        };
    }

}