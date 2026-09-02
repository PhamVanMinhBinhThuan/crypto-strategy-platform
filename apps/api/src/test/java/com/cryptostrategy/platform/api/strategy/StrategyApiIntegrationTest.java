package com.cryptostrategy.platform.api.strategy;

import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.CREATED_AT;
import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.DESCRIPTOR;
import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.DETAILS;
import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.PUBLISHED;
import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.STRATEGY_ID;
import static com.cryptostrategy.platform.api.strategy.StrategyApiFixtures.VERSION_ID;
import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.USER_A_ID;
import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.USER_B_ID;
import static com.cryptostrategy.platform.api.support.AuthenticatedUsers.authenticatedAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.strategy.api.error.StrategyErrorCode;
import com.cryptostrategy.platform.strategy.api.error.StrategyException;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import com.cryptostrategy.platform.strategy.api.model.user.SingleStrategyDraftSource;
import com.cryptostrategy.platform.strategy.api.model.user.command.CreateUserStrategyCommand;
import com.cryptostrategy.platform.strategy.api.model.user.command.PublishStrategyVersionCommand;
import com.cryptostrategy.platform.strategy.api.model.user.query.GetUserStrategyQuery;
import com.cryptostrategy.platform.strategy.api.model.user.query.StrategyCatalogPage;
import com.cryptostrategy.platform.strategy.api.model.user.query.UsableStrategyCatalog;
import com.cryptostrategy.platform.strategy.api.model.user.query.UsableStrategyPageRequest;
import com.cryptostrategy.platform.strategy.api.model.user.query.UserStrategyPage;
import com.cryptostrategy.platform.strategy.api.port.in.StrategyRegistry;
import com.cryptostrategy.platform.strategy.api.port.in.UserStrategyApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
            "spring.datasource.url=jdbc:h2:mem:strategy-api;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated"
        })
