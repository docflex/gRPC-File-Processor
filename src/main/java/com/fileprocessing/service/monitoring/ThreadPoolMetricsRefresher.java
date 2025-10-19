package com.fileprocessing.service.monitoring;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ThreadPoolMetricsRefresher {

    private final ThreadPoolMetrics metrics;

    public ThreadPoolMetricsRefresher(ThreadPoolMetrics metrics) {
        this.metrics = metrics;
    }

    @Scheduled(fixedRate = 100)
    public void refreshMetrics() {
        metrics.refresh();
    }
}
