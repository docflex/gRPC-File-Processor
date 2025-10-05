package com.fileprocessing.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a summary of multiple file processing operations.
 * Immutable, validated, and thread-safe.
 */
public record FileProcessingSummaryModel(
        int totalFiles,
        int successfulFiles,
        int failedFiles,
        List<FileOperationResultModel> results
) {

    /**
     * Compact constructor with validation and defensive copy.
     */
    public FileProcessingSummaryModel {
        if (totalFiles < 0 || successfulFiles < 0 || failedFiles < 0) {
            throw new IllegalArgumentException("File counts cannot be negative");
        }
        if (successfulFiles + failedFiles > totalFiles) {
            throw new IllegalArgumentException("Sum of successful and failed files cannot exceed totalFiles");
        }

        results = (results != null) ? List.copyOf(results) : Collections.emptyList();
    }

    /**
     * Builder for easier and flexible construction.
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

        public FileProcessingSummaryModelBuilder incrementSuccess() {
            this.successfulFiles++;
            this.totalFiles++;
            return this;
        }

        public FileProcessingSummaryModelBuilder incrementFailure() {
            this.failedFiles++;
            this.totalFiles++;
            return this;
        }

        public FileProcessingSummaryModel build() {
            return new FileProcessingSummaryModel(totalFiles, successfulFiles, failedFiles, results);
        }
    }
}
