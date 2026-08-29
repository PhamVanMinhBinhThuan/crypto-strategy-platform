package com.cryptostrategy.platform.api.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.api.ApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SupabaseReadinessIntegrationTest {
    private final HealthIndicator databaseReadiness;

    @Autowired
    SupabaseReadinessIntegrationTest(@Qualifier("databaseReadiness") HealthIndicator databaseReadiness) {
        this.databaseReadiness = databaseReadiness;
    }

    @Test
    void readinessUsesConnectionValidationOnly() {
        assertThat(databaseReadiness.health().getStatus()).isEqualTo(Status.UP);
    }
}
