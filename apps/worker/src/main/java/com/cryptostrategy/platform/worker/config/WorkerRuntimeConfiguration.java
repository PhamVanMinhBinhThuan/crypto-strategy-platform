package com.cryptostrategy.platform.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class WorkerRuntimeConfiguration {
    @Bean
    WorkerRuntimeState workerRuntimeState() {
        return new WorkerRuntimeState(WorkerMode.IDLE);
    }

    enum WorkerMode {
        IDLE
    }

    record WorkerRuntimeState(WorkerMode mode) {
    }
}
