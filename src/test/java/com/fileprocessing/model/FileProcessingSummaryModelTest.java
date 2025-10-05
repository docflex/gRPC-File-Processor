package com.fileprocessing.model;

import com.fileprocessing.FileSpec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileProcessingSummaryModelTest {

    // =======================
    // Constructor Validation
    // =======================

    @Test
    void constructor_ShouldThrowOnNegativeCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileProcessingSummaryModel(-1, 0, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new FileProcessingSummaryModel(0, -1, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new FileProcessingSummaryModel(0, 0, -1, null));
    }

    @Test
    void constructor_ShouldAllowValidCounts() {
        FileProcessingSummaryModel model = new FileProcessingSummaryModel(5, 3, 2, null);
        assertEquals(5, model.totalFiles());
        assertEquals(3, model.successfulFiles());
        assertEquals(2, model.failedFiles());
        assertNotNull(model.results());
        assertTrue(model.results().isEmpty());
    }

    @Test
    void constructor_ShouldMakeDefensiveCopyOfResults() {
        FileOperationResultModel result = FileOperationResultModel.builder()
                .fileId("f1")
                .operationType(com.fileprocessing.FileSpec.OperationType.VALIDATE)
                .status(com.fileprocessing.FileSpec.OperationStatus.SUCCESS)
                .build();

        List<FileOperationResultModel> resultsList = List.of(result);
        FileProcessingSummaryModel model = new FileProcessingSummaryModel(1, 1, 0, resultsList);

        // The internal list should be immutable
        assertThrows(UnsupportedOperationException.class, () -> model.results().add(result));
    }

    // =======================
    // Builder: basic usage
    // =======================

    @Test
    void builder_ShouldBuildWithExplicitValues() {
        FileOperationResultModel result = FileOperationResultModel.builder()
                .fileId("f1")
                .operationType(com.fileprocessing.FileSpec.OperationType.VALIDATE)
                .status(com.fileprocessing.FileSpec.OperationStatus.SUCCESS)
                .build();

        FileProcessingSummaryModel model = FileProcessingSummaryModel.builder()
                .totalFiles(2)
                .successfulFiles(1)
                .failedFiles(1)
                .addResult(result)
                .build();

        assertEquals(2, model.totalFiles());
        assertEquals(1, model.successfulFiles());
        assertEquals(1, model.failedFiles());
        assertEquals(1, model.results().size());
        assertEquals(result, model.results().get(0));
    }

    // =======================
    // Builder: increment helpers
    // =======================

    @Test
    void builder_incrementSuccess_ShouldUpdateCounts() {
        FileProcessingSummaryModel model = FileProcessingSummaryModel.builder()
                .incrementSuccess()
                .build();

        assertEquals(1, model.totalFiles());
        assertEquals(1, model.successfulFiles());
        assertEquals(0, model.failedFiles());
    }

    @Test
    void builder_incrementFailure_ShouldUpdateCounts() {
        FileProcessingSummaryModel model = FileProcessingSummaryModel.builder()
                .incrementFailure()
                .build();

        assertEquals(1, model.totalFiles());
        assertEquals(0, model.successfulFiles());
        assertEquals(1, model.failedFiles());
    }

    // =======================
    // Builder: null handling
    // =======================

    @Test
    void builder_ShouldIgnoreNullResults() {
        FileProcessingSummaryModel model = FileProcessingSummaryModel.builder()
                .results(null)
                .addResult(null)
                .build();

        assertNotNull(model.results());
        assertTrue(model.results().isEmpty());
    }

    // =======================
    // Immutability
    // =======================

    @Test
    void resultsList_ShouldBeImmutable() {
        FileOperationResultModel result = FileOperationResultModel.builder()
                .fileId("f1")
                .operationType(com.fileprocessing.FileSpec.OperationType.VALIDATE)
                .status(com.fileprocessing.FileSpec.OperationStatus.SUCCESS)
                .build();

        FileProcessingSummaryModel model = FileProcessingSummaryModel.builder()
                .addResult(result)
                .build();

        assertThrows(UnsupportedOperationException.class, () -> model.results().add(result));
    }

    // =======================
    // Edge cases
    // =======================

    @Test
    void builder_ShouldAllowZeroFiles() {
        FileProcessingSummaryModel model = FileProcessingSummaryModel.builder().build();
        assertEquals(0, model.totalFiles());
        assertEquals(0, model.successfulFiles());
        assertEquals(0, model.failedFiles());
        assertNotNull(model.results());
        assertTrue(model.results().isEmpty());
    }

    @Test
    void builder_ShouldHandleMultipleResults() {
        FileOperationResultModel r1 = FileOperationResultModel.builder()
                .fileId("f1")
                .operationType(com.fileprocessing.FileSpec.OperationType.VALIDATE)
                .status(com.fileprocessing.FileSpec.OperationStatus.SUCCESS)
                .build();
        FileOperationResultModel r2 = FileOperationResultModel.builder()
                .fileId("f2")
                .operationType(FileSpec.OperationType.IMAGE_RESIZE)
                .status(FileSpec.OperationStatus.FAILED)
                .build();

        FileProcessingSummaryModel model = FileProcessingSummaryModel.builder()
                .addResult(r1)
                .addResult(r2)
                .totalFiles(2)
                .successfulFiles(1)
                .failedFiles(1)
                .build();

        assertEquals(2, model.results().size());
        assertTrue(model.results().contains(r1));
        assertTrue(model.results().contains(r2));
    }
}
