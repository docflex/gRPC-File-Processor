package com.fileprocessing.util;

import com.fileprocessing.FileSpec.FileProcessingSummary;
import com.fileprocessing.FileSpec.FileOperationResult;
import com.fileprocessing.FileSpec.FileProcessingRequest;
import com.fileprocessing.FileSpec.OperationType;
import com.fileprocessing.FileSpec.File;
import com.fileprocessing.model.FileModel;
import com.fileprocessing.model.FileOperationResultModel;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.model.FileProcessingSummaryModel;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for converting between gRPC proto objects
 * and internal domain models.
 */
public final class ProtoConverter {

    private ProtoConverter() {
        // prevent instantiation
    }

    // =======================
    // Request Conversions
    // =======================

    public static FileProcessingRequestModel toInternalModel(FileProcessingRequest request) {
        List<FileModel> files = request.getFilesList().stream()
                .map(ProtoConverter::toInternalFileModel)
                .collect(Collectors.toList());

        List<OperationType> defaultOps = request.getOperationsList();

        return FileProcessingRequestModel.builder()
                .files(files)
                .defaultOperations(defaultOps)
                .build();
    }

    private static FileModel toInternalFileModel(File fileProto) {
        return FileModel.builder()
                .fileId(fileProto.getFileId())
                .fileName(fileProto.getFileName())
                .content(fileProto.getContent().toByteArray())
                .fileType(fileProto.getFileType())
                .sizeBytes(fileProto.getSizeBytes())
                .build();
    }

    // =======================
    // Response Conversions
    // =======================

    public static FileProcessingSummary toProto(FileProcessingSummaryModel model) {
        return FileProcessingSummary.newBuilder()
                .setTotalFiles(model.totalFiles())
                .setSuccessfulFiles(model.successfulFiles())
                .setFailedFiles(model.failedFiles())
                .addAllResults(
                        model.results().stream()
                                .map(ProtoConverter::toProtoResult)
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static FileOperationResult toProtoResult(FileOperationResultModel model) {
        return FileOperationResult.newBuilder()
                .setFileId(model.fileId())
                .setOperation(model.operationType())
                .setStatus(model.status())
                .setDetails(model.details())
                .setStartTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(model.startTime().getEpochSecond())
                        .setNanos(model.startTime().getNano())
                        .build())
                .setEndTime(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(model.endTime().getEpochSecond())
                        .setNanos(model.endTime().getNano())
                        .build())
                .setResultLocation(model.resultLocation())
                .build();
    }

    // =======================
    // Helpers
    // =======================

    private static Instant fromProtoTimestamp(com.google.protobuf.Timestamp ts) {
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }
}
