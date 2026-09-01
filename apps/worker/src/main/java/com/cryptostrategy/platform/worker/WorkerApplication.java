package com.cryptostrategy.platform.worker;

import com.cryptostrategy.platform.worker.config.WorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WorkerProperties.class)
public class WorkerApplication {
    public static void main(String[] args) {
        ignoreAmbiguousShellDebugVariable();
        SpringApplication.run(WorkerApplication.class, args);
    }

    private static void ignoreAmbiguousShellDebugVariable() {
        if (System.getProperty("debug") == null) {
            System.setProperty("debug", "false");
        }
    }
}
