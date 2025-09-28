package com.fileprocessing.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a file to be processed in the file processing microservice.
 * Immutable and validated to prevent accidental mutation or invalid state.
 *
 * @param content Immutable copy returned
 */
public record FileModel(String fileId, String fileName, byte[] content, String fileType, long sizeBytes) {

    private static final Set<String> IMAGE_TYPES = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));

    /**
     * Constructor with validation
     */
    public FileModel(String fileId, String fileName, byte[] content, String fileType, long sizeBytes) {
        this.fileId = Objects.requireNonNull(fileId, "fileId cannot be null");
        this.fileName = Objects.requireNonNull(fileName, "fileName cannot be null");
        this.content = (content != null) ? Arrays.copyOf(content, content.length) : new byte[0];
        this.fileType = Objects.requireNonNull(fileType, "fileType cannot be null").toLowerCase();
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes cannot be negative");
        }
        this.sizeBytes = sizeBytes;
    }

    /**
     * Builder for easier construction
     */
    public static FileModelBuilder builder() {
        return new FileModelBuilder();
    }

    /**
     * Returns a copy of the file content to prevent external modification.
     */
    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }

    /**
     * Checks if the file is an image (jpg, jpeg, png, gif).
     */
    public boolean isImage() {
        return IMAGE_TYPES.contains(fileType);
    }

    /**
     * Checks if the file is a PDF.
     */
    public boolean isPdf() {
        return "pdf".equalsIgnoreCase(fileType);
    }

    public static class FileModelBuilder {
        private String fileId;
        private String fileName;
        private byte[] content;
        private String fileType;
        private long sizeBytes;

        public FileModelBuilder fileId(String fileId) {
            this.fileId = fileId;
            return this;
        }

        public FileModelBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public FileModelBuilder content(byte[] content) {
            this.content = (content != null) ? Arrays.copyOf(content, content.length) : null;
            return this;
        }

        public FileModelBuilder fileType(String fileType) {
            this.fileType = fileType;
            return this;
        }

        public FileModelBuilder sizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
            return this;
        }

        public FileModel build() {
            return new FileModel(fileId, fileName, content, fileType, sizeBytes);
        }
    }
}
