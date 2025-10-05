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

    private int coreSize = Runtime.getRuntime().availableProcessors();
    private int maxSize = Runtime.getRuntime().availableProcessors() * 4;
    private int queueCapacity = 200;
    private int resizeThreshold = 50;
    private long keepAliveSeconds = 60;
    private long monitorIntervalSeconds = 10;

}
