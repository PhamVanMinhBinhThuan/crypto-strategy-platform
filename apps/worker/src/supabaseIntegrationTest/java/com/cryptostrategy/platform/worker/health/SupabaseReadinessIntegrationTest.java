package com.cryptostrategy.platform.worker.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.worker.WorkerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = WorkerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
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
