package com.cryptostrategy.platform.api.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.api.ApiApplication;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:api-health;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated"
        })
@AutoConfigureMockMvc(addFilters = false)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiHealthIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Test
    void livenessAndReadinessAreUpWhenDatabaseIsReachable() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }

    @Test
    void databaseFailureOnlyMakesReadinessUnavailable() throws Exception {
        ((HikariDataSource) dataSource).close();

        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void missingDatabasePasswordFailsFastWithKeyButNotSecretValue() {
        Exception failure = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                new SpringApplicationBuilder(ApiApplication.class)
                        .web(WebApplicationType.NONE)
                        .run(
                                "--spring.datasource.url=jdbc:h2:mem:missing-api",
                                "--spring.datasource.username=sa",
                                "--spring.datasource.password=",
                                "--platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
                                "--platform.security.jwt.jwk-set-uri=https://fixture.invalid/jwks.json",
                                "--platform.security.jwt.audience=authenticated"));

        String messages = exceptionMessages(failure);
        assertThat(messages).contains("DATABASE_PASSWORD");
        assertThat(messages).doesNotContain("fixture-password");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    private static String exceptionMessages(Throwable throwable) {
        List<String> messages = new ArrayList<>();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                messages.add(current.getMessage());
            }
            current = current.getCause();
        }
        return String.join(" | ", messages);
    }
}
