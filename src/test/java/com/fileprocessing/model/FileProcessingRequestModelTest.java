package com.fileprocessing.model;

import com.fileprocessing.FileSpec.OperationType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FileProcessingRequestModelTest {

    private FileModel createFile(String id) {
        return new FileModel(id, id + ".txt", new byte[]{1, 2}, "txt", 2L);
    }

    // =======================
    // Constructor Validation
    // =======================

    @Test
    void constructor_ShouldThrowOnNullFiles() {
        assertThrows(IllegalArgumentException.class, () ->
                new FileProcessingRequestModel(null, null, null));
    }

    @Test
    void constructor_ShouldThrowOnEmptyFiles() {
        assertThrows(IllegalArgumentException.class, () ->
                new FileProcessingRequestModel(Collections.emptyList(), null, null));
    }

    @Test
    void constructor_ShouldMakeDefensiveCopies() {
        List<FileModel> files = new ArrayList<>();
        files.add(createFile("f1"));

        List<OperationType> defaultOps = new ArrayList<>();
        defaultOps.add(OperationType.VALIDATE);

        Map<String, List<OperationType>> specificOps = new HashMap<>();
        specificOps.put("f1", new ArrayList<>(List.of(OperationType.IMAGE_RESIZE)));

        FileProcessingRequestModel model = new FileProcessingRequestModel(files, defaultOps, specificOps);

        // Modifying original collections should not affect model
        files.add(createFile("f2"));
        defaultOps.add(OperationType.IMAGE_RESIZE);
        specificOps.put("f2", List.of(OperationType.VALIDATE));

        assertEquals(1, model.files().size());
        assertEquals(1, model.defaultOperations().size());
        assertEquals(1, model.fileSpecificOperations().size());
    }

    @Test
    void constructor_ShouldHandleNullDefaultOperationsAndFileSpecificOperations() {
        FileProcessingRequestModel model = new FileProcessingRequestModel(
                List.of(createFile("f1")), null, null
        );

        assertNotNull(model.defaultOperations());
        assertTrue(model.defaultOperations().isEmpty());
        assertNotNull(model.fileSpecificOperations());
        assertTrue(model.fileSpecificOperations().isEmpty());
    }

    // =======================
    // Immutability Checks
    // =======================

    @Test
    void files_ShouldBeImmutable() {
        List<FileModel> files = new ArrayList<>();
        files.add(createFile("f1"));

        FileProcessingRequestModel model = new FileProcessingRequestModel(files, null, null);

        assertThrows(UnsupportedOperationException.class, () -> model.files().add(createFile("f2")));
    }

    @Test
    void defaultOperations_ShouldBeImmutable() {
        FileProcessingRequestModel model = new FileProcessingRequestModel(
                List.of(createFile("f1")),
                List.of(OperationType.VALIDATE),
                null
        );

        assertThrows(UnsupportedOperationException.class, () -> model.defaultOperations().add(OperationType.IMAGE_RESIZE));
    }

    @Test
    void fileSpecificOperations_ShouldBeImmutable() {
        Map<String, List<OperationType>> specificOps = new HashMap<>();
        specificOps.put("f1", List.of(OperationType.VALIDATE));

        FileProcessingRequestModel model = new FileProcessingRequestModel(
                List.of(createFile("f1")),
                null,
                specificOps
        );

        assertThrows(UnsupportedOperationException.class,
                () -> model.fileSpecificOperations().put("f2", List.of(OperationType.IMAGE_RESIZE)));
        assertThrows(UnsupportedOperationException.class,
                () -> model.fileSpecificOperations().get("f1").add(OperationType.IMAGE_RESIZE));
    }

    // =======================
    // Builder
    // =======================

    @Test
    void builder_ShouldBuildCorrectly() {
        FileModel f1 = createFile("f1");
        FileModel f2 = createFile("f2");

        FileProcessingRequestModel model = FileProcessingRequestModel.builder()
                .addFile(f1)
                .addFile(f2)
                .addDefaultOperation(OperationType.VALIDATE)
                .addFileSpecificOperation("f2", List.of(OperationType.IMAGE_RESIZE))
                .build();

        assertEquals(2, model.files().size());
        assertEquals(1, model.defaultOperations().size());
        assertEquals(1, model.fileSpecificOperations().size());
        assertEquals(List.of(OperationType.IMAGE_RESIZE), model.fileSpecificOperations().get("f2"));
    }

    // =======================
    // Edge Cases
    // =======================

    @Test
    void builder_ShouldAllowEmptyDefaultOperationsAndFileSpecificOperations() {
        FileProcessingRequestModel model = FileProcessingRequestModel.builder()
                .addFile(createFile("f1"))
                .build();

        assertNotNull(model.defaultOperations());
        assertTrue(model.defaultOperations().isEmpty());
        assertNotNull(model.fileSpecificOperations());
        assertTrue(model.fileSpecificOperations().isEmpty());
    }
}
