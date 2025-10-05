package com.fileprocessing.model;

import com.fileprocessing.FileSpec.OperationStatus;
import com.fileprocessing.FileSpec.OperationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FileOperationResultModelTest {

    // =======================
    // Constructor / Validation
    // =======================

    @Test
    void constructor_ShouldInitializeFieldsCorrectly() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(2);

        FileOperationResultModel result = new FileOperationResultModel(
                "file1", OperationType.VALIDATE, OperationStatus.SUCCESS,
                "details", start, end, "/tmp/file1"
        );

        assertEquals("file1", result.fileId());
        assertEquals(OperationType.VALIDATE, result.operationType());
        assertEquals(OperationStatus.SUCCESS, result.status());
        assertEquals("details", result.details());
        assertEquals(start, result.startTime());
        assertEquals(end, result.endTime());
        assertEquals("/tmp/file1", result.resultLocation());
    }

    @Test
    void constructor_ShouldDefaultOptionalFields_WhenNull() {
        FileOperationResultModel result = new FileOperationResultModel(
                "file1", OperationType.VALIDATE, OperationStatus.FAILED,
                null, null, null, null
        );

        assertEquals("", result.details());
        assertNotNull(result.startTime());
        assertNotNull(result.endTime());
        assertEquals("", result.resultLocation());
    }

    @Test
    void constructor_ShouldThrowOnNullFileId() {
        assertThrows(NullPointerException.class,
                () -> new FileOperationResultModel(null, OperationType.VALIDATE, OperationStatus.SUCCESS, "", null, null, ""));
    }

    @Test
    void constructor_ShouldThrowOnNullOperationType() {
        assertThrows(NullPointerException.class,
                () -> new FileOperationResultModel("file1", null, OperationStatus.SUCCESS, "", null, null, ""));
    }

    @Test
    void constructor_ShouldThrowOnNullStatus() {
        assertThrows(NullPointerException.class,
                () -> new FileOperationResultModel("file1", OperationType.VALIDATE, null, "", null, null, ""));
    }

    // =======================
    // getDurationMillis
    // =======================

    @Test
    void getDurationMillis_ShouldReturnCorrectDuration() {
        Instant start = Instant.now();
        Instant end = start.plusMillis(500);

        FileOperationResultModel result = new FileOperationResultModel(
                "file1", OperationType.IMAGE_RESIZE, OperationStatus.SUCCESS, "", start, end, ""
        );

        assertEquals(500, result.getDurationMillis());
    }

    @Test
    void getDurationMillis_ShouldReturnZero_WhenEndBeforeStart() {
        Instant start = Instant.now();
        Instant end = start.minusMillis(100);

        FileOperationResultModel result = new FileOperationResultModel(
                "file1", OperationType.IMAGE_RESIZE, OperationStatus.SUCCESS, "", start, end, ""
        );

        assertEquals(0, result.getDurationMillis());
    }

    // =======================
    // Builder
    // =======================

    @Test
    void builder_ShouldBuildObjectCorrectly() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(1);

        FileOperationResultModel result = FileOperationResultModel.builder()
                .fileId("file1")
                .operationType(OperationType.VALIDATE)
                .status(OperationStatus.SUCCESS)
                .details("ok")
                .startTime(start)
                .endTime(end)
                .resultLocation("/tmp/file1")
                .build();

        assertEquals("file1", result.fileId());
        assertEquals(OperationType.VALIDATE, result.operationType());
        assertEquals(OperationStatus.SUCCESS, result.status());
        assertEquals("ok", result.details());
        assertEquals(start, result.startTime());
        assertEquals(end, result.endTime());
        assertEquals("/tmp/file1", result.resultLocation());
    }

    @Test
    void builder_ShouldDefaultOptionalFields_WhenNotSet() {
        FileOperationResultModel result = FileOperationResultModel.builder()
                .fileId("file1")
                .operationType(OperationType.VALIDATE)
                .status(OperationStatus.FAILED)
                .build();

        assertEquals("", result.details());
        assertNotNull(result.startTime());
        assertNotNull(result.endTime());
        assertEquals("", result.resultLocation());
    }

    @Test
    void builder_ShouldThrow_WhenRequiredFieldsMissing() {
        FileOperationResultModel.FileOperationResultModelBuilder builder = FileOperationResultModel.builder()
                .details("something");

        assertThrows(NullPointerException.class, builder::build);
    }

    // =======================
    // toString / Record behavior
    // =======================

    @Test
    void recordFields_ShouldBeImmutable() {
        FileOperationResultModel result = FileOperationResultModel.builder()
                .fileId("file1")
                .operationType(OperationType.VALIDATE)
                .status(OperationStatus.SUCCESS)
                .build();

        assertEquals("file1", result.fileId());
        assertEquals(OperationType.VALIDATE, result.operationType());
        assertEquals(OperationStatus.SUCCESS, result.status());
    }
}
