package com.fileprocessing.model;

import com.fileprocessing.FileSpec.OperationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a client upload request containing a file and the operations to be performed.
 * Immutable and thread-safe.
 */
public record FileUploadRequestModel(FileModel file, List<OperationType> operations) {

    /**
     * Constructor with validation and defensive copy.
     */
    public FileUploadRequestModel {
        Objects.requireNonNull(file, "file cannot be null");
        operations = (operations != null) ? List.copyOf(operations) : Collections.emptyList();
    }

    /**
     * Builder for easier and safer construction.
     */
    public static FileUploadRequestModelBuilder builder() {
        return new FileUploadRequestModelBuilder();
    }

    public static class FileUploadRequestModelBuilder {
        private FileModel file;
        private final List<OperationType> operations = new ArrayList<>();

        public FileUploadRequestModelBuilder file(FileModel file) {
            this.file = file;
            return this;
        }

        public FileUploadRequestModelBuilder operations(List<OperationType> ops) {
            if (ops != null) {
                this.operations.addAll(ops);
            }
            return this;
        }

        public FileUploadRequestModelBuilder addOperation(OperationType op) {
            if (op != null) {
                this.operations.add(op);
            }
            return this;
        }

        public FileUploadRequestModel build() {
            return new FileUploadRequestModel(file, operations);
        }
    }
}
