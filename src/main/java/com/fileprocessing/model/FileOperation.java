package com.fileprocessing.model;

import com.fileprocessing.FileSpec.OperationType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a single operation to perform on a file.
 * Immutable and safe for multithreaded use.
 */
public record FileOperation(OperationType operationType, Map<String, Object> parameters) {

    /**
     * Constructor with validation and defensive copy.
     *
     * @param operationType the type of operation (required)
     * @param parameters    optional parameters for the operation
     */
    public FileOperation(OperationType operationType, Map<String, Object> parameters) {
        this.operationType = Objects.requireNonNull(operationType, "operationType cannot be null");

        if (parameters != null) {
            // Create unmodifiable copy to prevent external mutation
            this.parameters = Map.copyOf(parameters);
        } else {
            this.parameters = Collections.emptyMap();
        }
    }

    /**
     * Builder pattern for safer construction.
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

    public static class FileOperationBuilder {
        private final Map<String, Object> parameters = new HashMap<>();
        private OperationType operationType;

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
