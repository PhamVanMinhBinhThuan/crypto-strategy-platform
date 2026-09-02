package com.cryptostrategy.platform.api.realtime;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
class RealtimeLifecycleConfiguration {
    @Bean
    Clock realtimeClock() {
        return Clock.systemUTC();
    }

    @Bean(name = "realtimeTaskScheduler", destroyMethod = "shutdown")
    ThreadPoolTaskScheduler realtimeTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("realtime-lifecycle-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
