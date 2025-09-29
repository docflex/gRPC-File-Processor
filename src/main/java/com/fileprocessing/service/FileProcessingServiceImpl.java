package com.fileprocessing.service;

import com.fileprocessing.FileProcessingServiceGrpc.FileProcessingServiceImplBase;
import com.fileprocessing.FileSpec.File;
import com.fileprocessing.FileSpec.FileOperationResult;
import com.fileprocessing.FileSpec.FileProcessingRequest;
import com.fileprocessing.FileSpec.FileProcessingSummary;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.model.FileProcessingSummaryModel;
import com.fileprocessing.service.grpc.ProcessFileService;
import com.fileprocessing.service.grpc.StreamFileOperationsService;
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
//    private final UploadFilesService uploadFilesService;
//    private final LiveFileProcessingService liveFileProcessingService;

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
