package com.cryptostrategy.platform.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:worker-health;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "management.endpoint.health.group.readiness.include=readinessState,databaseReadiness",
            "spring.main.cloud-platform=kubernetes"
        })
@AutoConfigureMockMvc(addFilters = false)
class WorkerHealthIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListableBeanFactory beanFactory;

    @Test
    void workerStartsHealthyAndIdleWithoutQueueOrRedisConsumer() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        assertThat(beanFactory.getBean("workerRuntimeState").toString()).contains("IDLE");
        assertThat(Arrays.stream(beanFactory.getBeanDefinitionNames())
                        .map(String::toLowerCase)
                        .filter(name -> name.contains("redis") || name.contains("queue") || name.contains("jobconsumer")))
                .isEmpty();
    }

    @Test
    void missingDatabaseUsernameFailsFastWithKeyButNotSecretValue() {
        Exception failure = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                new SpringApplicationBuilder(WorkerApplication.class)
                        .web(WebApplicationType.NONE)
                        .run(
                                "--spring.datasource.url=jdbc:h2:mem:missing-worker",
                                "--spring.datasource.username=",
                                "--spring.datasource.password=not-printed"));

        String messages = exceptionMessages(failure);
        assertThat(messages).contains("DATABASE_USERNAME");
        assertThat(messages).doesNotContain("not-printed");
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
