package com.fileprocessing.model.concurrency;

import com.fileprocessing.FileSpec;
import com.fileprocessing.model.FileOperationResultModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FileWorkflowTest {

    private FileTask task1;
    private FileTask task2;

    @BeforeEach
    void setUp() {
        task1 = new FileTask(
                new com.fileprocessing.model.FileModel("file1", "file1.txt", new byte[]{1}, "TEXT", 1L),
                new com.fileprocessing.model.FileOperation(com.fileprocessing.FileSpec.OperationType.VALIDATE, null)
        );

        task2 = new FileTask(
                new com.fileprocessing.model.FileModel("file2", "file2.txt", new byte[]{2}, "TEXT", 2L),
                new com.fileprocessing.model.FileOperation(FileSpec.OperationType.FILE_COMPRESSION, null)
        );
    }

    // =======================
    // Constructor & Factory
    // =======================

    @Test
    void constructor_ShouldAssignWorkflowId_WhenProvided() {
        FileWorkflow workflow = new FileWorkflow("my-id", List.of(task1, task2), Instant.now());
        assertEquals("my-id", workflow.workflowId());
    }

    @Test
    void constructor_ShouldGenerateWorkflowId_WhenNull() {
        FileWorkflow workflow = new FileWorkflow(null, List.of(task1), Instant.now());
        assertNotNull(workflow.workflowId());
        assertFalse(workflow.workflowId().isEmpty());
    }

    @Test
    void constructor_ShouldAssignSubmittedAt_WhenProvided() {
        Instant now = Instant.now();
        FileWorkflow workflow = new FileWorkflow("id", List.of(task1), now);
        assertEquals(now, workflow.submittedAt());
    }

    @Test
    void constructor_ShouldDefaultSubmittedAt_WhenNull() {
        FileWorkflow workflow = new FileWorkflow("id", List.of(task1), null);
        assertNotNull(workflow.submittedAt());
    }

    @Test
    void constructor_ShouldThrow_WhenTasksNull() {
        assertThrows(NullPointerException.class, () -> new FileWorkflow("id", null, Instant.now()));
    }

    @Test
    void constructor_ShouldThrow_WhenTasksEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new FileWorkflow("id", List.of(), Instant.now()));
    }

    @Test
    void factoryOf_ShouldCreateWorkflowWithGeneratedId() {
        FileWorkflow workflow = FileWorkflow.of(List.of(task1, task2));
        assertNotNull(workflow.workflowId());
        assertEquals(2, workflow.totalTasks());
        assertNotNull(workflow.submittedAt());
    }

    // =======================
    // Task Counts
    // =======================

    @Test
    void totalTasks_ShouldReturnCorrectCount() {
        FileWorkflow workflow = new FileWorkflow("id", List.of(task1, task2), Instant.now());
        assertEquals(2, workflow.totalTasks());
    }

    @Test
    void completedTasks_ShouldReturnZero_WhenNoneDone() {
        FileWorkflow workflow = new FileWorkflow("id", List.of(task1, task2), Instant.now());
        assertEquals(0, workflow.completedTasks());
    }

    @Test
    void completedTasks_ShouldCountDoneTasks() {
        task1.complete(new FileOperationResultModel("file1", com.fileprocessing.FileSpec.OperationType.VALIDATE, com.fileprocessing.FileSpec.OperationStatus.SUCCESS, null, null, null, null),
                new com.fileprocessing.service.monitoring.FileProcessingMetrics(mock(io.micrometer.core.instrument.MeterRegistry.class)), 10);

        FileWorkflow workflow = new FileWorkflow("id", List.of(task1, task2), Instant.now());
        assertEquals(1, workflow.completedTasks());
    }

    @Test
    void failedTasks_ShouldReturnZero_WhenNoneFailed() {
        FileWorkflow workflow = new FileWorkflow("id", List.of(task1, task2), Instant.now());
        assertEquals(0, workflow.failedTasks());
    }

    @Test
    void failedTasks_ShouldCountExceptionallyCompletedTasks() {
        task2.completeExceptionally(new RuntimeException("fail"),
                new com.fileprocessing.service.monitoring.FileProcessingMetrics(mock(io.micrometer.core.instrument.MeterRegistry.class)), 5);

        FileWorkflow workflow = new FileWorkflow("id", List.of(task1, task2), Instant.now());
        assertEquals(1, workflow.failedTasks());
    }

    // =======================
    // toString
    // =======================

    @Test
    void toString_ShouldContainIdAndCounts() {
        FileWorkflow workflow = new FileWorkflow("workflow123", List.of(task1, task2), Instant.now());
        String str = workflow.toString();
        assertTrue(str.contains("workflow123"));
        assertTrue(str.contains("tasks=2"));
        assertTrue(str.contains("completed=0"));
    }

    @Test
    void toString_ShouldReflectCompletedTasks() {
        task1.complete(new FileOperationResultModel("file1", com.fileprocessing.FileSpec.OperationType.VALIDATE, com.fileprocessing.FileSpec.OperationStatus.SUCCESS, null, null, null, null),
                new com.fileprocessing.service.monitoring.FileProcessingMetrics(mock(io.micrometer.core.instrument.MeterRegistry.class)), 10);

        FileWorkflow workflow = new FileWorkflow("workflow123", List.of(task1, task2), Instant.now());
        String str = workflow.toString();
        assertTrue(str.contains("completed=1"));
    }
}
