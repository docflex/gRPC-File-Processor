package com.fileprocessing.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a summary of multiple file processing operations.
 * Immutable and thread-safe.
 */
public record FileProcessingSummaryModel(int totalFiles, int successfulFiles, int failedFiles,
                                         List<FileOperationResultModel> results) {

    /**
     * Constructor with validation and defensive copy.
     */
    public FileProcessingSummaryModel(int totalFiles,
                                      int successfulFiles,
                                      int failedFiles,
                                      List<FileOperationResultModel> results) {

        if (totalFiles < 0 || successfulFiles < 0 || failedFiles < 0) {
            throw new IllegalArgumentException("File counts cannot be negative");
        }
        if (results == null) {
            results = Collections.emptyList();
        }

        this.totalFiles = totalFiles;
        this.successfulFiles = successfulFiles;
        this.failedFiles = failedFiles;
        this.results = List.copyOf(results);
    }

    /**
     * Builder pattern for safer and flexible construction.
     */
    public static FileProcessingSummaryModelBuilder builder() {
        return new FileProcessingSummaryModelBuilder();
    }

    public static class FileProcessingSummaryModelBuilder {
        private final List<FileOperationResultModel> results = new ArrayList<>();
        private int totalFiles;
        private int successfulFiles;
        private int failedFiles;

        public FileProcessingSummaryModelBuilder totalFiles(int totalFiles) {
            this.totalFiles = totalFiles;
            return this;
        }

        public FileProcessingSummaryModelBuilder successfulFiles(int successfulFiles) {
            this.successfulFiles = successfulFiles;
            return this;
        }

        public FileProcessingSummaryModelBuilder failedFiles(int failedFiles) {
            this.failedFiles = failedFiles;
            return this;
        }

        public FileProcessingSummaryModelBuilder results(List<FileOperationResultModel> results) {
            if (results != null) {
                this.results.addAll(results);
            }
            return this;
        }

        public FileProcessingSummaryModelBuilder addResult(FileOperationResultModel result) {
            if (result != null) {
                this.results.add(result);
            }
            return this;
        }

        public FileProcessingSummaryModel build() {
            return new FileProcessingSummaryModel(totalFiles, successfulFiles, failedFiles, results);
        }
    }
}
