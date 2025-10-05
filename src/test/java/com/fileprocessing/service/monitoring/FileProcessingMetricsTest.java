//package com.fileprocessing.service.monitoring;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class FileProcessingMetricsTest {
//
//    private FileProcessingMetrics metrics;
//
//    @BeforeEach
//    void setUp() {
//        metrics = new FileProcessingMetrics();
//    }
//
//    @Test
//    void testTaskMetricsInitialState() {
//        assertEquals(0, metrics.getActiveTasks());
//        assertEquals(0, metrics.getCompletedTasks());
//        assertEquals(0, metrics.getFailedTasks());
//        assertEquals(0, metrics.getAverageTaskDurationMillis());
//    }
//
//    @Test
//    void testRequestMetricsInitialState() {
//        assertEquals(0, metrics.getActiveRequests());
//        assertEquals(0, metrics.getCompletedRequests());
//        assertEquals(0, metrics.getFailedRequests());
//        assertEquals(0, metrics.getAverageRequestDurationMillis());
//    }
//
//    @Test
//    void testTaskOperations() {
//        metrics.incrementActiveTasks();
//        assertEquals(1, metrics.getActiveTasks());
//
//        metrics.incrementActiveTasks();
//        assertEquals(2, metrics.getActiveTasks());
//
//        metrics.decrementActiveTasks();
//        assertEquals(1, metrics.getActiveTasks());
//
//        metrics.addTaskDuration(100);
//        assertEquals(1, metrics.getCompletedTasks());
//        assertEquals(100, metrics.getAverageTaskDurationMillis());
//
//        metrics.addTaskDuration(300);
//        assertEquals(2, metrics.getCompletedTasks());
//        assertEquals(200, metrics.getAverageTaskDurationMillis()); // (100 + 300) / 2
//
//        metrics.incrementFailedTasks();
//        assertEquals(1, metrics.getFailedTasks());
//    }
//
//    @Test
//    void testRequestOperations() {
//        metrics.incrementActiveRequests();
//        assertEquals(1, metrics.getActiveRequests());
//
//        metrics.incrementActiveRequests();
//        assertEquals(2, metrics.getActiveRequests());
//
//        metrics.decrementActiveRequests();
//        assertEquals(1, metrics.getActiveRequests());
//
//        metrics.addRequestDuration(150);
//        assertEquals(1, metrics.getCompletedRequests());
//        assertEquals(150, metrics.getAverageRequestDurationMillis());
//
//        metrics.addRequestDuration(450);
//        assertEquals(2, metrics.getCompletedRequests());
//        assertEquals(300, metrics.getAverageRequestDurationMillis()); // (150 + 450) / 2
//
//        metrics.incrementFailedRequests();
//        assertEquals(1, metrics.getFailedRequests());
//    }
//
//    @Test
//    void testReset() {
//        // Set up some non-zero state
//        metrics.incrementActiveTasks();
//        metrics.addTaskDuration(100);
//        metrics.incrementFailedTasks();
//        metrics.incrementActiveRequests();
//        metrics.addRequestDuration(200);
//        metrics.incrementFailedRequests();
//
//        // Verify non-zero state
//        assertNotEquals(0, metrics.getActiveTasks());
//        assertNotEquals(0, metrics.getCompletedTasks());
//        assertNotEquals(0, metrics.getFailedTasks());
//        assertNotEquals(0, metrics.getActiveRequests());
//        assertNotEquals(0, metrics.getCompletedRequests());
//        assertNotEquals(0, metrics.getFailedRequests());
//
//        // Reset and verify all metrics are zero
//        metrics.reset();
//
//        assertEquals(0, metrics.getActiveTasks());
//        assertEquals(0, metrics.getCompletedTasks());
//        assertEquals(0, metrics.getFailedTasks());
//        assertEquals(0, metrics.getAverageTaskDurationMillis());
//        assertEquals(0, metrics.getActiveRequests());
//        assertEquals(0, metrics.getCompletedRequests());
//        assertEquals(0, metrics.getFailedRequests());
//        assertEquals(0, metrics.getAverageRequestDurationMillis());
//    }
//
//    @Test
//    void testAsMap() {
//        // Set up some known state
//        metrics.incrementActiveTasks();
//        metrics.addTaskDuration(100);
//        metrics.incrementFailedTasks();
//        metrics.incrementActiveRequests();
//        metrics.addRequestDuration(200);
//        metrics.incrementFailedRequests();
//
//        Map<String, Object> metricsMap = metrics.asMap();
//
//        assertEquals(1, metricsMap.get("activeTasks"));
//        assertEquals(1, metricsMap.get("completedTasks"));
//        assertEquals(1, metricsMap.get("failedTasks"));
//        assertEquals(100L, metricsMap.get("averageTaskDurationMillis"));
//        assertEquals(1, metricsMap.get("activeRequests"));
//        assertEquals(1, metricsMap.get("completedRequests"));
//        assertEquals(1, metricsMap.get("failedRequests"));
//        assertEquals(200L, metricsMap.get("averageRequestDurationMillis"));
//    }
//
//    @Test
//    void testToString() {
//        // Set up some known state
//        metrics.incrementActiveTasks();
//        metrics.addTaskDuration(100);
//
//        String toString = metrics.toString();
//        assertTrue(toString.startsWith("FileProcessingMetrics"));
//        assertTrue(toString.contains("activeTasks"));
//        assertTrue(toString.contains("completedTasks"));
//    }
//
//    @Test
//    void testAverageCalculationsWithZeroCompletions() {
//        // Should not throw division by zero
//        assertEquals(0, metrics.getAverageTaskDurationMillis());
//        assertEquals(0, metrics.getAverageRequestDurationMillis());
//    }
//
//    @Test
//    void testConcurrentIncrements() {
//        // Test concurrent increments
//        Runnable incrementTask = () -> {
//            for (int i = 0; i < 1000; i++) {
//                metrics.incrementActiveTasks();
//                metrics.decrementActiveTasks();
//                metrics.addTaskDuration(100);
//                metrics.incrementFailedTasks();
//            }
//        };
//
//        // Create multiple threads
//        Thread t1 = new Thread(incrementTask);
//        Thread t2 = new Thread(incrementTask);
//
//        // Start threads
//        t1.start();
//        t2.start();
//
//        // Wait for completion
//        try {
//            t1.join();
//            t2.join();
//        } catch (InterruptedException e) {
//            fail("Thread interrupted");
//        }
//
//        // Verify results
//        assertEquals(0, metrics.getActiveTasks());
//        assertEquals(2000, metrics.getCompletedTasks());
//        assertEquals(2000, metrics.getFailedTasks());
//        assertEquals(100, metrics.getAverageTaskDurationMillis());
//    }
//}
