package client;

import com.fileprocessing.FileProcessingServiceGrpc;
import com.fileprocessing.FileSpec.FileUploadRequest;
import com.fileprocessing.FileSpec.File;
import com.fileprocessing.FileSpec.FileOperationResult;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LiveFileProcessingClient {

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        FileProcessingServiceGrpc.FileProcessingServiceStub stub =
                FileProcessingServiceGrpc.newStub(channel);

        CountDownLatch latch = new CountDownLatch(1);

        // StreamObserver to receive results from the server
        StreamObserver<FileOperationResult> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(FileOperationResult result) {
                System.out.println("┌───────────── Result Received ─────────────┐");
                System.out.printf("File ID   : %s%n", result.getFileId());
                System.out.printf("Operation : %s%n", result.getOperation());
                System.out.printf("Status    : %s%n", result.getStatus());
                System.out.printf("Details   : %s%n", result.getDetails());
                System.out.println("└───────────────────────────────────────────┘\n");
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Streaming error: " + t.getMessage());
                t.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("All results processed!");
                latch.countDown();
            }
        };

        // StreamObserver to send files to the server
        StreamObserver<FileUploadRequest> requestObserver = stub.liveFileProcessing(responseObserver);

        // Send multiple files with unique IDs and a delay to visualize streaming
        try {
            int totalFiles = 10000;
            for (int i = 1; i <= totalFiles; i++) {
                File file = File.newBuilder()
                        .setFileId(String.format("file-%03d", i))
                        .setFileName("example" + i + ".txt")
                        .setContent(ByteString.copyFromUtf8("Hello World " + i))
                        .setFileType("txt")
                        .setSizeBytes(11)
                        .build();

                FileUploadRequest request = FileUploadRequest.newBuilder()
                        .setFile(file)
                        .addOperations(com.fileprocessing.FileSpec.OperationType.VALIDATE)
                        .addOperations(com.fileprocessing.FileSpec.OperationType.METADATA_EXTRACTION)
                        .build();

                System.out.println(">>> Sending file: " + file.getFileId());
                requestObserver.onNext(request);

                // Delay to visualize streaming
                Thread.sleep(250);
            }
        } catch (Exception e) {
            requestObserver.onError(e);
            e.printStackTrace();
        }

        // Mark end of sending
        requestObserver.onCompleted();

        // Wait for all results
        latch.await(30, TimeUnit.SECONDS);

        channel.shutdown();
    }
}
