package com.cryptostrategy.platform.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.auth.WebSocketTicketService;
import com.cryptostrategy.platform.news.api.model.NewsId;
import com.cryptostrategy.platform.news.api.port.in.GetSentimentAuditUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:security-boundary;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated",
            "platform.security.allowed-origins=https://dashboard.example.test",
            "news.audit.service-token=internal-fixture-token"
        })
@AutoConfigureMockMvc
class SecurityBoundaryIntegrationTest {
    private static final UUID USER_ID =
            UUID.fromString("26b58306-aec9-4e70-b57c-25f77ac9e452");
    private static final String ALLOWED_ORIGIN = "https://dashboard.example.test";
    private static final String NEWS_ID = "10000000000000000000000001";
    private static final Instant AUTHENTICATION_EXPIRES_AT =
            Instant.parse("2026-09-02T03:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebSocketTicketService tickets;

    @MockitoBean
    private GetSentimentAuditUseCase sentimentAudit;

    @Test
    void websocketTicketEndpointAllowsOnlyConfiguredOrigin() throws Exception {
        when(tickets.issue(USER_ID, ALLOWED_ORIGIN, AUTHENTICATION_EXPIRES_AT)).thenReturn(
                new WebSocketTicketService.IssuedTicket(
                        "one-time-ticket",
                        Instant.parse("2026-09-02T02:00:00Z")));

        mockMvc.perform(post("/api/v1/realtime/ticket")
                        .with(authenticatedAs(USER_ID))
                        .header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").value("one-time-ticket"));
        mockMvc.perform(post("/api/v1/realtime/ticket")
                        .with(authenticatedAs(USER_ID))
                        .header("Origin", "https://dashboard.example.test.evil.invalid"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ORIGIN"));
        mockMvc.perform(post("/api/v1/realtime/ticket")
                        .with(authenticatedAs(USER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN_ORIGIN"));

        verify(tickets).issue(USER_ID, ALLOWED_ORIGIN, AUTHENTICATION_EXPIRES_AT);
        verifyNoMoreInteractions(tickets);
    }

    @Test
    void browserApiPreflightUsesTheConfiguredOriginAllowlistWithoutAuthentication() throws Exception {
        mockMvc.perform(options("/api/v1/experiments/example")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization,x-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));

        mockMvc.perform(options("/api/v1/experiments/example")
                        .header("Origin", "https://dashboard.example.test.evil.invalid")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalAuditRequiresDedicatedTokenAndRejectsBrowserAuthority() throws Exception {
        NewsId newsId = new NewsId(NEWS_ID);
        when(sentimentAudit.findLatest(any(NewsId.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/internal/news-items/{newsId}/sentiment", NEWS_ID)
                        .header("Authorization", "Bearer internal-fixture-token"))
                .andExpect(status().isNotFound());
        verify(sentimentAudit).findLatest(newsId);

        mockMvc.perform(get("/internal/news-items/{newsId}/sentiment", NEWS_ID)
                        .with(authenticatedAs(USER_ID))
                        .header("Authorization", "Bearer browser-user-token"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/internal/news-items/{newsId}/sentiment", NEWS_ID)
                        .with(authenticatedAs(USER_ID)))
                .andExpect(status().isUnauthorized());

        verifyNoMoreInteractions(sentimentAudit);
    }

    private static RequestPostProcessor authenticatedAs(UUID userId) {
        var user = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUserContext(userId, AUTHENTICATION_EXPIRES_AT),
                "fixture",
                List.of());
        return authentication(user);
    }
}