@AutoConfigureMockMvc
class StrategyApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

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
        when(strategies.listUsableStrategies(any(), any()))
                .thenAnswer(invocation -> catalog(
                        invocation.getArgument(0), invocation.getArgument(1)));
        when(strategies.createUserStrategy(eq(USER_A_ID), any(CreateUserStrategyCommand.class)))
                .thenReturn(DETAILS);
        when(strategies.getUserStrategy(
                        USER_A_ID, new GetUserStrategyQuery(STRATEGY_ID)))
                .thenReturn(DETAILS);
        when(strategies.getUserStrategy(
                        USER_B_ID, new GetUserStrategyQuery(STRATEGY_ID)))
                .thenThrow(new StrategyException(
                        StrategyErrorCode.STRATEGY_NOT_FOUND, "private fixture must not leak"));
        when(strategies.publish(eq(USER_A_ID), any(PublishStrategyVersionCommand.class)))
                .thenReturn(PUBLISHED);
        when(strategies.archive(eq(USER_A_ID), any()))
                .thenReturn(StrategyApiFixtures.archivedDetails());
    }

    @Test
    void pagesSystemCatalogWithVersionedRulesAndOpaqueCursor() throws Exception {
        String firstBody = mockMvc.perform(get("/api/v1/strategies")
                        .with(authenticatedAs(USER_A_ID))
                        .queryParam("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].strategyId").value("ma-crossover"))
                .andExpect(jsonPath("$.items[0].strategyVersionId")
                        .value(DESCRIPTOR.reference().strategyVersionId().value()))
                .andExpect(jsonPath("$.items[0].version").value("1.0.0"))
                .andExpect(jsonPath("$.items[0].requiredLookback").value(25))
                .andExpect(jsonPath("$.items[0].supportedSignals[0]").value("BUY"))
                .andExpect(jsonPath("$.items[0].parameters[0].name").value("fastPeriod"))
                .andExpect(jsonPath("$.items[0].parameters[0].defaultValue").value("5"))
                .andExpect(jsonPath("$.items[0].parameters[2].minimum")
                        .value("0.000000000001"))
                .andExpect(jsonPath("$.items[0].parameters[2].defaultValue")
                        .value("0.100000000001"))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String cursor = json.readTree(firstBody).path("nextCursor").asText();
        assertThat(cursor).isNotBlank().isNotEqualTo("1");

        mockMvc.perform(get("/api/v1/strategies")
                        .with(authenticatedAs(USER_A_ID))
                        .queryParam("limit", "1")
                        .queryParam("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.hasMore").value(false));

        ArgumentCaptor<UsableStrategyPageRequest> requests =
                ArgumentCaptor.forClass(UsableStrategyPageRequest.class);
        verify(strategies, org.mockito.Mockito.times(2))
                .listUsableStrategies(eq(USER_A_ID), requests.capture());
        assertThat(requests.getAllValues().get(1).systemCursor()).contains("1");
    }

    @Test
    void createsPublishesReadsAndListsOnlyTheAuthenticatedUsersStrategy()
            throws Exception {
        String createRequest = """
                {
                  "name": "Private MA",
                  "description": "Exact private parameters",
                  "kind": "SINGLE",
                  "source": {
                    "type": "SINGLE",
                    "strategy": {
                      "strategyId": "ma-crossover",
                      "version": "1.0.0",
                      "parameters": {
                        "fastPeriod": 5,
                        "slowPeriod": 25,
                        "threshold": "0.100000000001"
                      }
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/user-strategies")
                        .with(authenticatedAs(USER_A_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/v1/user-strategies/" + STRATEGY_ID.value()))
                .andExpect(jsonPath("$.userStrategyId").value(STRATEGY_ID.value()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.latestVersion.userStrategyVersionId")
                        .value(VERSION_ID.value()))
                .andExpect(jsonPath("$.latestVersion.source.strategy.parameters.threshold")
                        .value("0.100000000001"));

        ArgumentCaptor<CreateUserStrategyCommand> commands =
                ArgumentCaptor.forClass(CreateUserStrategyCommand.class);
        verify(strategies).createUserStrategy(eq(USER_A_ID), commands.capture());
        var supplied = ((SingleStrategyDraftSource) commands.getValue().source())
                .parameters()
                .require("threshold");
        assertThat(supplied).isInstanceOf(StrategyParameterValue.DecimalValue.class);
        assertThat(supplied.canonicalText()).isEqualTo("0.100000000001");

        mockMvc.perform(get("/api/v1/user-strategies/{id}", STRATEGY_ID.value())
                        .with(authenticatedAs(USER_A_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Private MA"));

        mockMvc.perform(post(
                                "/api/v1/user-strategies/{id}/versions/{version}/publish",
                                STRATEGY_ID.value(),
                                VERSION_ID.value())
                        .with(authenticatedAs(USER_A_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersionNo\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.versionNo").value(1))
                .andExpect(jsonPath("$.publishedAt")
                        .value(CREATED_AT.plusSeconds(60).toString()));

        verify(strategies).publish(
                USER_A_ID,
                new PublishStrategyVersionCommand(STRATEGY_ID, VERSION_ID, 1));

        mockMvc.perform(post(
                                "/api/v1/user-strategies/{id}/archive",
                                STRATEGY_ID.value())
                        .with(authenticatedAs(USER_A_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.latestVersion.userStrategyVersionId")
                        .value(VERSION_ID.value()));

        mockMvc.perform(get("/api/v1/user-strategies")
                        .with(authenticatedAs(USER_A_ID))
                        .queryParam("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].userStrategyId")
                        .value(STRATEGY_ID.value()));

        mockMvc.perform(get("/api/v1/user-strategies")
                        .with(authenticatedAs(USER_B_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        String inaccessible = mockMvc.perform(get(
                                "/api/v1/user-strategies/{id}", STRATEGY_ID.value())
                        .with(authenticatedAs(USER_B_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STRATEGY_NOT_FOUND"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(inaccessible)
                .doesNotContain("Private MA")
                .doesNotContain("private fixture must not leak");
    }

    @Test
    void rejectsCursorFromAnotherStrategyCollection() throws Exception {
        String body = mockMvc.perform(get("/api/v1/strategies")
                        .with(authenticatedAs(USER_A_ID))
                        .queryParam("limit", "1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String systemCursor = json.readTree(body).path("nextCursor").asText();

        mockMvc.perform(get("/api/v1/user-strategies")
                        .with(authenticatedAs(USER_A_ID))
                        .queryParam("cursor", systemCursor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));

        String negativeOffset = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("v1:SYSTEM:-1".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(get("/api/v1/strategies")
                        .with(authenticatedAs(USER_A_ID))
                        .queryParam("cursor", negativeOffset))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
    }

    private static UsableStrategyCatalog catalog(
            java.util.UUID owner, UsableStrategyPageRequest request) {
        boolean firstSystemPage = request.systemCursor().isEmpty();
        StrategyCatalogPage system = new StrategyCatalogPage(
                firstSystemPage ? List.of(DESCRIPTOR) : List.of(),
                firstSystemPage ? Optional.of("1") : Optional.empty());
        boolean firstPrivatePage = owner.equals(USER_A_ID)
                && request.privateCursor().isEmpty();
        UserStrategyPage privatePage = new UserStrategyPage(
                firstPrivatePage ? List.of(StrategyApiFixtures.summary()) : List.of(),
                firstPrivatePage ? Optional.of(STRATEGY_ID.value()) : Optional.empty());
        return new UsableStrategyCatalog(system, privatePage);
    }
}
