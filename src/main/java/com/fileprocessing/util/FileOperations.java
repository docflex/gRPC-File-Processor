package com.fileprocessing.util;

import com.fileprocessing.model.FileModel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for placeholder file operations.
 * Can later be replaced with real implementations.
 */
@Slf4j
public final class FileOperations {

    private FileOperations() {
        // private constructor to prevent instantiation
    }

    /**
     * Validate that the file is non-empty and matches basic constraints.
     *
     * @param file the file to validate
     */
    public static void validateFile(@NotNull FileModel file) {
        if (file.sizeBytes() <= 0) {
            throw new IllegalArgumentException("File is empty: " + file.fileName());
        }
        if (file.fileName().contains("..") || file.fileName().contains("/")) {
            throw new IllegalArgumentException("Invalid file name: " + file.fileName());
        }
        log.debug("Validated file: {} ({} bytes)", file.fileName(), file.sizeBytes());
    }

    /**
     * Extract basic metadata from the file.
     * This example extracts file size and type.
     *
     * @param file the file to extract metadata from
     */
    public static void extractMetadata(@NotNull FileModel file) {
        // Here you could add real metadata extraction like PDF page count, image dimensions, etc.
        String metadata = String.format("FileID=%s, Name=%s, Type=%s, Size=%d bytes",
                file.fileId(), file.fileName(), file.fileType(), file.sizeBytes());
        log.debug("Extracted metadata: {}", metadata);
    }

    /**
     * Compress the file using GZIP and save it to a temporary location.
     *
     * @param file the file to compress
     */
    public static void compressFile(@NotNull FileModel file) {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("compressed_files");
            Path outputFile = tempDir.resolve(file.fileName() + ".gz");

            try (OutputStream fos = Files.newOutputStream(outputFile);
                 GZIPOutputStream gos = new GZIPOutputStream(fos)) {
                gos.write(file.content());
            }

            log.debug("Compressed {} to {}", file.fileName(), outputFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress file: " + file.fileName(), e);
        }
    }

    public static void performOcr(@NotNull FileModel file) {
        if (!file.isImage() && !file.isPdf()) {
            throw new UnsupportedOperationException("OCR not supported for type " + file.fileType());
        }
        log.debug("Performing OCR on {}", file.fileName());
    }

    public static void resizeImage(@NotNull FileModel file) {
        if (!file.isImage()) {
            throw new UnsupportedOperationException("Resize only supported for images");
        }
        log.debug("Resizing image {}", file.fileName());
    }

    public static void convertFormat(@NotNull FileModel file) {
        log.debug("Converting format of {}", file.fileName());
    }

    public static void storeFile(@NotNull FileModel file) {
        log.debug("Storing {}", file.fileName());
    }
}
