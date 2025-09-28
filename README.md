
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

Here’s a full update section for your `README.md` with **unary RPC usage** examples using `grpcurl`. You can place it under a **“Usage”** or **“Testing”** section.

---

## Unary RPC Usage Example

The unary RPC `ProcessFile` allows sending a batch of files in a single request and receiving a summarized result.

### Prerequisites

* Make sure the gRPC server is running (default port: `9090`).
* `grpcurl` installed. You can install it via:

```bash
brew install grpcurl   # macOS
sudo apt install grpcurl # Linux (Debian/Ubuntu)
```

### Example Proto Request

Assume the proto message:

```proto
message FileProcessingRequest {
  repeated File files = 1;
  repeated OperationType operations = 2;
}
```

Example JSON payload for `grpcurl`:

```json
{
  "files": [
    {
      "fileId": "file-001",
      "fileName": "example.pdf",
      "content": "VGhpcyBpcyBhIHRlc3QgZmlsZSBjb250ZW50Lg==",
      "fileType": "pdf",
      "sizeBytes": 1024
    }
  ],
  "operations": ["VALIDATE", "METADATA_EXTRACTION"]
}
```

### grpcurl Command

```bash
grpcurl -plaintext \
  -d '{
        "files": [
          {
            "fileId": "file-001",
            "fileName": "example.pdf",
            "content": "VGhpcyBpcyBhIHRlc3QgZmlsZSBjb250ZW50Lg==",
            "fileType": "pdf",
            "sizeBytes": 1024
          }
        ],
        "operations": ["VALIDATE","METADATA_EXTRACTION"]
      }' \
  localhost:9090 com.fileprocessing.FileProcessingService/ProcessFile
```

### Sample Response

```json
{
  "totalFiles": 1,
  "successfulFiles": 2,
  "failedFiles": 0,
  "results": [
    {
      "fileId": "file-001",
      "operation": "VALIDATE",
      "status": "SUCCESS",
      "details": "File validated successfully",
      "startTime": "2025-09-28T20:00:00.000Z",
      "endTime": "2025-09-28T20:00:00.150Z",
      "resultLocation": ""
    },
    {
      "fileId": "file-001",
      "operation": "METADATA_EXTRACTION",
      "status": "SUCCESS",
      "details": "Metadata extracted",
      "startTime": "2025-09-28T20:00:00.150Z",
      "endTime": "2025-09-28T20:00:00.250Z",
      "resultLocation": ""
    }
  ]
}
```

### Notes

* All file contents must be Base64 encoded when sending JSON requests.
* The unary RPC returns **once all requested operations are completed**.
* Errors in processing will return an `INTERNAL` gRPC status with a descriptive message.

---

## License

MIT License — free to use and extend.

---

# To Do List

## **1. gRPC Services**

* [ ] Implement `StreamFileOperations` (server streaming)
* [ ] Implement `UploadFiles` (client streaming)
* [ ] Implement `LiveFileProcessing` (bidirectional streaming)
* [ ] Add request validation for all RPCs (check file size, type, operation support)
* [ ] Support gRPC error handling with meaningful status codes

---

## **2. File Operations / Business Logic**

* [ ] Implement actual **OCR** functionality for PDF and images
* [ ] Implement **image resizing** logic for JPG, PNG, GIF
* [ ] Implement **compression** logic (e.g., zip or image compression)
* [ ] Implement **format conversion** (e.g., PNG → JPG, PDF → TXT)
* [ ] Implement **file storage** logic (local FS or cloud like S3/MinIO)
* [ ] Add **metadata extraction** logic (size, type, creation date, EXIF for images)
* [ ] Add **validation rules** for file type, allowed operations, etc.

---

## **3. Concurrency / Workflow**

* [ ] Finalize `WorkflowExecutorService` integration for all gRPC endpoints
* [ ] Add error handling for individual `FileTask`s without failing the whole workflow
* [ ] Add **timeout support** for long-running tasks
* [ ] Improve **backpressure** handling (queue thresholds, client notifications)

---

## **4. Metrics / Monitoring**

* [x] Add per-operation metrics (e.g., processing duration per operation type)
* [ ] Expose metrics via **Prometheus** or **Spring Actuator** (note: actuator endpoint conflict with gRPC)
* [ ] Track **success/failure rates** per workflow

---

## **5. Spring / Configuration**

* [ ] Make all service properties configurable via `application.properties` (port, thread pool sizes, thresholds)
* [ ] Enable **hot reload** for config changes without restarting server
* [ ] Ensure **beans** are properly registered (`ProcessFileService`, `StreamFileOperationsService`, etc.)

---

## **6. Testing**

* [x] Unit tests for all **FileOperations**
* [ ] Integration tests for **WorkflowExecutorService**
* [ ] gRPC end-to-end tests using `grpc-java` or `grpcurl`
* [ ] Load testing for concurrent workflows
* [ ] Failure scenario testing (invalid file, large file, network failure)

