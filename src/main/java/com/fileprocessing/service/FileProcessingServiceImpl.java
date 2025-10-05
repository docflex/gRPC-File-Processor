package com.fileprocessing.service;

import com.fileprocessing.FileProcessingServiceGrpc.FileProcessingServiceImplBase;
import com.fileprocessing.FileSpec.*;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.model.FileProcessingSummaryModel;
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
//    private final LiveFileProcessingService liveFileProcessingService;

    // TODO: Rule of thumb
    //  Outer service = translate request, delegate, update metrics.
    //  Inner service = owns the lifecycle of the StreamObserver.

    /**
     * Unary gRPC RPC to process a batch of files.
     * <p>
     * Steps:
     * <ol>
     *     <li>Convert the incoming proto request to an internal domain model.</li>
     *     <li>Delegate processing to {@link ProcessFileService}.</li>
     *     <li>Convert the internal summary model back to a proto response.</li>
     *     <li>Send the response via the provided {@link StreamObserver}.</li>
     * </ol>
     * </p>
     *
     * <p><b>Metrics Tracking:</b> Increments active requests at the start, decrements after
     * completion or failure, and records request duration.</p>
     *
     * <p><b>Error Handling:</b> Any exception during processing results in an INTERNAL gRPC
     * status sent to the client with a descriptive message.</p>
     *
     * @param fileProcessingRequest the incoming gRPC request containing files and requested operations
     * @param responseObserver      the gRPC stream observer used to send the summarized response
     */
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
            processingMetrics.addRequestDuration(System.currentTimeMillis() - startTime);
            log.info("Current Metrics: {}", processingMetrics);
        }
    }

    /**
     * Handles a server-streaming gRPC request to process multiple files.
     * <p>
     * Each file operation is executed concurrently, and the results are streamed
     * to the client as soon as they complete. This method delegates the actual
     * streaming logic to {@link StreamFileOperationsService}.
     * </p>
     *
     * <p><b>Metrics Tracking:</b></p>
     * <ul>
     *     <li>Increments active request count at the start.</li>
     *     <li>Decrements active request count and records request duration if an exception occurs before delegation.</li>
     *     <li>Increments failed request count on exception.</li>
     * </ul>
     *
     * <p><b>Error Handling:</b></p>
     * <ul>
     *     <li>If any exception occurs during request conversion or delegation, the client
     *     receives an INTERNAL gRPC status with the exception details.</li>
     * </ul>
     *
     * @param fileProcessingRequest the incoming gRPC request containing files and operations
     * @param responseObserver      the gRPC stream observer used to send each
     *                              {@link com.fileprocessing.FileSpec.FileOperationResult} back to the client
     */
    @Override
    public void streamFileOperations(FileProcessingRequest fileProcessingRequest,
                                     StreamObserver<FileOperationResult> responseObserver) {
        long startTime = System.currentTimeMillis();
        processingMetrics.incrementActiveRequests();

        try {
            FileProcessingRequestModel fileProcessingRequestModel = ProtoConverter.toInternalModel(fileProcessingRequest);
            streamFileOperationsService.streamFileOperations(fileProcessingRequestModel, responseObserver, startTime);
        } catch (Exception e) {
            log.error("Error processing streamFileOperations", e);
            processingMetrics.incrementFailedRequests();
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Streaming file processing failed: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
            processingMetrics.decrementActiveRequests();
            processingMetrics.addRequestDuration(System.currentTimeMillis() - startTime);
            log.info("Current Metrics: {}", processingMetrics);
        }
    }


    /**
     * Handles client-streaming gRPC requests to upload multiple files.
     * <p>
     * This method delegates the actual upload processing to {@link UploadFilesService}.
     * It manages request-level metrics such as active requests, request duration,
     * and failed request counts.
     * </p>
     *
     * <p><b>Workflow:</b></p>
     * <ol>
     *     <li>Increments the active request count at the start.</li>
     *     <li>Delegates the streaming lifecycle to {@link UploadFilesService#uploadFiles}.</li>
     *     <li>On completion of the stream, decrements the active request count and records
     *     request duration.</li>
     *     <li>If any exception occurs during setup, increments the failed requests counter
     *     and sends an INTERNAL gRPC error to the client.</li>
     * </ol>
     *
     * <p><b>Metrics Tracking:</b></p>
     * <ul>
     *     <li>{@link FileProcessingMetrics#incrementActiveRequests()} is called at the start.</li>
     *     <li>{@link FileProcessingMetrics#decrementActiveRequests()} and
     *     {@link FileProcessingMetrics#addRequestDuration(long)} are called on stream completion.</li>
     *     <li>{@link FileProcessingMetrics#incrementFailedRequests()} is called if setup fails or
     *     an exception occurs during processing.</li>
     * </ul>
     *
     * <p><b>Error Handling:</b></p>
     * <ul>
     *     <li>If an exception is thrown during the setup of the streaming observer,
     *     the client receives a gRPC {@link io.grpc.Status#INTERNAL} error with a descriptive message.</li>
     *     <li>If an exception occurs while processing individual files, it is handled
     *     inside {@link UploadFilesService} and the failed tasks are tracked via metrics.</li>
     * </ul>
     *
     * @param responseObserver the gRPC {@link StreamObserver} used to send the final
     *                         {@link FileProcessingSummary} back to the client once all files are processed
     * @return a {@link StreamObserver} that the gRPC client uses to stream {@link FileUploadRequest} messages
     * (one per file) to the server
     */
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
                        processingMetrics.addRequestDuration(System.currentTimeMillis() - startTime);
                        log.info("Current Metrics: {}", processingMetrics);
                    }
            );
        } catch (Exception e) {
            log.error("Error handling uploadFiles", e);
            processingMetrics.incrementFailedRequests();
            processingMetrics.decrementActiveRequests();
            processingMetrics.addRequestDuration(System.currentTimeMillis() - startTime);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Upload files failed: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
            return new StreamObserver<>() {
                @Override
                public void onNext(FileUploadRequest file) {
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onCompleted() {
                }
            };
        }
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
