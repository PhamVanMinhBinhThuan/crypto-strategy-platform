package com.cryptostrategy.platform.worker.config;

import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class HealthConfiguration {
    @Bean
    DatabaseConfigurationValidated databaseConfigurationValidated(
            @Value("${spring.datasource.url:}") String url,
            @Value("${spring.datasource.username:}") String username,
            @Value("${spring.datasource.password:}") String password) {
        requireNonBlank("DATABASE_URL", url);
        requireNonBlank("DATABASE_USERNAME", username);
        requireNonBlank("DATABASE_PASSWORD", password);
        return new DatabaseConfigurationValidated();
    }

    @Bean("databaseReadiness")
    HealthIndicator databaseReadiness(DataSource dataSource, DatabaseConfigurationValidated ignored) {
        return () -> connectionHealth(dataSource);
    }

    private static Health connectionHealth(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? Health.up().build() : Health.down().build();
        } catch (Exception ignored) {
            return Health.down().build();
        }
    }

    private static void requireNonBlank(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration: " + key);
        }
    }

    static final class DatabaseConfigurationValidated {
    }
}
