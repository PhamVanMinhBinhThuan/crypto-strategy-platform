package com.cryptostrategy.platform.api.strategy;

import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.DESCRIPTOR;
import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.DETAILS;
import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.NEXT_DRAFT;
import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.STRATEGY_ID;
import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.VERSION_ID;
import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.USER_A_ID;
import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.authenticatedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.user.command.CreateNextStrategyVersionCommand;
import com.cryptostrategy.platform.strategy.api.model.user.command.PublishStrategyVersionCommand;
import com.cryptostrategy.platform.strategy.api.model.user.query.GetUserStrategyQuery;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.cryptostrategy.platform.strategy.api.port.in.UserStrategyApplication;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:strategy-conflict;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated"
        })
@AutoConfigureMockMvc
class StrategyVersionConflictTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserStrategyApplication strategies;

    @MockitoBean
    private StrategyRegistry registry;

    @BeforeEach
    void publishStrategyFixtures() {
        when(registry.descriptor(
                        new StrategyPluginId("ma-crossover"),
                        new SemanticVersion(1, 0, 0)))
                .thenReturn(DESCRIPTOR);
        when(strategies.getUserStrategy(
                        USER_A_ID, new GetUserStrategyQuery(STRATEGY_ID)))
                .thenReturn(DETAILS);
    }

    @Test
    void concurrentNextVersionRequestsYieldOneVersionAndOneStableConflict()
            throws Exception {
        AtomicBoolean accepted = new AtomicBoolean();
        when(strategies.createNextVersion(
                        eq(USER_A_ID), any(CreateNextStrategyVersionCommand.class)))
                .thenAnswer(invocation -> {
                    if (accepted.compareAndSet(false, true)) {
                        return NEXT_DRAFT;
                    }
                    throw conflict();
                });

        Callable<Integer> request = () -> mockMvc.perform(post(
                                "/api/v1/user-strategies/{id}/versions",
                                STRATEGY_ID.value())
                        .with(authenticatedAs(USER_A_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nextVersionBody(1)))
                .andReturn()
                .getResponse()
                .getStatus();

        List<Integer> statuses;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(request);
            var second = executor.submit(request);
            statuses = List.of(first.get(), second.get());
        }

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        ArgumentCaptor<CreateNextStrategyVersionCommand> commands =
                ArgumentCaptor.forClass(CreateNextStrategyVersionCommand.class);
        verify(strategies, org.mockito.Mockito.times(2))
                .createNextVersion(eq(USER_A_ID), commands.capture());
        assertThat(commands.getAllValues())
                .extracting(CreateNextStrategyVersionCommand::expectedLatestVersionNo)
                .containsOnly(1);
    }

    @Test
    void stalePublishExpectationReturnsSafeConflictBoundToPathRoot()
            throws Exception {
        when(strategies.publish(eq(USER_A_ID), any(PublishStrategyVersionCommand.class)))
                .thenThrow(conflict());

        mockMvc.perform(post(
                                "/api/v1/user-strategies/{id}/versions/{version}/publish",
                                STRATEGY_ID.value(),
                                VERSION_ID.value())
                        .with(authenticatedAs(USER_A_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersionNo\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"))
                .andExpect(jsonPath("$.message")
                        .value("The strategy version conflicts with the current state."));

        verify(strategies).publish(
                USER_A_ID,
                new PublishStrategyVersionCommand(STRATEGY_ID, VERSION_ID, 1));
    }

    @Test
    void invalidVersionExpectationDoesNotInvokeMutation() throws Exception {
        mockMvc.perform(post(
                                "/api/v1/user-strategies/{id}/versions",
                                STRATEGY_ID.value())
                        .with(authenticatedAs(USER_A_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nextVersionBody(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));

        verify(strategies, never())
                .createNextVersion(eq(USER_A_ID), any(CreateNextStrategyVersionCommand.class));
    }

    private static String nextVersionBody(int expectedLatestVersionNo) {
        return """
                {
                  "expectedLatestVersionNo": %d,
                  "source": {
                    "type": "SINGLE",
                    "strategy": {
                      "strategyId": "ma-crossover",
                      "version": "1.0.0",
                      "parameters": {
                        "fastPeriod": 6,
                        "slowPeriod": 30,
                        "threshold": "0.200000000001"
                      }
                    }
                  }
                }
                """.formatted(expectedLatestVersionNo);
    }

    private static StrategyException conflict() {
        return new StrategyException(
                StrategyErrorCode.STRATEGY_CONFLICT,
                "raw current version and storage details must stay private");
    }
}
