package com.fileprocessing.model;

import com.fileprocessing.FileSpec.OperationType;

import java.util.*;

/**
 * Represents an internal request to process multiple files.
 * Immutable, validated, and thread-safe.
 */
public record FileProcessingRequestModel(
        List<FileModel> files,
        List<OperationType> defaultOperations,
        Map<String, List<OperationType>> fileSpecificOperations
) {

    /**
     * Compact constructor with validation and defensive copies.
     */
    public FileProcessingRequestModel {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files cannot be null or empty");
        }
        files = List.copyOf(files);

        defaultOperations = (defaultOperations != null)
                ? List.copyOf(defaultOperations)
                : Collections.emptyList();

        if (fileSpecificOperations != null) {
            Map<String, List<OperationType>> copy = new HashMap<>();
            for (Map.Entry<String, List<OperationType>> entry : fileSpecificOperations.entrySet()) {
                copy.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            fileSpecificOperations = Collections.unmodifiableMap(copy);
        } else {
            fileSpecificOperations = Collections.emptyMap();
        }
    }

    /**
     * Builder for easier construction.
     */
    public static FileProcessingRequestModelBuilder builder() {
        return new FileProcessingRequestModelBuilder();
    }

    public static class FileProcessingRequestModelBuilder {
        private final List<FileModel> files = new ArrayList<>();
        private final List<OperationType> defaultOperations = new ArrayList<>();
        private final Map<String, List<OperationType>> fileSpecificOperations = new HashMap<>();

        public FileProcessingRequestModelBuilder files(List<FileModel> files) {
            if (files != null) {
                this.files.addAll(files);
            }
            return this;
        }

        public FileProcessingRequestModelBuilder addFile(FileModel file) {
            if (file != null) {
                this.files.add(file);
            }
            return this;
        }

        public FileProcessingRequestModelBuilder defaultOperations(List<OperationType> operations) {
            if (operations != null) {
                this.defaultOperations.addAll(operations);
            }
            return this;
        }

        public FileProcessingRequestModelBuilder addDefaultOperation(OperationType operation) {
            if (operation != null) {
                this.defaultOperations.add(operation);
            }
            return this;
        }

        public FileProcessingRequestModelBuilder addFileSpecificOperation(String fileId, List<OperationType> operations) {
            if (fileId != null && operations != null) {
                this.fileSpecificOperations.put(fileId, new ArrayList<>(operations));
            }
            return this;
        }

        public FileProcessingRequestModel build() {
            return new FileProcessingRequestModel(files, defaultOperations, fileSpecificOperations);
        }
    }
}
