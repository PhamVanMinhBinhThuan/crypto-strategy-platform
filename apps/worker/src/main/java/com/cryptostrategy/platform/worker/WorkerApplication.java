package com.cryptostrategy.platform.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
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
