package com.cryptostrategy.platform.api.config;

import com.cryptostrategy.platform.experiment.api.port.out.IdempotencyStore;
import com.cryptostrategy.platform.persistence.api.ExperimentPersistenceFactory;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdempotencyConfiguration {
    @Bean
    IdempotencyStore idempotencyStore(DataSource dataSource) {
        return new ExperimentPersistenceFactory(dataSource).createIdempotencyStore();
    }
}
