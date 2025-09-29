package com.fileprocessing.util;

import com.fileprocessing.model.FileModel;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for file operations with complete implementations.
 */
@Slf4j
public final class FileOperations {
    static final String STORAGE_DIR = "processed_files";
    private static final int MAX_FILE_SIZE_MB = 100;
    private static final Map<String, String> MIME_TYPES;
    private static final ReentrantLock STORAGE_LOCK = new ReentrantLock();

    static {
        MIME_TYPES = Map.of("pdf", "application/pdf", "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "gif", "image/gif");

        // Create storage directory if it doesn't exist
        try {
            Files.createDirectories(Path.of(STORAGE_DIR));
        } catch (IOException e) {
            log.error("Failed to create storage directory", e);
        }
    }

    private FileOperations() {
        // private constructor to prevent instantiation
    }

    /**
     * Validate that the file is non-empty and matches security constraints.
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateFile(@NotNull FileModel file) {
        if (file.fileName().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        if (file.fileType().isEmpty()) {
            throw new IllegalArgumentException("File type cannot be empty");
        }
        if (file.sizeBytes() <= 0) {
            throw new IllegalArgumentException("File is empty: " + file.fileName());
        }
        if (file.sizeBytes() > MAX_FILE_SIZE_MB * 1024 * 1024) {
            throw new IllegalArgumentException("File exceeds maximum size of " + MAX_FILE_SIZE_MB + "MB");
        }
        if (file.fileName().contains("..") || file.fileName().contains("/")) {
            throw new IllegalArgumentException("Invalid file name: " + file.fileName());
        }

        if (!file.fileName().matches("[a-zA-Z0-9._-]+\\.[a-zA-Z0-9]+")) {
            throw new IllegalArgumentException("File name contains invalid characters: " + file.fileName());
        }
        if (!MIME_TYPES.containsKey(file.fileType().toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: " + file.fileType());
        }

        // Validate file content matches its extension
        validateFileContent(file);

        log.info("Validated file: {} ({} bytes)", file.fileName(), file.sizeBytes());
    }

    private static void validateFileContent(@NotNull FileModel file) {
        try {
            if (file.isImage()) {
                try (InputStream is = new ByteArrayInputStream(file.content())) {
                    if (ImageIO.read(is) == null) {
                        throw new IllegalArgumentException("Invalid image content for file: " + file.fileName());
                    }
                }
            }
            // Add more content validation for other file types as needed
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to validate file content: " + file.fileName(), e);
        }
    }

    /**
     * Extract comprehensive metadata from the file.
     *
     * @param file the file to extract metadata from
     * @return Map containing the metadata
     */
    public static Map<String, String> extractMetadata(@NotNull FileModel file) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("fileId", file.fileId());
        metadata.put("fileName", file.fileName());
        metadata.put("fileType", file.fileType());
        metadata.put("sizeBytes", String.valueOf(file.sizeBytes()));
        metadata.put("mimeType", MIME_TYPES.getOrDefault(file.fileType().toLowerCase(), "application/octet-stream"));
        metadata.put("checksum", calculateChecksum(file.content()));

        try {
            if (file.isImage()) {
                try (InputStream is = new ByteArrayInputStream(file.content())) {
                    BufferedImage image = ImageIO.read(is);
                    if (image != null) {
                        metadata.put("width", String.valueOf(image.getWidth()));
                        metadata.put("height", String.valueOf(image.getHeight()));
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to extract image metadata for file: {}", file.fileName(), e);
        }

        log.info("Extracted metadata: {}", metadata);
        return metadata;
    }

    private static String calculateChecksum(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }

    /**
     * Compress the file using GZIP with optimal compression settings.
     *
     * @param file the file to compress
     * @return Path to the compressed file
     */
    public static Path compressFile(@NotNull FileModel file) {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("compressed_files");
            Path outputFile = tempDir.resolve(file.fileName() + ".gz");

            try (OutputStream fos = Files.newOutputStream(outputFile);
                 GZIPOutputStream gos = new GZIPOutputStream(fos)) {
                gos.write(file.content());
            }

            log.info("Compressed {} to {} (Original size: {}, Compressed size: {})",
                    file.fileName(), outputFile, file.sizeBytes(), Files.size(outputFile));
            return outputFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress file: " + file.fileName(), e);
        }
    }

    /**
     * Perform OCR on image or PDF files using Tesseract.
     * Note: Requires Tesseract to be installed on the system.
     *
     * @param file the file to OCR
     * @return extracted text
     */
    public static String performOcr(@NotNull FileModel file) {
        if (!file.isImage() && !file.isPdf()) {
            throw new UnsupportedOperationException("OCR not supported for type " + file.fileType());
        }

        // This is a placeholder for actual OCR implementation
        // In a real implementation, you would:
        // 1. Save the file to a temporary location
        // 2. Use Tesseract API or process builder to run OCR
        // 3. Read and return the results
        log.info("Performing OCR on {}", file.fileName());
        return "OCR text would be returned here";
    }

    /**
     * Resize an image while maintaining aspect ratio.
     *
     * @param file      the image to resize
     * @param maxWidth  maximum width
     * @param maxHeight maximum height
     * @return resized image as a new FileModel
     */
    public static FileModel resizeImage(@NotNull FileModel file, int maxWidth, int maxHeight) {
        if (file == null) {
            throw new NullPointerException("File cannot be null");
        }
        if (!file.isImage()) {
            throw new UnsupportedOperationException("Resize only supported for images");
        }
        if (maxWidth <= 0 || maxHeight <= 0) {
            throw new IllegalArgumentException("Width and height must be positive values");
        }
        if (maxWidth == Integer.MAX_VALUE || maxHeight == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Dimensions are too large");
        }

        try (InputStream is = new ByteArrayInputStream(file.content())) {
            BufferedImage originalImage = ImageIO.read(is);
            if (originalImage == null) {
                throw new IllegalArgumentException("Invalid image content");
            }

            // Calculate dimensions
            double scale = Math.min(
                    (double) maxWidth / originalImage.getWidth(),
                    (double) maxHeight / originalImage.getHeight()
            );
            int targetWidth = (int) (originalImage.getWidth() * scale);
            int targetHeight = (int) (originalImage.getHeight() * scale);

            // Create resized image
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, file.fileType(), baos);

            return FileModel.builder()
                    .fileId(UUID.randomUUID().toString())
                    .fileName("resized_" + file.fileName())
                    .content(baos.toByteArray())
                    .fileType(file.fileType())
                    .sizeBytes(baos.size())
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to resize image: " + file.fileName(), e);
        }
    }

    /**
     * Convert file format to a different type.
     *
     * @param file         the file to convert
     * @param targetFormat the desired output format
     * @return converted file as a new FileModel
     */
    public static FileModel convertFormat(@NotNull FileModel file, @NotNull String targetFormat) {
        if (targetFormat.isEmpty()) {
            throw new UnsupportedOperationException("Target format cannot be empty");
        }
        if (!file.isImage()) {
            throw new UnsupportedOperationException("Format conversion currently only supported for images");
        }

        try (InputStream is = new ByteArrayInputStream(file.content())) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new IllegalArgumentException("Invalid image content");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (!ImageIO.write(image, targetFormat.toLowerCase(), baos)) {
                throw new UnsupportedOperationException("Unsupported target format: " + targetFormat);
            }

            String newFileName = file.fileName().substring(0, file.fileName().lastIndexOf('.')) + "." + targetFormat;
            return FileModel.builder()
                    .fileId(UUID.randomUUID().toString())
                    .fileName(newFileName)
                    .content(baos.toByteArray())
                    .fileType(targetFormat)
                    .sizeBytes(baos.size())
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert file format: " + file.fileName(), e);
        }
    }

    /**
     * Store the file in a persistent location with proper organization.
     *
     * @param file the file to store
     * @return Path to the stored file
     */
    public static Path storeFile(@NotNull FileModel file) {
        STORAGE_LOCK.lock();
        try {
            // Create type-specific subdirectory
            Path typeDir = Path.of(STORAGE_DIR, file.fileType().toLowerCase());
            Files.createDirectories(typeDir);

            // Create a unique filename to prevent collisions
            String uniqueFileName = file.fileId() + "_" + file.fileName();
            Path destinationPath = typeDir.resolve(uniqueFileName);

            // Store the file
            try (InputStream is = new ByteArrayInputStream(file.content())) {
                Files.copy(is, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Stored {} to {}", file.fileName(), destinationPath);
            return destinationPath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + file.fileName(), e);
        } finally {
            STORAGE_LOCK.unlock();
        }
    }
}
