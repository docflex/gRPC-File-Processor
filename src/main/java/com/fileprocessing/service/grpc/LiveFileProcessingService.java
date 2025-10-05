package com.fileprocessing.service.grpc;

import com.fileprocessing.FileSpec.FileUploadRequest;
import com.fileprocessing.FileSpec.FileOperationResult;
import com.fileprocessing.concurrency.WorkflowExecutorService;
import com.fileprocessing.model.FileModel;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.service.monitoring.FileProcessingMetrics;
import com.fileprocessing.util.ProtoConverter;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveFileProcessingService {

    private final WorkflowExecutorService workflowExecutorService;
    private final FileProcessingMetrics processingMetrics;

    public StreamObserver<FileUploadRequest> liveFileProcessing(StreamObserver<FileOperationResult> responseObserver) {
        long startTime = System.currentTimeMillis();
        processingMetrics.incrementActiveRequests();
        AtomicBoolean completedOrErrored = new AtomicBoolean(false); // final reference

        return new StreamObserver<>() {

            @Override
            public void onNext(FileUploadRequest request) {
                if (completedOrErrored.get()) return;

                try {
                    FileModel file = ProtoConverter.toInternalFileModel(request.getFile());
                    List<com.fileprocessing.FileSpec.OperationType> operations =
                            request.getOperationsList().isEmpty()
                                    ? List.of(com.fileprocessing.FileSpec.OperationType.VALIDATE)
                                    : request.getOperationsList();

                    FileProcessingRequestModel requestModel = new FileProcessingRequestModel(
                            List.of(file),
                            operations,
                            Collections.emptyMap()
                    );

                    workflowExecutorService.processWorkflowStreamed(
                            requestModel,
                            resultModel -> {
                                try {
                                    FileOperationResult protoResult = ProtoConverter.toProto(resultModel);
                                    responseObserver.onNext(protoResult);
                                } catch (Exception e) {
                                    log.error("Error sending result to client for file {}", file.fileId(), e);
                                }
                            }
                    );

                } catch (Exception e) {
                    log.error("Error processing incoming file {}", request.getFile().getFileId(), e);
                    processingMetrics.incrementFailedRequests();
                    responseObserver.onError(e);
                    completedOrErrored.set(true);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (completedOrErrored.get()) return;
                completedOrErrored.set(true);
                log.error("Client stream errored", t);
                processingMetrics.incrementFailedRequests();
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                if (completedOrErrored.get()) return;
                completedOrErrored.set(true);

                try {
                    responseObserver.onCompleted();
                } finally {
                    processingMetrics.decrementActiveRequests();
                    processingMetrics.recordRequestCompletion(System.currentTimeMillis() - startTime);
                    log.info("Current Metrics: {}", processingMetrics);
                }
            }
        };
    }
}
