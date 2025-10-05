package com.fileprocessing.service.monitoring;

import com.fileprocessing.concurrency.ThreadPoolManager;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ThreadPoolMetrics {

    private final ThreadPoolManager threadPoolManager;
    private final MeterRegistry registry;

    @PostConstruct
    public void init() {
        Gauge.builder("fileprocessing.threadpool.active", threadPoolManager, ThreadPoolManager::getActiveCount)
                .description("Active threads in file processing pool")
                .register(registry);

        Gauge.builder("fileprocessing.threadpool.queue", threadPoolManager, ThreadPoolManager::getQueueSize)
                .description("Queue size in file processing pool")
                .register(registry);
    }
}