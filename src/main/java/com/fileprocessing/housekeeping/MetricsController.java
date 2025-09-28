package com.fileprocessing.housekeeping;

import com.fileprocessing.model.concurrency.FileProcessingMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final FileProcessingMetrics metrics;

    @GetMapping("/unary")
    public Map<String, Object> getFileProcessingMetrics() {
        return Map.of(
                "activeTasks", metrics.getActiveTasks(),
                "averageTaskDurationMillis", metrics.getAverageTaskDurationMillis()
        );
    }
}
