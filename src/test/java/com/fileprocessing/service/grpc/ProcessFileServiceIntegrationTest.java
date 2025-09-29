package com.fileprocessing.service.grpc;

import com.fileprocessing.FileSpec.File;
import com.fileprocessing.FileSpec.FileOperationResult;
import com.fileprocessing.FileSpec.FileProcessingRequest;
import com.fileprocessing.FileSpec.FileProcessingSummary;
import com.fileprocessing.FileSpec.OperationType;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    properties = {
        "grpc.server.inProcessName=test",
        "grpc.server.port=-1",
        "grpc.client.inProcess.address=in-process:test"
    }
)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext
class ProcessFileServiceIntegrationTest {

    @GrpcClient("inProcess")
    private com.fileprocessing.FileProcessingServiceGrpc.FileProcessingServiceBlockingStub fileProcessingStub;

    private byte[] createDummyPngImage() {
        // Create a 1x1 black PNG image
        byte[] png = {
            (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0D,
            (byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x08, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x1F, (byte) 0x15, (byte) 0xC4, (byte) 0x89,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0D,
            (byte) 0x49, (byte) 0x44, (byte) 0x41, (byte) 0x54,
            (byte) 0x08, (byte) 0xD7, (byte) 0x63, (byte) 0x60,
            (byte) 0x60, (byte) 0x60, (byte) 0x60, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x00,
            (byte) 0x01, (byte) 0x0D, (byte) 0x69, (byte) 0x28,
            (byte) 0xD4, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x49, (byte) 0x45, (byte) 0x4E,
            (byte) 0x44, (byte) 0xAE, (byte) 0x42, (byte) 0x60,
            (byte) 0x82
        };
        return png;
    }

    @Test
    void whenProcessingSingleValidFile_thenSuccessful() throws IOException {
        // Given
        byte[] imageContent = createDummyPngImage();
        String fileId = UUID.randomUUID().toString();

        File testFile = File.newBuilder()
            .setFileId(fileId)
            .setFileName("test.png")
            .setContent(ByteString.copyFrom(imageContent))
            .setFileType("png")
            .setSizeBytes(imageContent.length)
            .build();

        FileProcessingRequest request = FileProcessingRequest.newBuilder()
            .addFiles(testFile)
            .addOperations(OperationType.VALIDATE)
            .build();

        // When
        FileProcessingSummary summary = fileProcessingStub.processFile(request);

        // Then
        assertNotNull(summary);
        assertEquals(1, summary.getTotalFiles());
        assertEquals(1, summary.getSuccessfulFiles());
        assertEquals(0, summary.getFailedFiles());

        List<FileOperationResult> results = summary.getResultsList();
        assertFalse(results.isEmpty());

        FileOperationResult firstResult = results.get(0);
        assertEquals(fileId, firstResult.getFileId());
        assertNotNull(firstResult.getStartTime());
        assertNotNull(firstResult.getEndTime());
    }

    @Test
    void whenProcessingMultipleFiles_thenAllProcessedSuccessfully() {
        // Given
        byte[] imageContent = createDummyPngImage();
        String fileId1 = UUID.randomUUID().toString();
        String fileId2 = UUID.randomUUID().toString();

        File testFile1 = File.newBuilder()
            .setFileId(fileId1)
            .setFileName("test1.png")
            .setContent(ByteString.copyFrom(imageContent))
            .setFileType("png")
            .setSizeBytes(imageContent.length)
            .build();

        File testFile2 = File.newBuilder()
            .setFileId(fileId2)
            .setFileName("test2.png")
            .setContent(ByteString.copyFrom(imageContent))
            .setFileType("png")
            .setSizeBytes(imageContent.length)
            .build();

        FileProcessingRequest request = FileProcessingRequest.newBuilder()
            .addFiles(testFile1)
            .addFiles(testFile2)
            .addOperations(OperationType.VALIDATE)
            .addOperations(OperationType.METADATA_EXTRACTION)
            .build();

        // When
        FileProcessingSummary summary = fileProcessingStub.processFile(request);

        // Then
        assertNotNull(summary);
        assertEquals(2, summary.getTotalFiles());
        assertEquals(4, summary.getSuccessfulFiles());
        assertEquals(0, summary.getFailedFiles());

        List<FileOperationResult> results = summary.getResultsList();
        assertEquals(4, results.size()); // 2 operations per file

        Set<String> processedFileIds = new HashSet<>();
        results.forEach(result -> {
            assertTrue(result.getFileId().equals(fileId1) || result.getFileId().equals(fileId2));
            assertNotNull(result.getStartTime());
            assertNotNull(result.getEndTime());
            processedFileIds.add(result.getFileId());
        });
        assertEquals(2, processedFileIds.size()); // Verify both files were processed
    }

    @Test
    void whenProcessingLargeFile_thenHandledCorrectly() throws IOException {
        // Given
        byte[] header = createDummyPngImage();
        byte[] largeContent = new byte[5 * 1024 * 1024];
        System.arraycopy(header, 0, largeContent, 0, header.length);
        String fileId = UUID.randomUUID().toString();

        File largeFile = File.newBuilder()
            .setFileId(fileId)
            .setFileName("large.png")
            .setContent(ByteString.copyFrom(largeContent))
            .setFileType("png")
            .setSizeBytes(largeContent.length)
            .build();

        FileProcessingRequest request = FileProcessingRequest.newBuilder()
            .addFiles(largeFile)
            .addOperations(OperationType.VALIDATE)
            .addOperations(OperationType.FILE_COMPRESSION)
            .build();

        // When
        FileProcessingSummary summary = fileProcessingStub.processFile(request);

        // Then
        assertNotNull(summary);
        assertEquals(1, summary.getTotalFiles());
        assertEquals(2, summary.getSuccessfulFiles());
        assertEquals(0, summary.getFailedFiles());

        List<FileOperationResult> results = summary.getResultsList();
        assertEquals(2, results.size()); // VALIDATE and FILE_COMPRESSION operations

        FileOperationResult compressionResult = results.stream()
            .filter(r -> r.getResultLocation().endsWith(".gz"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No compression result found"));

        assertEquals(fileId, compressionResult.getFileId());

        // Verify the compressed file exists and is smaller
        Path compressedFile = Path.of(compressionResult.getResultLocation());
        assertTrue(Files.exists(compressedFile));
        assertTrue(Files.size(compressedFile) < largeContent.length);
    }

    @Test
    void whenProcessingInvalidFile_thenFailsGracefully() {
        // Given
        String fileId = UUID.randomUUID().toString();

        File invalidFile = File.newBuilder()
            .setFileId(fileId)
            .setFileName("invalid.xyz")
            .setContent(ByteString.copyFrom(new byte[0]))
            .setFileType("xyz") // Invalid type
            .setSizeBytes(0)
            .build();

        FileProcessingRequest request = FileProcessingRequest.newBuilder()
            .addFiles(invalidFile)
            .addOperations(OperationType.VALIDATE)
            .build();

        // When
        FileProcessingSummary summary = fileProcessingStub.processFile(request);

        // Then
        assertNotNull(summary);
        assertEquals(1, summary.getTotalFiles());
        assertEquals(0, summary.getSuccessfulFiles());
        assertEquals(1, summary.getFailedFiles());

        FileOperationResult result = summary.getResults(0);
        assertEquals(fileId, result.getFileId());
        assertEquals(OperationType.VALIDATE, result.getOperation());
        assertTrue(result.getDetails().contains("File is empty"));
    }

    // TODO - Re-enable when OCR operation is implemented
//    @Test
//    void whenProcessingWithInvalidOperation_thenFailsGracefully() {
//        // Given
//        byte[] imageContent = createDummyPngImage();
//        String fileId = UUID.randomUUID().toString();
//
//        File testFile = File.newBuilder()
//            .setFileId(fileId)
//            .setFileName("test.png")
//            .setContent(ByteString.copyFrom(imageContent))
//            .setFileType("png")
//            .setSizeBytes(imageContent.length)
//            .build();
//
//        FileProcessingRequest request = FileProcessingRequest.newBuilder()
//            .addFiles(testFile)
//            .addOperations(OperationType.OCR_TEXT_EXTRACTION) // Invalid operation for png
//            .build();
//
//        // When
//        FileProcessingSummary summary = fileProcessingStub.processFile(request);
//
//        // Then
//        assertNotNull(summary);
//        assertEquals(1, summary.getTotalFiles());
//        assertEquals(0, summary.getSuccessfulFiles());
//        assertEquals(1, summary.getFailedFiles());
//
//        FileOperationResult result = summary.getResults(0);
//        assertEquals(fileId, result.getFileId());
//        assertEquals(OperationType.OCR_TEXT_EXTRACTION, result.getOperation());
//        assertTrue(result.getDetails().toLowerCase().contains("ocr not supported"));
//    }

    @Test
    void whenProcessingWithTimeout_thenFailsWithTimeout() {
        // Given
        byte[] largeContent = new byte[50 * 1024 * 1024]; // 50MB to force timeout
        String fileId = UUID.randomUUID().toString();

        File largeFile = File.newBuilder()
            .setFileId(fileId)
            .setFileName("timeout_test.png")
            .setContent(ByteString.copyFrom(largeContent))
            .setFileType("png")
            .setSizeBytes(largeContent.length)
            .build();

        FileProcessingRequest request = FileProcessingRequest.newBuilder()
            .addFiles(largeFile)
            .addOperations(OperationType.FILE_COMPRESSION)
            .build();

        // When/Then
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
            () -> fileProcessingStub.withDeadlineAfter(1, TimeUnit.MILLISECONDS).processFile(request));
        assertEquals(Status.Code.DEADLINE_EXCEEDED, exception.getStatus().getCode());
    }

    @Test
    void whenConcurrentProcessing_thenAllProcessedCorrectly() throws InterruptedException {
        // Given
        byte[] imageContent = createDummyPngImage();
        int numFiles = 5;
        FileProcessingRequest[] requests = new FileProcessingRequest[numFiles];
        String[] fileIds = new String[numFiles];

        for (int i = 0; i < numFiles; i++) {
            fileIds[i] = UUID.randomUUID().toString();

            File testFile = File.newBuilder()
                .setFileId(fileIds[i])
                .setFileName("concurrent_" + i + ".png")
                .setContent(ByteString.copyFrom(imageContent))
                .setFileType("png")
                .setSizeBytes(imageContent.length)
                .build();

            requests[i] = FileProcessingRequest.newBuilder()
                .addFiles(testFile)
                .addOperations(OperationType.VALIDATE)
                .addOperations(OperationType.METADATA_EXTRACTION)
                .build();
        }

        // When
        Thread[] threads = new Thread[numFiles];
        FileProcessingSummary[] summaries = new FileProcessingSummary[numFiles];

        for (int i = 0; i < numFiles; i++) {
            final int index = i;
            threads[i] = new Thread(() -> summaries[index] = fileProcessingStub.processFile(requests[index]));
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        for (int i = 0; i < numFiles; i++) {
            FileProcessingSummary summary = summaries[i];
            assertNotNull(summary);
            assertEquals(1, summary.getTotalFiles());
            assertEquals(2, summary.getSuccessfulFiles());
            assertEquals(0, summary.getFailedFiles());

            List<FileOperationResult> results = summary.getResultsList();
            assertEquals(2, results.size()); // VALIDATE and METADATA_EXTRACTION

            int finalI = i;
            results.forEach(result -> {
                assertEquals(fileIds[finalI], result.getFileId());
                assertNotNull(result.getStartTime());
                assertNotNull(result.getEndTime());
            });
        }
    }
}