---

## **7. Documentation**

* [ ] Convert ASCII diagrams to **PlantUML/Mermaid visuals** for README
* [ ] Document gRPC request/response examples
* [ ] Document supported file types and operations
* [ ] Add **contributing guide** if open-source

---

## **8. Future Enhancements / Optional**

* [ ] Add **authentication/authorization** for gRPC endpoints (JWT/mTLS)
* [ ] Add **persistent storage** for workflow state
* [ ] Add **distributed workflow support** (Kafka, RabbitMQ, or other queue)
* [ ] Expose **REST gateway** via `grpc-spring-boot-starter` or `Envoy proxy`
* [ ] Add **real-time notifications** for live file processing (WebSockets, gRPC streaming)

---

# Milestones

## **[DELIVERED] Phase 1 — Core Unary File Processing (MVP)**

**Goal:** Get the basic unary RPC (`ProcessFile`) fully functional with core operations.

**Tasks:**

1. Implement all placeholder operations in `ProcessFileService`:

  * Validation
  * Metadata extraction
  * Compression
  * Format conversion
  * File storage
2. Ensure `ProcessFileService` is properly registered as a Spring Bean.
3. Complete the unary gRPC endpoint (`processFile`) in `FileProcessingServiceImpl`.
4. Add basic unit tests for operations.
5. Expose basic metrics (`FileProcessingMetrics`) for the unary flow.
6. Update README with unary RPC usage examples (`grpcurl` commands).

**Milestone Deliverable:**
✅ Fully functional unary RPC processing files end-to-end with logs, metrics, and error handling.

---

## **Phase 2 — Concurrency & Workflow Management**

**Goal:** Introduce `WorkflowExecutorService` for orchestrating tasks with thread pool management.

**Tasks:**

1. Integrate `WorkflowExecutorService` with `ProcessFileService` for concurrent execution of tasks.
2. Add `ThreadPoolManager` adaptive thread pool management.
3. Track per-task metrics in `FileProcessingMetrics`.
4. Implement error isolation so a failed task does not fail the entire workflow.
5. Unit tests and integration tests for concurrency handling.

**Milestone Deliverable:**
✅ Concurrent file processing with metrics, adaptive threads, and task-level isolation.

---

## **Phase 3 — Streaming gRPC Endpoints**

**Goal:** Implement non-unary RPCs for batch and real-time processing.

**Tasks:**

1. Implement `StreamFileOperations` (server streaming).
2. Implement `UploadFiles` (client streaming).
3. Implement `LiveFileProcessing` (bidirectional streaming).
4. Add proper backpressure handling for streaming (queue limits, slow consumers).
5. Add tests for streaming scenarios.

**Milestone Deliverable:**
✅ Streaming endpoints functional with concurrency, backpressure, and error handling.

---

## **Phase 4 — Storage, Format, and OCR Enhancements**

**Goal:** Implement full file-processing logic beyond placeholders.

**Tasks:**

1. Implement OCR for PDFs and images.
2. Implement image resizing and format conversion.
3. Implement compression (zip, image optimization).
4. Implement file storage (local, S3, MinIO, or pluggable storage).
5. Metadata extraction (EXIF, file info, text content).

**Milestone Deliverable:**
✅ Fully-featured file-processing pipeline for supported file types.

---

## **Phase 5 — Monitoring, Metrics, and Observability**

**Goal:** Make the service observable and production-ready.

**Tasks:**

1. Expose metrics via Spring Actuator or Prometheus.
2. Track workflow success/failure rates, average task duration.
3. Add logging with SLF4J/Logback and structured logging.
4. Optional: gRPC reflection for debugging.

**Milestone Deliverable:**
✅ Production-grade observability with metrics and logs.

---

## **Phase 6 — Configuration, Security, and Scaling**

**Goal:** Make the service configurable, secure, and horizontally scalable.

**Tasks:**

1. Make thread pool sizes, queue thresholds, and ports configurable via `application.properties` (hot-reloadable).
2. Add authentication/authorization for gRPC (JWT/mTLS).
3. Add persistent workflow storage (optional: Redis, DB, or queue for distributed processing).
4. Add load testing and performance tuning.

**Milestone Deliverable:**
✅ Configurable, secure, and horizontally scalable file processing service.

---

## **Phase 7 — Optional Enhancements**

**Tasks:**

1. Add REST gateway for external clients.
2. Add real-time notifications (WebSocket or gRPC streaming).
3. Add distributed workflow orchestration (Kafka, RabbitMQ, etc.).
4. Add more file types or operation plugins (extensible architecture).

**Milestone Deliverable:**
✅ Advanced production-ready features, extensible for future growth.

---
