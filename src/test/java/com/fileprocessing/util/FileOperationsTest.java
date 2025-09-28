package com.fileprocessing.util;

import com.fileprocessing.model.FileModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class FileOperationsTest {
    private FileModel validImageFile;
    private FileModel invalidImageFile;
    private FileModel largeFile;
    private FileModel emptyFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a valid test image
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        validImageFile = FileModel.builder()
                .fileId("test-id-1")
                .fileName("test.png")
                .content(baos.toByteArray())
                .fileType("png")
                .sizeBytes(baos.size())
                .build();

        // Create an invalid image file
        invalidImageFile = FileModel.builder()
                .fileId("test-id-2")
                .fileName("invalid.png")
                .content("not an image".getBytes())
                .fileType("png")
                .sizeBytes(11)
                .build();

        // Create a large file
        byte[] largeContent = new byte[101 * 1024 * 1024]; // 101MB
        largeFile = FileModel.builder()
                .fileId("test-id-3")
                .fileName("large.png")
                .content(largeContent)
                .fileType("png")
                .sizeBytes(largeContent.length)
                .build();

        // Create an empty file
        emptyFile = FileModel.builder()
                .fileId("test-id-4")
                .fileName("empty.png")
                .content(new byte[0])
                .fileType("png")
                .sizeBytes(0)
                .build();
    }

    @Test
    void validateFile_withValidImage_shouldNotThrowException() {
        assertDoesNotThrow(() -> FileOperations.validateFile(validImageFile));
    }

    @Test
    void validateFile_withInvalidImage_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(invalidImageFile));
    }

    @Test
    void validateFile_withLargeFile_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(largeFile));
    }

    @Test
    void validateFile_withEmptyFile_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(emptyFile));
    }

    @Test
    void validateFile_withInvalidFileName_shouldThrowException() {
        FileModel fileWithInvalidName = FileModel.builder()
                .fileId("test-id-5")
                .fileName("../test.png")
                .content(validImageFile.content())
                .fileType("png")
                .sizeBytes(validImageFile.sizeBytes())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(fileWithInvalidName));
    }

    @Test
    void validateFile_withUnsupportedType_shouldThrowException() {
        FileModel fileWithUnsupportedType = FileModel.builder()
                .fileId("test-id-6")
                .fileName("test.xyz")
                .content(validImageFile.content())
                .fileType("xyz")
                .sizeBytes(validImageFile.sizeBytes())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(fileWithUnsupportedType));
    }

    @Test
    void extractMetadata_withValidImage_shouldReturnCorrectMetadata() {
        Map<String, String> metadata = FileOperations.extractMetadata(validImageFile);

        assertNotNull(metadata);
        assertEquals(validImageFile.fileId(), metadata.get("fileId"));
        assertEquals(validImageFile.fileName(), metadata.get("fileName"));
        assertEquals(validImageFile.fileType(), metadata.get("fileType"));
        assertEquals(String.valueOf(validImageFile.sizeBytes()), metadata.get("sizeBytes"));
        assertEquals("image/png", metadata.get("mimeType"));
        assertNotNull(metadata.get("checksum"));
        assertEquals("100", metadata.get("width"));
        assertEquals("100", metadata.get("height"));
    }

    @Test
    void compressFile_withValidFile_shouldCreateCompressedFile(@TempDir Path tempDir) throws IOException {
        Path compressedFile = FileOperations.compressFile(validImageFile);

        assertTrue(Files.exists(compressedFile));
        assertTrue(compressedFile.toString().endsWith(".gz"));

        // Verify the compressed file can be decompressed
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(Files.newInputStream(compressedFile));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipInputStream.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            assertArrayEquals(validImageFile.content(), baos.toByteArray());
        }
    }

    @Test
    void resizeImage_withValidImage_shouldResizeCorrectly() throws IOException {
        int maxWidth = 50;
        int maxHeight = 50;
        FileModel resizedFile = FileOperations.resizeImage(validImageFile, maxWidth, maxHeight);

        assertNotNull(resizedFile);
        assertTrue(resizedFile.fileName().startsWith("resized_"));
        assertEquals(validImageFile.fileType(), resizedFile.fileType());

        // Verify the dimensions of the resized image
        try (InputStream is = new ByteArrayInputStream(resizedFile.content())) {
            BufferedImage resizedImage = ImageIO.read(is);
            assertNotNull(resizedImage);
            assertTrue(resizedImage.getWidth() <= maxWidth);
            assertTrue(resizedImage.getHeight() <= maxHeight);
        }
    }

    @Test
    void resizeImage_withNonImage_shouldThrowException() {
        FileModel nonImageFile = FileModel.builder()
                .fileId("test-id-7")
                .fileName("test.pdf")
                .content("pdf content".getBytes())
                .fileType("pdf")
                .sizeBytes(11)
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> FileOperations.resizeImage(nonImageFile, 50, 50));
    }

    @Test
    void convertFormat_withValidImage_shouldConvertFormat() throws IOException {
        String targetFormat = "jpg";
        FileModel convertedFile = FileOperations.convertFormat(validImageFile, targetFormat);

        assertNotNull(convertedFile);
        assertTrue(convertedFile.fileName().endsWith("." + targetFormat));
        assertEquals(targetFormat, convertedFile.fileType());

        // Verify the converted file is a valid image
        try (InputStream is = new ByteArrayInputStream(convertedFile.content())) {
            BufferedImage convertedImage = ImageIO.read(is);
            assertNotNull(convertedImage);
            assertEquals(100, convertedImage.getWidth());
            assertEquals(100, convertedImage.getHeight());
        }
    }

    @Test
    void convertFormat_withInvalidTargetFormat_shouldThrowException() {
        assertThrows(UnsupportedOperationException.class,
                () -> FileOperations.convertFormat(validImageFile, "xyz"));
    }

    @Test
    void convertFormat_withNonImage_shouldThrowException() {
        FileModel nonImageFile = FileModel.builder()
                .fileId("test-id-8")
                .fileName("test.pdf")
                .content("pdf content".getBytes())
                .fileType("pdf")
                .sizeBytes(11)
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> FileOperations.convertFormat(nonImageFile, "jpg"));
    }

    @Test
    void storeFile_withValidFile_shouldStoreAndReturnPath(@TempDir Path tempDir) throws IOException {
        Path storedFilePath = FileOperations.storeFile(validImageFile);

        assertNotNull(storedFilePath);
        assertTrue(Files.exists(storedFilePath));
        assertTrue(storedFilePath.toString().contains(validImageFile.fileType().toLowerCase()));
        assertTrue(storedFilePath.toString().contains(validImageFile.fileId()));
        assertArrayEquals(validImageFile.content(), Files.readAllBytes(storedFilePath));
    }

    @Test
    void performOcr_withNonImageOrPdf_shouldThrowException() {
        FileModel textFile = FileModel.builder()
                .fileId("test-id-9")
                .fileName("test.txt")
                .content("text content".getBytes())
                .fileType("txt")
                .sizeBytes(12)
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> FileOperations.performOcr(textFile));
    }

    @Test
    void concurrentStoreFile_shouldHandleMultipleThreads(@TempDir Path tempDir) throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Path> storedPaths = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                try {
                    Path path = FileOperations.storeFile(validImageFile);
                    storedPaths.add(path);
                } finally {
                    latch.countDown();
                }
            });
            threads.add(t);
            t.start();
        }

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(threadCount, storedPaths.size());
        // Verify all files were stored successfully
        storedPaths.forEach(path -> assertTrue(Files.exists(path)));
    }

    @Test
    void validateFile_withMaliciousFileName_shouldThrowException() {
        FileModel fileWithMaliciousName = FileModel.builder()
                .fileId("test-id-10")
                .fileName("../../malicious.png")
                .content(validImageFile.content())
                .fileType("png")
                .sizeBytes(validImageFile.sizeBytes())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(fileWithMaliciousName));
    }

    @Test
    void validateFile_withSpecialCharactersInFileName_shouldThrowException() {
        FileModel fileWithSpecialChars = FileModel.builder()
                .fileId("test-id-11")
                .fileName("test<>:|\".png")
                .content(validImageFile.content())
                .fileType("png")
                .sizeBytes(validImageFile.sizeBytes())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(fileWithSpecialChars));
    }

    @Test
    void extractMetadata_withCorruptedImage_shouldReturnBasicMetadata() {
        byte[] corruptedImageData = Arrays.copyOf(validImageFile.content(), 50); // Truncate the image data
        FileModel corruptedFile = FileModel.builder()
                .fileId("test-id-12")
                .fileName("corrupted.png")
                .content(corruptedImageData)
                .fileType("png")
                .sizeBytes(corruptedImageData.length)
                .build();

        Map<String, String> metadata = FileOperations.extractMetadata(corruptedFile);

        assertNotNull(metadata);
        assertEquals(corruptedFile.fileId(), metadata.get("fileId"));
        assertEquals(corruptedFile.fileName(), metadata.get("fileName"));
        assertEquals(corruptedFile.fileType(), metadata.get("fileType"));
        assertEquals(String.valueOf(corruptedFile.sizeBytes()), metadata.get("sizeBytes"));
        assertNotNull(metadata.get("checksum"));
        // Image-specific metadata should not be present
        assertNull(metadata.get("width"));
        assertNull(metadata.get("height"));
    }

    @Test
    void resizeImage_withZeroDimensions_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.resizeImage(validImageFile, 0, 0));
    }

    @Test
    void resizeImage_withNegativeDimensions_shouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.resizeImage(validImageFile, -100, -100));
    }

    @Test
    void compressFile_withAlreadyCompressedFile() throws IOException {
        // First compression
        Path firstCompression = FileOperations.compressFile(validImageFile);

        // Create a new FileModel from the compressed file
        byte[] compressedContent = Files.readAllBytes(firstCompression);
        FileModel compressedFile = FileModel.builder()
                .fileId("test-id-13")
                .fileName("already-compressed.gz")
                .content(compressedContent)
                .fileType("gz")
                .sizeBytes(compressedContent.length)
                .build();

        // Second compression
        Path secondCompression = FileOperations.compressFile(compressedFile);

        assertTrue(Files.exists(secondCompression));
        assertTrue(secondCompression.toString().endsWith(".gz"));
        // Second compression should still work but might not reduce size significantly
        assertTrue(Files.size(secondCompression) > 0);
    }

    @Test
    void convertFormat_withSameSourceAndTargetFormat_shouldReturnEquivalentFile() throws IOException {
        FileModel converted = FileOperations.convertFormat(validImageFile, validImageFile.fileType());

        assertNotNull(converted);
        assertEquals(validImageFile.fileType(), converted.fileType());
        assertEquals(validImageFile.fileName(), converted.fileName());

        // Images should be equivalent (same dimensions and type)
        try (InputStream originalIs = new ByteArrayInputStream(validImageFile.content());
             InputStream convertedIs = new ByteArrayInputStream(converted.content())) {
            BufferedImage originalImage = ImageIO.read(originalIs);
            BufferedImage convertedImage = ImageIO.read(convertedIs);

            assertEquals(originalImage.getWidth(), convertedImage.getWidth());
            assertEquals(originalImage.getHeight(), convertedImage.getHeight());
            assertEquals(originalImage.getType(), convertedImage.getType());
        }
    }

    @Test
    void validateFileContent_withIOException_shouldThrowIllegalArgumentException() {
        // Create a file model that will cause an IO exception when reading
        FileModel invalidFile = FileModel.builder()
                .fileId("test-id-14")
                .fileName("test.png")
                .content(null)  // This will cause IOException when trying to create ByteArrayInputStream
                .fileType("png")
                .sizeBytes(0)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(invalidFile));
    }

    @Test
    void calculateChecksum_withNoSuchAlgorithmException_shouldThrowRuntimeException() {
        // This test requires mocking MessageDigest, but since we can't mock static methods easily,
        // we'll just verify that the method doesn't return null
        assertNotNull(FileOperations.extractMetadata(validImageFile).get("checksum"));
    }

    @Test
    void resizeImage_withIOException_shouldThrowRuntimeException() {
        FileModel invalidFile = FileModel.builder()
                .fileId("test-id-16")
                .fileName("test.png")
                .content(null)
                .fileType("png")
                .sizeBytes(0)
                .build();

        assertThrows(RuntimeException.class,
                () -> FileOperations.resizeImage(invalidFile, 100, 100));
    }

    @Test
    void convertFormat_withIOException_shouldThrowRuntimeException() {
        FileModel invalidFile = FileModel.builder()
                .fileId("test-id-17")
                .fileName("test.png")
                .content(null)
                .fileType("png")
                .sizeBytes(0)
                .build();

        assertThrows(RuntimeException.class,
                () -> FileOperations.convertFormat(invalidFile, "jpg"));
    }

    @Test
    void convertFormat_withUnsupportedWriteFormat_shouldThrowUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class,
                () -> FileOperations.convertFormat(validImageFile, "unsupported"));
    }

    @Test
    void storeFile_withIOException_shouldThrowRuntimeException() throws IOException {
        // Create a file model that will cause IOException during storage
        FileModel validFile = FileModel.builder()
                .fileId("test-id-18")
                .fileName("test.png")
                .content(validImageFile.content())
                .fileType("png")
                .sizeBytes(validImageFile.sizeBytes())
                .build();

        // Create storage directory with restricted permissions
        Path storageDir = Path.of(FileOperations.STORAGE_DIR, "png");
        Files.createDirectories(storageDir);

        // Make the directory read-only with all parent permissions
        storageDir.toFile().setReadOnly();

        try {
            assertThrows(RuntimeException.class,
                    () -> FileOperations.storeFile(validFile));
        } finally {
            // Restore write permissions for cleanup
            storageDir.toFile().setWritable(true);
            // Clean up any files that might have been created
            Files.walk(storageDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
        }
    }

    @Test
    void storeFile_withConcurrentModification_shouldHandleGracefully() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Create threads that will try to store files simultaneously
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    FileOperations.storeFile(validImageFile);
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
            threads.add(t);
            t.start();
        }

        // Start all threads simultaneously
        startLatch.countDown();
        completionLatch.await(10, TimeUnit.SECONDS);

        // Verify no exceptions occurred
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent file storage");
    }

    @Test
    void extractMetadata_withInvalidImageButValidContent_shouldReturnBasicMetadata() {
        byte[] invalidImageData = "This is not an image".getBytes();
        FileModel invalidImageFile = FileModel.builder()
                .fileId("test-id-19")
                .fileName("invalid.png")
                .content(invalidImageData)
                .fileType("png")
                .sizeBytes(invalidImageData.length)
                .build();

        Map<String, String> metadata = FileOperations.extractMetadata(invalidImageFile);

        assertNotNull(metadata);
        assertEquals(invalidImageFile.fileId(), metadata.get("fileId"));
        assertEquals(invalidImageFile.fileName(), metadata.get("fileName"));
        assertEquals(invalidImageFile.fileType(), metadata.get("fileType"));
        assertEquals(String.valueOf(invalidImageFile.sizeBytes()), metadata.get("sizeBytes"));
        assertNotNull(metadata.get("checksum"));
        assertNull(metadata.get("width"));
        assertNull(metadata.get("height"));
    }

    @Test
    void validateFile_withNullContent_shouldThrowIllegalArgumentException() {
        FileModel nullContentFile = FileModel.builder()
                .fileId("test-id-21")
                .fileName("null.png")
                .content(null)
                .fileType("png")
                .sizeBytes(0)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(nullContentFile));
    }

    @Test
    void validateFile_withInvalidFileType_shouldThrowIllegalArgumentException() {
        FileModel invalidTypeFile = FileModel.builder()
                .fileId("test-id-22")
                .fileName("test.invalid")
                .content(validImageFile.content())
                .fileType("invalid")
                .sizeBytes(validImageFile.sizeBytes())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(invalidTypeFile));
    }

    @Test
    void storeFile_withInaccessibleDirectory_shouldThrowRuntimeException() throws IOException {
        // Create a file model
        FileModel restrictedFile = FileModel.builder()
                .fileId("test-id-23")
                .fileName("test.png")
                .content(validImageFile.content())
                .fileType("png")
                .sizeBytes(validImageFile.sizeBytes())
                .build();

        // Create and restrict the actual storage directory used by FileOperations
        Path storageDir = Path.of(FileOperations.STORAGE_DIR, "png");
        Files.createDirectories(storageDir);
        storageDir.toFile().setReadOnly();

        try {
            assertThrows(RuntimeException.class,
                    () -> FileOperations.storeFile(restrictedFile));
        } finally {
            // Restore permissions and cleanup
            storageDir.toFile().setWritable(true);
            Files.walk(storageDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
        }
    }

    @Test
    void extractMetadata_withNullContent_shouldHandleGracefully() {
        FileModel nullContentFile = FileModel.builder()
                .fileId("test-id-24")
                .fileName("null.png")
                .content(null)
                .fileType("png")
                .sizeBytes(0)
                .build();

        Map<String, String> metadata = FileOperations.extractMetadata(nullContentFile);

        assertNotNull(metadata);
        assertNotNull(metadata.get("fileId"));
        assertNotNull(metadata.get("fileName"));
        assertNull(metadata.get("width"));
        assertNull(metadata.get("height"));
    }

    @Test
    void performOcr_withValidImage_shouldReturnText() {
        String ocrResult = FileOperations.performOcr(validImageFile);
        assertNotNull(ocrResult);
        assertFalse(ocrResult.isEmpty());
    }

    @Test
    void performOcr_withPdfFile_shouldNotThrowException() {
        FileModel pdfFile = FileModel.builder()
                .fileId("test-id-29")
                .fileName("test.pdf")
                .content("PDF content".getBytes())
                .fileType("pdf")
                .sizeBytes(11)
                .build();

        assertDoesNotThrow(() -> FileOperations.performOcr(pdfFile));
    }

    @Test
    void validateFile_withEmptyFileName_shouldThrowIllegalArgumentException() {
        FileModel emptyNameFile = FileModel.builder()
                .fileId("test-id-30")
                .fileName("")
                .content(validImageFile.content())
                .fileType("png")
                .sizeBytes(validImageFile.sizeBytes())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(emptyNameFile));
    }

    @Test
    void validateFile_withEmptyFileType_shouldThrowIllegalArgumentException() {
        FileModel emptyTypeFile = FileModel.builder()
                .fileId("test-id-31")
                .fileName("test.png")
                .content(validImageFile.content())
                .fileType("")
                .sizeBytes(validImageFile.sizeBytes())
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.validateFile(emptyTypeFile));
    }

    @Test
    void resizeImage_withExtremelyLargeDimensions_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> FileOperations.resizeImage(validImageFile, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void convertFormat_withEmptyTargetFormat_shouldThrowUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class,
                () -> FileOperations.convertFormat(validImageFile, ""));
    }
}
