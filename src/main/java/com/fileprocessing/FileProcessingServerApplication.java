package com.fileprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileProcessingServerApplication {

    // TODO: Make all of these configurable via application properties and hot-reloadable

    public static void main(String[] args) {
        SpringApplication.run(FileProcessingServerApplication.class, args);
    }

}
