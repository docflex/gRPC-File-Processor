package com.fileprocessing.model;

import com.fileprocessing.FileSpec.OperationType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileOperationTest {

    // =======================
    // Constructor / Validation
    // =======================

    @Test
    void constructor_ShouldInitializeFieldsCorrectly() {
        Map<String, Object> params = Map.of("key1", "value1", "key2", 42);
        FileOperation op = new FileOperation(OperationType.VALIDATE, params);

        assertEquals(OperationType.VALIDATE, op.operationType());
        assertEquals(2, op.parameters().size());
        assertEquals("value1", op.getParameter("key1"));
        assertEquals(42, op.getParameter("key2"));
    }

    @Test
    void constructor_ShouldDefaultParametersToEmptyMap_WhenNull() {
        FileOperation op = new FileOperation(OperationType.VALIDATE, null);
        assertNotNull(op.parameters());
        assertTrue(op.parameters().isEmpty());
    }

    @Test
    void constructor_ShouldThrowOnNullOperationType() {
        assertThrows(NullPointerException.class, () -> new FileOperation(null, Map.of()));
    }

    // =======================
    // Parameter Access
    // =======================

    @Test
    void getParameter_ShouldReturnValueOrNull() {
        Map<String, Object> params = Map.of("foo", 123);
        FileOperation op = new FileOperation(OperationType.IMAGE_RESIZE, params);

        assertEquals(123, op.getParameter("foo"));
        assertNull(op.getParameter("missingKey"));
    }

    @Test
    void hasParameters_ShouldReturnTrueWhenPresent() {
        FileOperation op = new FileOperation(OperationType.VALIDATE, Map.of("a", 1));
        assertTrue(op.hasParameters());
    }

    @Test
    void hasParameters_ShouldReturnFalseWhenEmpty() {
        FileOperation op = new FileOperation(OperationType.VALIDATE, Map.of());
        assertFalse(op.hasParameters());

        FileOperation op2 = new FileOperation(OperationType.VALIDATE, null);
        assertFalse(op2.hasParameters());
    }

    // =======================
    // Immutability / Defensive Copy
    // =======================

    @Test
    void parameters_ShouldBeImmutable() {
        Map<String, Object> params = new HashMap<>();
        params.put("key", "value");

        FileOperation op = new FileOperation(OperationType.IMAGE_RESIZE, params);
        // Original map modification should not affect FileOperation
        params.put("key2", "newValue");
        assertEquals(1, op.parameters().size());
        assertFalse(op.parameters().containsKey("key2"));

        // Trying to modify internal map should throw
        assertThrows(UnsupportedOperationException.class, () -> op.parameters().put("x", 1));
    }

    // =======================
    // Builder
    // =======================

    @Test
    void builder_ShouldBuildCorrectObject() {
        FileOperation op = FileOperation.builder()
                .operationType(OperationType.VALIDATE)
                .addParameter("p1", "v1")
                .addParameter("p2", 42)
                .build();

        assertEquals(OperationType.VALIDATE, op.operationType());
        assertEquals(2, op.parameters().size());
        assertEquals("v1", op.getParameter("p1"));
        assertEquals(42, op.getParameter("p2"));
    }

    @Test
    void builder_ShouldAllowAddingMultipleParametersMap() {
        Map<String, Object> map = Map.of("x", 1, "y", 2);
        FileOperation op = FileOperation.builder()
                .operationType(OperationType.IMAGE_RESIZE)
                .parameters(map)
                .build();

        assertEquals(OperationType.IMAGE_RESIZE, op.operationType());
        assertEquals(2, op.parameters().size());
        assertEquals(1, op.getParameter("x"));
        assertEquals(2, op.getParameter("y"));
    }

    @Test
    void builder_ShouldOverwriteParametersWithLaterCalls() {
        FileOperation op = FileOperation.builder()
                .operationType(OperationType.VALIDATE)
                .addParameter("a", 1)
                .parameters(Map.of("b", 2))
                .addParameter("c", 3)
                .build();

        assertEquals(3, op.parameters().size());
        assertEquals(1, op.getParameter("a"));
        assertEquals(2, op.getParameter("b"));
        assertEquals(3, op.getParameter("c"));
    }

    @Test
    void builder_ShouldThrowOnMissingOperationType() {
        FileOperation.FileOperationBuilder builder = FileOperation.builder()
                .addParameter("key", "value");
        assertThrows(NullPointerException.class, builder::build);
    }

    // =======================
    // toString / Record behavior
    // =======================

    @Test
    void recordFields_ShouldBeImmutable() {
        FileOperation op = new FileOperation(OperationType.VALIDATE, Map.of("x", 1));
        assertEquals(OperationType.VALIDATE, op.operationType());
        assertEquals(1, op.parameters().size());
        assertEquals(1, op.getParameter("x"));
    }
}
