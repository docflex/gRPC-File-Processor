package com.fileprocessing.service;

import com.fileprocessing.FileProcessingServiceGrpc.FileProcessingServiceImplBase;
import com.fileprocessing.FileSpec.File;
import com.fileprocessing.FileSpec.FileOperationResult;
import com.fileprocessing.FileSpec.FileProcessingRequest;
import com.fileprocessing.FileSpec.FileProcessingSummary;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.model.FileProcessingSummaryModel;
import com.fileprocessing.service.grpc.LiveFileProcessingService;
import com.fileprocessing.service.grpc.ProcessFileService;
import com.fileprocessing.service.grpc.StreamFileOperationsService;
import com.fileprocessing.service.grpc.UploadFilesService;
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
     * @param fileProcessingRequest: The incoming gRPC request containing files and operations.
     * @param responseObserver: The gRPC stream observer to send the response.
     */
    @Override
    public void processFile(FileProcessingRequest fileProcessingRequest, StreamObserver<FileProcessingSummary> responseObserver) {
        try {
            FileProcessingRequestModel fileProcessingRequestModel = ProtoConverter.toInternalModel(fileProcessingRequest);
            FileProcessingSummaryModel fileProcessingSummaryModel = processFileService.processFiles(fileProcessingRequestModel);
            FileProcessingSummary response = ProtoConverter.toProto(fileProcessingSummaryModel);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing file workflow", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("File processing failed: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void streamFileOperations(FileProcessingRequest fileProcessingRequest, StreamObserver<FileOperationResult> responseObserver) {
    }

    @Override
    public StreamObserver<File> uploadFiles(StreamObserver<FileProcessingSummary> responseObserver) {
        return null;
    }

    @Override
    public StreamObserver<File> liveFileProcessing(StreamObserver<FileOperationResult> responseObserver) {
        return null;
    }
}
