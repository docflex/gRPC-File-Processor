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

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveFileProcessingService {

    private final WorkflowExecutorService workflowExecutorService;
    private final FileProcessingMetrics processingMetrics;

    /**
     * Returns a StreamObserver for bi-directional streaming of file uploads.
     * <p>
     * Each incoming file is processed immediately, and the operation results are streamed
     * back to the client in real-time.
     * </p>
     */
    public StreamObserver<FileUploadRequest> liveFileProcessing(StreamObserver<FileOperationResult> responseObserver) {
        long startTime = System.currentTimeMillis();
        processingMetrics.incrementActiveRequests();

        return new StreamObserver<>() {
            boolean completedOrErrored = false;

            @Override
            public void onNext(FileUploadRequest request) {
                if (completedOrErrored) return;

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

                    // Process and stream results in real-time
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
                    completedOrErrored = true;
                }
            }

            @Override
            public void onError(Throwable t) {
                if (completedOrErrored) return;
                completedOrErrored = true;
                log.error("Client stream errored", t);
                processingMetrics.incrementFailedRequests();
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                if (completedOrErrored) return;
                completedOrErrored = true;
                try {
                    responseObserver.onCompleted();
                    processingMetrics.incrementActiveRequests(); // optional if needed
                } finally {
                    processingMetrics.decrementActiveRequests();
                    processingMetrics.addRequestDuration(System.currentTimeMillis() - startTime);
                    log.info("Current Metrics: {}", processingMetrics);
                }
            }
        };
    }
}
