package com.fileprocessing.model.concurrency;

import com.fileprocessing.FileSpec.OperationStatus;
import com.fileprocessing.FileSpec.OperationType;
import com.fileprocessing.model.FileModel;
import com.fileprocessing.model.FileOperation;
import com.fileprocessing.model.FileOperationResultModel;
import com.fileprocessing.service.monitoring.FileProcessingMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileTaskTest {

    private FileModel file;
    private FileOperation operation;
    private FileProcessingMetrics metrics;

    @BeforeEach
    void setUp() {
        file = new FileModel("file123", "example.txt", new byte[]{1, 2, 3}, "TEXT", 3L);
        operation = new FileOperation(OperationType.VALIDATE, null);
        metrics = mock(FileProcessingMetrics.class);
    }

    // =======================
    // Construction & Validation
    // =======================

    @Test
    void constructor_ShouldInitializeFields() {
        FileTask task = new FileTask(file, operation);

        assertEquals(file, task.file());
        assertEquals(operation, task.operation());
        assertNotNull(task.futureResult());
        assertFalse(task.isDone());
    }

    @Test
    void constructor_ShouldThrowOnNullFile() {
        assertThrows(NullPointerException.class, () -> new FileTask(null, operation));
    }

    @Test
    void constructor_ShouldThrowOnNullOperation() {
        assertThrows(NullPointerException.class, () -> new FileTask(file, null));
    }

    // =======================
    // Successful Completion
    // =======================

    @Test
    void complete_ShouldCompleteFutureAndRecordMetrics() throws Exception {
        FileTask task = new FileTask(file, operation);
        FileOperationResultModel result = new FileOperationResultModel(
                file.fileId(), OperationType.VALIDATE, OperationStatus.SUCCESS, null, null, null, null
        );

        task.complete(result, metrics, 50);

        assertTrue(task.isDone());
        assertTrue(task.futureResult().isDone());
        assertEquals(result, task.futureResult().get());

        verify(metrics).recordTaskCompletion(50);
    }

    @Test
    void complete_ShouldNotAllowNullResult() {
        FileTask task = new FileTask(file, operation);
        assertThrows(NullPointerException.class, () -> task.complete(null, metrics, 10));
    }

    // =======================
    // Exceptional Completion
    // =======================

    @Test
    void completeExceptionally_ShouldCompleteFutureExceptionallyAndUpdateMetrics() {
        FileTask task = new FileTask(file, operation);
        RuntimeException ex = new RuntimeException("fail");

        task.completeExceptionally(ex, metrics, 100);

        assertTrue(task.isDone());
        assertTrue(task.futureResult().isCompletedExceptionally());

        verify(metrics).incrementFailedTasks();
        verify(metrics).recordTaskCompletion(100);

        ExecutionException thrown = assertThrows(ExecutionException.class, () -> task.futureResult().get());
        assertEquals(ex, thrown.getCause());
    }

    @Test
    void completeExceptionally_ShouldNotAllowNullThrowable() {
        FileTask task = new FileTask(file, operation);
        assertThrows(NullPointerException.class, () -> task.completeExceptionally(null, metrics, 10));
    }

    // =======================
    // Idempotency & Concurrency
    // =======================

    @Test
    void complete_ShouldOnlyRunOnce_WhenCalledMultipleTimes() throws Exception {
        FileTask task = new FileTask(file, operation);
        FileOperationResultModel r1 = new FileOperationResultModel(file.fileId(), OperationType.VALIDATE, OperationStatus.SUCCESS, null, null, null, null);
        FileOperationResultModel r2 = new FileOperationResultModel(file.fileId(), OperationType.VALIDATE, OperationStatus.SUCCESS, null, null, null, null);

        task.complete(r1, metrics, 10);
        task.complete(r2, metrics, 20); // Should be ignored

        assertEquals(r1, task.futureResult().get());
        verify(metrics, times(1)).recordTaskCompletion(10);
    }

    @Test
    void completeExceptionally_ShouldOnlyRunOnce_WhenCalledMultipleTimes() {
        FileTask task = new FileTask(file, operation);
        RuntimeException ex1 = new RuntimeException("ex1");
        RuntimeException ex2 = new RuntimeException("ex2");

        task.completeExceptionally(ex1, metrics, 5);
        task.completeExceptionally(ex2, metrics, 10);

        assertTrue(task.futureResult().isCompletedExceptionally());
        assertTrue(task.isDone());

        verify(metrics, times(1)).incrementFailedTasks();
        verify(metrics, times(1)).recordTaskCompletion(5);
    }

    @Test
    void completeAndCompleteExceptionally_Concurrency_ShouldHandleRaceCondition() throws Exception {
        FileTask task = new FileTask(file, operation);
        FileOperationResultModel result = new FileOperationResultModel(file.fileId(), OperationType.VALIDATE, OperationStatus.SUCCESS, null, null, null, null);
        RuntimeException ex = new RuntimeException("failure");

        // Simulate race: both complete and completeExceptionally called
        Thread t1 = new Thread(() -> task.complete(result, metrics, 30));
        Thread t2 = new Thread(() -> task.completeExceptionally(ex, metrics, 40));
        t1.start(); t2.start();
        t1.join(); t2.join();

        assertTrue(task.isDone());
        // Only one metric should be recorded (either success or failure)
        verify(metrics, atMost(1)).recordTaskCompletion(anyLong());
    }

    // =======================
    // Future retrieval
    // =======================

    @Test
    void futureResult_ShouldReturnSameCompletableFuture() {
        FileTask task = new FileTask(file, operation);
        CompletableFuture<FileOperationResultModel> future1 = task.futureResult();
        CompletableFuture<FileOperationResultModel> future2 = task.futureResult();
        assertSame(future1, future2);
    }

    // =======================
    // toString
    // =======================

    @Test
    void toString_ShouldIncludeFileIdOperationAndDoneStatus() {
        FileTask task = new FileTask(file, operation);
        String str = task.toString();

        assertTrue(str.contains(file.fileId()));
        assertTrue(str.contains(OperationType.VALIDATE.toString()));
        assertTrue(str.contains("done=false"));
    }

    @Test
    void toString_ShouldShowDoneTrueAfterCompletion() {
        FileTask task = new FileTask(file, operation);
        FileOperationResultModel result = new FileOperationResultModel(file.fileId(), OperationType.VALIDATE, OperationStatus.SUCCESS, null, null, null, null);
        task.complete(result, metrics, 10);

        assertTrue(task.toString().contains("done=true"));
    }
}
