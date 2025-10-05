package com.fileprocessing.model;

import com.fileprocessing.FileSpec.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadRequestModelTest {

    private FileModel file;

    @BeforeEach
    void setUp() {
        file = new FileModel("f1", "test.txt", new byte[]{1, 2, 3}, "txt", 3);
    }

    // =======================
    // Constructor validation
    // =======================

    @Test
    void constructor_ShouldThrowOnNullFile() {
        assertThrows(NullPointerException.class, () -> new FileUploadRequestModel(null, List.of(OperationType.VALIDATE)));
    }

    @Test
    void constructor_ShouldAllowNullOperations() {
        FileUploadRequestModel model = new FileUploadRequestModel(file, null);
        assertNotNull(model.operations());
        assertTrue(model.operations().isEmpty());
    }

    @Test
    void constructor_ShouldMakeDefensiveCopyOfOperations() {
        List<OperationType> ops = Arrays.asList(OperationType.VALIDATE, OperationType.IMAGE_RESIZE);
        FileUploadRequestModel model = new FileUploadRequestModel(file, ops);

        assertEquals(2, model.operations().size());
        // Internal list should be immutable
        assertThrows(UnsupportedOperationException.class, () -> model.operations().add(OperationType.IMAGE_RESIZE));
    }

    // =======================
    // Builder: basic usage
    // =======================

    @Test
    void builder_ShouldBuildWithFileAndOperations() {
        FileUploadRequestModel model = FileUploadRequestModel.builder()
                .file(file)
                .operations(List.of(OperationType.VALIDATE, OperationType.IMAGE_RESIZE))
                .build();

        assertEquals(file, model.file());
        assertEquals(2, model.operations().size());
        assertTrue(model.operations().contains(OperationType.VALIDATE));
        assertTrue(model.operations().contains(OperationType.IMAGE_RESIZE));
    }

    // =======================
    // Builder: addOperation
    // =======================

    @Test
    void builder_addOperation_ShouldAppendOperations() {
        FileUploadRequestModel model = FileUploadRequestModel.builder()
                .file(file)
                .addOperation(OperationType.VALIDATE)
                .addOperation(OperationType.IMAGE_RESIZE)
                .build();

        assertEquals(2, model.operations().size());
        assertEquals(OperationType.VALIDATE, model.operations().get(0));
        assertEquals(OperationType.IMAGE_RESIZE, model.operations().get(1));
    }

    @Test
    void builder_addOperation_ShouldIgnoreNulls() {
        FileUploadRequestModel model = FileUploadRequestModel.builder()
                .file(file)
                .addOperation(null)
                .build();

        assertNotNull(model.operations());
        assertTrue(model.operations().isEmpty());
    }

    @Test
    void builder_operations_ShouldIgnoreNullList() {
        FileUploadRequestModel model = FileUploadRequestModel.builder()
                .file(file)
                .operations(null)
                .build();

        assertNotNull(model.operations());
        assertTrue(model.operations().isEmpty());
    }

    // =======================
    // Builder: null file
    // =======================

    @Test
    void builder_ShouldThrowIfFileNotSet() {
        assertThrows(NullPointerException.class, () -> FileUploadRequestModel.builder()
                .addOperation(OperationType.VALIDATE)
                .build());
    }

    // =======================
    // Immutability
    // =======================

    @Test
    void operationsList_ShouldBeImmutable() {
        FileUploadRequestModel model = FileUploadRequestModel.builder()
                .file(file)
                .operations(Arrays.asList(OperationType.VALIDATE))
                .build();

        assertThrows(UnsupportedOperationException.class, () -> model.operations().add(OperationType.IMAGE_RESIZE));
    }

    // =======================
    // Edge cases
    // =======================

    @Test
    void constructor_ShouldAllowEmptyOperations() {
        FileUploadRequestModel model = new FileUploadRequestModel(file, Collections.emptyList());
        assertNotNull(model.operations());
        assertTrue(model.operations().isEmpty());
    }

    @Test
    void builder_ShouldAllowEmptyOperations() {
        FileUploadRequestModel model = FileUploadRequestModel.builder()
                .file(file)
                .operations(Collections.emptyList())
                .build();

        assertNotNull(model.operations());
        assertTrue(model.operations().isEmpty());
    }
}
