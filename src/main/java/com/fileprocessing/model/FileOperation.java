package com.fileprocessing.model;

import com.fileprocessing.FileSpec.OperationType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single operation to perform on a file.
 * Immutable, validated, and thread-safe.
 */
public record FileOperation(OperationType operationType, Map<String, Object> parameters) {

    /**
     * Constructor with validation and defensive copy.
     */
    public FileOperation {
        Objects.requireNonNull(operationType, "operationType cannot be null");
        parameters = (parameters != null) ? Map.copyOf(parameters) : Collections.emptyMap();
    }

    /**
     * Builder for easier construction.
     */
    public static FileOperationBuilder builder() {
        return new FileOperationBuilder();
    }

    /**
     * Returns the parameter value by key or null if absent.
     */
    public Object getParameter(String key) {
        return parameters.get(key);
    }

    /**
     * Checks if this operation has any parameters.
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }

    public static class FileOperationBuilder {
        private OperationType operationType;
        private final Map<String, Object> parameters = new HashMap<>();

        public FileOperationBuilder operationType(OperationType operationType) {
            this.operationType = operationType;
            return this;
        }

        public FileOperationBuilder addParameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }

        public FileOperationBuilder parameters(Map<String, Object> params) {
            if (params != null) {
                this.parameters.putAll(params);
            }
            return this;
        }

        public FileOperation build() {
            return new FileOperation(operationType, parameters);
        }
    }
}
