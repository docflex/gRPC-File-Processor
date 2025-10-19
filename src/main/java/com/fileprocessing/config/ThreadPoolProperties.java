package com.fileprocessing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "fileprocessing.threadpool")
public class ThreadPoolProperties {

    private int coreSize;
    private int maxSize;
    private int queueCapacity;
    private int resizeThreshold;
    private long keepAliveSeconds;
    private long monitorIntervalSeconds;

}
