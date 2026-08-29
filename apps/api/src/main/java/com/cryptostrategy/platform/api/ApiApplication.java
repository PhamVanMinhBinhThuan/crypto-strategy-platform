package com.cryptostrategy.platform.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {
    public static void main(String[] args) {
        ignoreAmbiguousShellDebugVariable();
        SpringApplication.run(ApiApplication.class, args);
    }

    private static void ignoreAmbiguousShellDebugVariable() {
        if (System.getProperty("debug") == null) {
            System.setProperty("debug", "false");
        }
    }
}
