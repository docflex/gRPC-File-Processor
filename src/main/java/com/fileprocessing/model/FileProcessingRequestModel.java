package com.fileprocessing.model;

import com.fileprocessing.FileSpec.OperationType;

import java.util.*;

/**
 * Represents an internal request to process multiple files.
 * Immutable and safe for concurrent use.
 */
public record FileProcessingRequestModel(List<FileModel> files, List<OperationType> defaultOperations,
                                         Map<String, List<OperationType>> fileSpecificOperations) {

    /**
     * Constructor with validation and defensive copies.
     */
    public FileProcessingRequestModel(List<FileModel> files,
                                      List<OperationType> defaultOperations,
                                      Map<String, List<OperationType>> fileSpecificOperations) {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files cannot be null or empty");
        }
        this.files = List.copyOf(files);

        this.defaultOperations = (defaultOperations != null)
                ? List.copyOf(defaultOperations)
                : Collections.emptyList();

        if (fileSpecificOperations != null) {
            Map<String, List<OperationType>> tempMap = new HashMap<>();
            for (Map.Entry<String, List<OperationType>> entry : fileSpecificOperations.entrySet()) {
                tempMap.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            this.fileSpecificOperations = Collections.unmodifiableMap(tempMap);
        } else {
            this.fileSpecificOperations = Collections.emptyMap();
        }
    }

    /**
     * Builder pattern for easier construction.
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
