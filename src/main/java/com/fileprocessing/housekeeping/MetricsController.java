package com.fileprocessing.housekeeping;

import com.fileprocessing.service.monitoring.FileProcessingMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller to expose file processing metrics for monitoring purposes.
 * Provides task-level, request-level, and derived metrics for dashboards.
 */
@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final FileProcessingMetrics metrics;

    /**
     * Expose current file processing metrics for unary requests.
     * Basic task-level metrics only.
     */
    @GetMapping("/unary")
    public Map<String, Object> getFileProcessingMetrics() {
        return Map.of(
                "activeTasks", metrics.getActiveTasks(),
                "averageTaskDurationMillis", metrics.getAverageTaskDurationMillis()
        );
    }

    /**
     * Expose a full summary of metrics for tasks and requests, including derived metrics like success rates.
     */
    @GetMapping("/summary")
    public Map<String, Object> getFileProcessingSummary() {
        Map<String, Object> summary = new HashMap<>(metrics.asMap());

        // Derived task metrics
        int completedTasks = metrics.getCompletedTasks();
        int failedTasks = metrics.getFailedTasks();
        int totalTasks = completedTasks + failedTasks;
        double taskSuccessRate = totalTasks == 0 ? 0 : (completedTasks * 100.0) / totalTasks;

        // Derived request metrics
        int completedRequests = metrics.getCompletedRequests();
        int failedRequests = metrics.getFailedRequests();
        int totalRequests = completedRequests + failedRequests;
        double requestSuccessRate = totalRequests == 0 ? 0 : (completedRequests * 100.0) / totalRequests;

        summary.put("totalTasks", totalTasks);
        summary.put("taskSuccessRatePercent", taskSuccessRate);
        summary.put("totalRequests", totalRequests);
        summary.put("requestSuccessRatePercent", requestSuccessRate);

        return summary;
    }

    /**
     * Reset all metrics to 0. Useful for testing or dashboard reset.
     */
    @GetMapping("/reset")
    public Map<String, Object> resetMetrics() {
        metrics.reset();
        return metrics.asMap();
    }
}
