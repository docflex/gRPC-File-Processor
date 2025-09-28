package com.fileprocessing.model;

import com.fileprocessing.FileSpec.OperationStatus;
import com.fileprocessing.FileSpec.OperationType;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the result of a single file operation.
 * Immutable and safe for concurrent use.
 *
 * @param resultLocation path/URL to stored file
 */
public record FileOperationResultModel(String fileId, OperationType operationType, OperationStatus status,
                                       String details, Instant startTime, Instant endTime, String resultLocation) {

    /**
     * Constructor with validation.
     */
    public FileOperationResultModel(String fileId,
                                    OperationType operationType,
                                    OperationStatus status,
                                    String details,
                                    Instant startTime,
                                    Instant endTime,
                                    String resultLocation) {

        this.fileId = Objects.requireNonNull(fileId, "fileId cannot be null");
        this.operationType = Objects.requireNonNull(operationType, "operationType cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.details = (details != null) ? details : "";
        this.startTime = (startTime != null) ? startTime : Instant.now();
        this.endTime = (endTime != null) ? endTime : Instant.now();
        this.resultLocation = (resultLocation != null) ? resultLocation : "";
    }

    /**
     * Builder pattern for easier construction.
     */
    public static FileOperationResultModelBuilder builder() {
        return new FileOperationResultModelBuilder();
    }

    /**
     * Returns the duration of the operation in milliseconds.
     */
    public long getDurationMillis() {
        long duration = endTime.toEpochMilli() - startTime.toEpochMilli();
        return Math.max(duration, 0);
    }


    public static class FileOperationResultModelBuilder {
        private String fileId;
        private OperationType operationType;
        private OperationStatus status;
        private String details;
        private Instant startTime;
        private Instant endTime;
        private String resultLocation;

        public FileOperationResultModelBuilder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }

        public FileOperationResultModelBuilder operationType(OperationType operationType) {
            this.operationType = operationType;
            return this;
        }

        public FileOperationResultModelBuilder status(OperationStatus status) {
            this.status = status;
            return this;
        }

        public FileOperationResultModelBuilder details(String details) {
            this.details = details;
            return this;
        }

        public FileOperationResultModelBuilder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public FileOperationResultModelBuilder endTime(Instant endTime) {
            this.endTime = endTime;
            return this;
        }

        public FileOperationResultModelBuilder resultLocation(String resultLocation) {
            this.resultLocation = resultLocation;
            return this;
        }

        public FileOperationResultModel build() {
            return new FileOperationResultModel(fileId, operationType, status, details, startTime, endTime, resultLocation);
        }
    }
}
