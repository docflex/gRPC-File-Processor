package com.fileprocessing.service.grpc;

import com.fileprocessing.FileSpec.FileProcessingSummary;
import com.fileprocessing.FileSpec.FileUploadRequest;
import com.fileprocessing.FileSpec.OperationType;
import com.fileprocessing.model.FileModel;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.model.FileProcessingSummaryModel;
import com.fileprocessing.model.FileUploadRequestModel;
import com.fileprocessing.util.ProtoConverter;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadFilesService {

    private final ProcessFileService processFileService;

    public StreamObserver<FileUploadRequest> uploadFiles(
            StreamObserver<FileProcessingSummary> responseObserver,
            Runnable onSuccess,
            Runnable onFailure,
            Runnable onCompletion) {

        List<FileUploadRequestModel> uploads = new ArrayList<>();
        boolean[] completedOrErrored = {false};

        return new StreamObserver<>() {

            @Override
            public void onNext(FileUploadRequest fileUploadRequest) {
                if (completedOrErrored[0]) return;
                try {
                    FileModel fileModel = ProtoConverter.toInternalFileModel(fileUploadRequest.getFile());
                    List<OperationType> operations = fileUploadRequest.getOperationsList();
                    uploads.add(new FileUploadRequestModel(fileModel, operations));
                } catch (Exception e) {
                    log.error("Error converting uploaded file", e);
                    onFailure.run();
                    completedOrErrored[0] = true;
                    responseObserver.onError(
                            Status.INVALID_ARGUMENT
                                    .withDescription("Invalid file data: " + e.getMessage())
                                    .withCause(e)
                                    .asRuntimeException()
                    );
                    onCompletion.run();
                }
            }

            @Override
            public void onError(Throwable t) {
                if (completedOrErrored[0]) return;
                completedOrErrored[0] = true;
                log.error("Client cancelled or errored during upload", t);
                onFailure.run();
                onCompletion.run();
            }

            @Override
            public void onCompleted() {
                if (completedOrErrored[0]) return;
                completedOrErrored[0] = true;

                try {
                    List<FileModel> files = uploads.stream()
                            .map(FileUploadRequestModel::file)
                            .toList();

                    Map<String, List<OperationType>> fileOpsMap = new HashMap<>();
                    for (FileUploadRequestModel req : uploads) {
                        fileOpsMap.put(req.file().fileId(), req.operations());
                    }

                    FileProcessingRequestModel requestModel =
                            new FileProcessingRequestModel(files, List.of(), fileOpsMap);

                    FileProcessingSummaryModel summaryModel = processFileService.processFiles(requestModel);
                    FileProcessingSummary response = ProtoConverter.toProto(summaryModel);

                    onSuccess.run();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    log.error("Error processing uploaded files", e);
                    onFailure.run();
                    responseObserver.onError(
                            Status.INTERNAL
                                    .withDescription("Upload processing failed: " + e.getMessage())
                                    .withCause(e)
                                    .asRuntimeException()
                    );
                } finally {
                    onCompletion.run();
                }
            }
        };
    }
}
