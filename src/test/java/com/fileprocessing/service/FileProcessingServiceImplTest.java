package com.fileprocessing.service;

import com.fileprocessing.FileSpec.FileUploadRequest;
import com.fileprocessing.FileSpec.File;
import com.fileprocessing.FileSpec.FileOperationResult;
import com.fileprocessing.FileSpec.FileProcessingRequest;
import com.fileprocessing.FileSpec.FileProcessingSummary;
import com.fileprocessing.model.FileProcessingRequestModel;
import com.fileprocessing.model.FileProcessingSummaryModel;
import com.fileprocessing.service.grpc.LiveFileProcessingService;
import com.fileprocessing.service.grpc.StreamFileOperationsService;
import com.fileprocessing.service.grpc.UploadFilesService;
import com.fileprocessing.service.monitoring.FileProcessingMetrics;
import com.fileprocessing.service.grpc.ProcessFileService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileProcessingServiceImplTest {

    @Mock
    private FileProcessingMetrics processingMetrics;

    @Mock
    private ProcessFileService processFileService;

    @Mock
    private StreamFileOperationsService streamFileOperationsService;

    @Mock
    private UploadFilesService uploadFilesService;

    @Mock
    private LiveFileProcessingService liveFileProcessingService;

    @Mock
    private StreamObserver<FileProcessingSummary> responseObserver;

    private FileProcessingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileProcessingServiceImpl(processingMetrics, processFileService, streamFileOperationsService, uploadFilesService, liveFileProcessingService);
    }

    @Test
    void processFile_SuccessfulProcessing() {
        // Given
        FileProcessingRequest request = FileProcessingRequest.newBuilder()
                .addFiles(File.newBuilder()
                        .setFileId("test-id")
                        .setFileName("test.txt")
                        .setFileType("txt")
                        .setSizeBytes(100)
                        .build())
                .build();

        FileProcessingSummaryModel mockSummaryModel = new FileProcessingSummaryModel(
                1, // totalFiles
                1, // successfulFiles
                0, // failedFiles
                Collections.emptyList() // results
        );
        when(processFileService.processFiles(any(FileProcessingRequestModel.class)))
                .thenReturn(mockSummaryModel);

        // When
        service.processFile(request, responseObserver);

        // Then
        verify(processingMetrics).incrementActiveRequests();
        verify(processingMetrics).decrementActiveRequests();
        verify(processingMetrics).addRequestDuration(anyLong());
        verify(processingMetrics, never()).incrementFailedRequests();

        verify(responseObserver).onNext(any(FileProcessingSummary.class));
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());
    }

    @Test
    void processFile_HandlesProcessingException() {
        // Given
        FileProcessingRequest request = FileProcessingRequest.newBuilder()
                .addFiles(File.newBuilder()
                        .setFileId("test-id")
                        .setFileName("test.txt")
                        .setFileType("txt")
                        .setSizeBytes(100)
                        .build())
                .build();

        RuntimeException mockException = new RuntimeException("Processing failed");
        when(processFileService.processFiles(any(FileProcessingRequestModel.class)))
                .thenThrow(mockException);

        // When
        service.processFile(request, responseObserver);

        // Then
        verify(processingMetrics).incrementActiveRequests();
        verify(processingMetrics).decrementActiveRequests();
        verify(processingMetrics).addRequestDuration(anyLong());
        verify(processingMetrics).incrementFailedRequests();

        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();

        ArgumentCaptor<StatusRuntimeException> errorCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(responseObserver).onError(errorCaptor.capture());

        StatusRuntimeException capturedError = errorCaptor.getValue();
        assertEquals(Status.Code.INTERNAL, capturedError.getStatus().getCode());
        assertTrue(capturedError.getMessage().contains("Processing failed"));
    }

    @Test
    void processFile_MetricsTracking() {
        // Given
        FileProcessingRequest request = FileProcessingRequest.newBuilder()
                .addFiles(File.newBuilder()
                        .setFileId("test-id")
                        .setFileName("test.txt")
                        .setFileType("txt")
                        .setSizeBytes(100)
                        .build())
                .build();

        when(processFileService.processFiles(any(FileProcessingRequestModel.class)))
                .thenReturn(new FileProcessingSummaryModel(1, 1, 0, Collections.emptyList()));

        // When
        service.processFile(request, responseObserver);

        // Then
        verify(processingMetrics).incrementActiveRequests();
        verify(processingMetrics).decrementActiveRequests();
        verify(processingMetrics).addRequestDuration(anyLong());
    }

    @Test
    void streamFileOperations_DelegatesCorrectly() {
        // Given
        FileProcessingRequest request = FileProcessingRequest.newBuilder()
                .addFiles(File.newBuilder()
                        .setFileId("test-id")
                        .setFileName("test.txt")
                        .setFileType("txt")
                        .setSizeBytes(100)
                        .build())
                .build();
        @SuppressWarnings("unchecked")
        StreamObserver<FileOperationResult> observer = mock(StreamObserver.class);

        // When
        service.streamFileOperations(request, observer);

        // Then
        verify(processingMetrics).incrementActiveRequests();
        verify(streamFileOperationsService).streamFileOperations(any(FileProcessingRequestModel.class), eq(observer), anyLong());
        verify(processingMetrics, never()).incrementFailedRequests();
    }

    @Test
    void streamFileOperations_HandlesExceptions() {
        // Given
        FileProcessingRequest request = FileProcessingRequest.newBuilder()
                .addFiles(File.newBuilder()
                        .setFileId("test-id")
                        .setFileName("test.txt")
                        .build())
                .build();
        @SuppressWarnings("unchecked")
        StreamObserver<FileOperationResult> observer = mock(StreamObserver.class);

        RuntimeException expectedException = new RuntimeException("Test exception");
        doThrow(expectedException).when(streamFileOperationsService)
            .streamFileOperations(any(), any(), anyLong());

        // When
        service.streamFileOperations(request, observer);

        // Then
        verify(processingMetrics).incrementActiveRequests();
        verify(processingMetrics).incrementFailedRequests();
        verify(processingMetrics).decrementActiveRequests();
        verify(processingMetrics).addRequestDuration(anyLong());

        ArgumentCaptor<StatusRuntimeException> errorCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(observer).onError(errorCaptor.capture());
        StatusRuntimeException capturedError = errorCaptor.getValue();
        assertEquals(Status.Code.INTERNAL, capturedError.getStatus().getCode());
        assertTrue(capturedError.getMessage().contains("Test exception"));
    }

    @Test
    void uploadFiles_DelegatesCorrectly() {
        // Given
        @SuppressWarnings("unchecked")
        StreamObserver<FileUploadRequest> expectedStreamObserver = mock(StreamObserver.class);
        when(uploadFilesService.uploadFiles(any(), any(), any(), any()))
            .thenReturn(expectedStreamObserver);

        // When
        StreamObserver<FileUploadRequest> result = service.uploadFiles(responseObserver);

        // Then
        verify(processingMetrics).incrementActiveRequests();
        verify(uploadFilesService).uploadFiles(
            eq(responseObserver),
            any(Runnable.class),
            any(Runnable.class),
            any(Runnable.class)
        );
        assertEquals(expectedStreamObserver, result);
    }

    @Test
    void uploadFiles_HandlesExceptions() {
        // Given
        RuntimeException expectedException = new RuntimeException("Test exception");
        when(uploadFilesService.uploadFiles(any(), any(), any(), any()))
            .thenThrow(expectedException);

        // When
        StreamObserver<FileUploadRequest> result = service.uploadFiles(responseObserver);

        // Then
        verify(processingMetrics).incrementActiveRequests();
        verify(processingMetrics).incrementFailedRequests();
        verify(processingMetrics).decrementActiveRequests();
        verify(processingMetrics).addRequestDuration(anyLong());

        ArgumentCaptor<StatusRuntimeException> errorCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(responseObserver).onError(errorCaptor.capture());
        StatusRuntimeException capturedError = errorCaptor.getValue();
        assertEquals(Status.Code.INTERNAL, capturedError.getStatus().getCode());
        assertTrue(capturedError.getMessage().contains("Test exception"));
        assertNotNull(result); // Returns a no-op observer
    }

    @Test
    void liveFileProcessing_DelegatesCorrectly() {
        // Given
        @SuppressWarnings("unchecked")
        StreamObserver<FileOperationResult> observer = mock(StreamObserver.class);
        StreamObserver<FileUploadRequest> expectedStreamObserver = mock(StreamObserver.class);
        when(liveFileProcessingService.liveFileProcessing(observer)).thenReturn(expectedStreamObserver);

        // When
        StreamObserver<FileUploadRequest> result = service.liveFileProcessing(observer);

        // Then
        verify(processingMetrics).incrementActiveRequests();
        verify(processingMetrics).decrementActiveRequests();
        verify(processingMetrics).addRequestDuration(anyLong());
        verify(liveFileProcessingService).liveFileProcessing(observer);
        assertEquals(expectedStreamObserver, result);
        verify(processingMetrics, never()).incrementFailedRequests();
    }

    @Test
    void liveFileProcessing_HandlesExceptions() {
        // Given
        @SuppressWarnings("unchecked")
        StreamObserver<FileOperationResult> observer = mock(StreamObserver.class);
        RuntimeException expectedException = new RuntimeException("Test exception");
        when(liveFileProcessingService.liveFileProcessing(observer)).thenThrow(expectedException);

        // When
        StreamObserver<FileUploadRequest> result = service.liveFileProcessing(observer);

        // Then
        verify(processingMetrics).incrementActiveRequests();
        verify(processingMetrics).incrementFailedRequests();
        verify(processingMetrics).decrementActiveRequests();
        verify(processingMetrics).addRequestDuration(anyLong());

        ArgumentCaptor<StatusRuntimeException> errorCaptor = ArgumentCaptor.forClass(StatusRuntimeException.class);
        verify(observer).onError(errorCaptor.capture());
        StatusRuntimeException capturedError = errorCaptor.getValue();
        assertEquals(Status.Code.INTERNAL, capturedError.getStatus().getCode());
        assertTrue(capturedError.getMessage().contains("Test exception"));
        assertNull(result);
    }
}
