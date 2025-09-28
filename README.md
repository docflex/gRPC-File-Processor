
# File Processing Microservice

A scalable, concurrent, gRPC-based microservice for processing files with multiple operations such as validation, metadata extraction, OCR, compression, format conversion, image resizing, and storage.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Key Components](#key-components)
4. [gRPC Services & Methods](#grpc-services--methods)
5. [Usage](#usage)
6. [Metrics](#metrics)
7. [Limitations](#limitations)
8. [Future Scope](#future-scope)
9. [License](#license)

---

## Overview

This microservice is designed to:

* Handle large volumes of files concurrently.
* Allow multiple file operations in a **scalable, thread-safe** manner.
* Provide **unary, streaming, and bidirectional gRPC endpoints**.
* Enable runtime introspection using gRPC **Server Reflection**.

---

## Architecture

```
+--------------------------+
|  gRPC Client / grpcurl   |
+-----------+--------------+
            |
            v
+--------------------------+
|  FileProcessingService   |  (GrpcService)
|  - processFile           |
|  - streamFileOperations  |
|  - uploadFiles           |
|  - liveFileProcessing    |
+-----------+--------------+
            |
            v
+--------------------------+
| ProcessFileService       |  (Business logic)
| - Converts proto request |
|   -> Internal model      |
| - Executes operations   |
| - Updates metrics        |
+-----------+--------------+
            |
            v
+--------------------------+
| WorkflowExecutorService  |  (Concurrency)
| - Manages FileWorkflow   |
| - Submits FileTasks to   |
|   ThreadPoolManager      |
+-----------+--------------+
            |
            v
+--------------------------+
| ThreadPoolManager        |  (Thread Pool)
| - Adaptive resizing      |
| - Backpressure handling  |
| - Task metrics tracking  |
+--------------------------+
```

---

## Key Components

### 1. **Models**

| Model                        | Purpose                                                                |
| ---------------------------- | ---------------------------------------------------------------------- |
| `FileModel`                  | Immutable file representation (name, content, type, size)              |
| `FileOperation`              | Encapsulates a single operation on a file with optional parameters     |
| `FileOperationResultModel`   | Captures status, timestamps, and result location of an operation       |
| `FileProcessingRequestModel` | Internal request model supporting default and file-specific operations |
| `FileProcessingSummaryModel` | Summarizes results of a batch of file operations                       |
| `FileTask`                   | Encapsulates a file + operation + CompletableFuture result             |
| `FileWorkflow`               | Groups multiple `FileTask`s into a workflow with unique ID             |
| `FileProcessingMetrics`      | Tracks active tasks and average duration                               |

---

### 2. **Threading / Concurrency**

* **ThreadPoolManager**

    * Dynamic resizing based on queue size
    * Bounded queue for backpressure
    * Monitors active tasks and adjusts core/max threads
    * Uses `CallerRunsPolicy` for overload protection

* **WorkflowExecutorService**

    * Converts a workflow of tasks into concurrent submissions
    * Waits for all `CompletableFuture`s to complete
    * Aggregates results into a summary

---

### 3. **Operations**

Operations are now implemented in a utility class `FileOperationsUtil`:

* `validateFile(FileModel file)` – validates file size
* `extractMetadata(FileModel file)` – mock metadata extraction
* `compressFile(FileModel file)` – mock compression
* *(Other operations like OCR, resize, format conversion, store can be implemented similarly)*

---

## gRPC Services & Methods

| RPC Method             | Type                    | Description                                             |
| ---------------------- | ----------------------- | ------------------------------------------------------- |
| `ProcessFile`          | Unary                   | Process multiple files and return a summary.            |
| `StreamFileOperations` | Server Streaming        | Stream each `FileOperationResult` as it completes.      |
| `UploadFiles`          | Client Streaming        | Upload multiple files as a stream and return a summary. |
| `LiveFileProcessing`   | Bidirectional Streaming | Real-time streaming of file operations and results.     |

**Server Reflection** is enabled, allowing `grpcurl` to inspect services without `.proto` files.

---

## Usage

### 1. Run the server

```bash
mvn spring-boot:run
# or
java -jar target/fileprocessing-1.0-SNAPSHOT.jar
```

Server runs on `localhost:9090` (default).

### 2. List services via grpcurl

```bash
grpcurl -plaintext localhost:9090 list
# Output:
# com.fileprocessing.FileProcessingService
# grpc.health.v1.Health
# grpc.reflection.v1alpha.ServerReflection
```

### 3. Describe methods

```bash
grpcurl -plaintext localhost:9090 describe com.fileprocessing.FileProcessingService
```

### 4. Invoke `ProcessFile` (unary)

```bash
grpcurl -plaintext -d '{
  "files": [
    {
      "fileId": "1",
      "fileName": "sample.txt",
      "content": "SGVsbG8gd29ybGQ=",   // Base64 encoded content
      "fileType": "txt",
      "sizeBytes": 11
    }
  ],
  "operations": ["VALIDATE","METADATA_EXTRACTION"]
}' localhost:9090 com.fileprocessing.FileProcessingService/ProcessFile
```

### 5. Metrics

* Metrics tracked in `FileProcessingMetrics`
* Active tasks and average task duration are updated in real-time

---

## Limitations

* Operations like OCR, format conversion, image resizing are currently **mock implementations**.
* No persistence: file storage is only simulated.
* No authentication, authorization, or multi-tenant support.
* Single node only; clustering not yet implemented.
* Streaming endpoints are placeholders and need completion.

---

## Future Scope

* Implement actual OCR, compression, format conversion, resizing, and storage.
* Persist results and metadata to database or object storage (S3, MinIO).
* Add security (TLS, JWT, or mTLS) for gRPC endpoints.
* Distributed workflows using Kafka/RabbitMQ for massive file batches.
* Expose REST gateway for non-gRPC clients.
* Add advanced metrics and monitoring dashboards (Prometheus + Grafana).
* Support for hot-swappable operations and dynamic configuration.

---

## Diagrams

**Workflow Execution (Unary)**

```
Client ---> gRPC Server ---> ProcessFileService ---> WorkflowExecutorService ---> ThreadPoolManager
   |                                                          |
   |<------------------- FileProcessingSummary --------------|
```

**Thread Pool / Concurrency**

```
ThreadPoolManager
+---------------------+
| Queue (bounded)     |<-- Task submissions
| Active Threads      |
| Dynamic Resizing    |
+---------------------+
       |
       v
  FileTask.run() -> executes operation -> updates metrics
```

---

## Development Notes

* Use **Java 17**, **Spring Boot**, **gRPC**, and **Maven**.
* Proto files are under `src/main/proto/`.
* Generate gRPC stubs using `mvn protobuf:compile`.
* All models are immutable (`record`) for thread-safety.
* `ThreadPoolManager` dynamically scales cores and max threads based on queue load.

---

## License

MIT License — free to use and extend.

---
