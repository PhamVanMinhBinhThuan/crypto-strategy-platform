package com.cryptostrategy.platform.api.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.error.InvalidStateTransitionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:public-errors;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated"
        })
@AutoConfigureMockMvc
@Import(PublicErrorContractTest.FixtureConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class PublicErrorContractTest {
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final UUID USER_ID = UUID.fromString("9eb2106e-80b2-4e3f-b46a-f17f30aab120");
    private static final String SENSITIVE_FRAGMENT = "SUPER_SECRET_TOKEN_VALUE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void validationFailureUsesTheRequiredEnvelopeAndUtcTimestamp() throws Exception {
        Instant before = Instant.now();

        MvcResult result = expectError("validation", "VALIDATION-CONTRACT-123", 400,
                "REQUEST_VALIDATION_FAILED");

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("code", "message", "details", "correlationId", "timestamp");
        assertThat(body.path("message").asText()).isEqualTo("Request validation failed.");
        assertThat(body.path("details").isObject()).isTrue();
        assertThat(body.path("details").isEmpty()).isTrue();

        String serializedTimestamp = body.path("timestamp").asText();
        Instant timestamp = Instant.parse(serializedTimestamp);
        assertThat(serializedTimestamp).endsWith("Z");
        assertThat(timestamp).isBetween(before, Instant.now());
    }

    @Test
    void inaccessibleResourceAlwaysUsesOwnershipSafeNotFound() throws Exception {
        String body = expectError("inaccessible", "NOT-FOUND-CONTRACT-123", 404,
                "RESOURCE_NOT_FOUND")
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .contains("The requested resource was not found.")
                .doesNotContain("foreign-experiment-id")
                .doesNotContain(USER_ID.toString());
    }

    @Test
    void knownConflictsUseStableConflictCodesWithoutLeakingExceptionMessages() throws Exception {
        String idempotencyBody = expectError("idempotency", "IDEMPOTENCY-CONTRACT-123", 409,
                "IDEMPOTENCY_KEY_CONFLICT")
                .getResponse()
                .getContentAsString();
        String transitionBody = expectError("state", "STATE-CONTRACT-123", 409,
                "INVALID_STATE_TRANSITION")
                .getResponse()
                .getContentAsString();

        assertThat(idempotencyBody).doesNotContain(SENSITIVE_FRAGMENT);
        assertThat(transitionBody).doesNotContain("internal-state=QUEUED");
    }

    @Test
    void unexpectedFailureReturnsAndLogsOnlySafeInformation(CapturedOutput output) throws Exception {
        String body = expectError("unexpected", "INTERNAL-CONTRACT-123", 500, "INTERNAL_ERROR")
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .doesNotContain(SENSITIVE_FRAGMENT)
                .doesNotContain("SELECT * FROM private_credentials")
                .doesNotContain("/srv/private/provider-response.json")
                .doesNotContain("stackTrace")
                .doesNotContain("IllegalStateException");
        assertThat(output)
                .doesNotContain(SENSITIVE_FRAGMENT)
                .doesNotContain("SELECT * FROM private_credentials")
                .doesNotContain("/srv/private/provider-response.json");
    }

    @Test
    void envelopeDefensivelyCopiesAllowlistedStructuredDetails() {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        fieldErrors.add(new LinkedHashMap<>(Map.of(
                "field", "limit",
                "reason", "must be between 1 and 200")));
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fieldErrors", fieldErrors);
        details.put("retryable", false);

        ErrorEnvelope envelope = new ErrorEnvelope(
                "REQUEST_VALIDATION_FAILED",
                "Request validation failed.",
                details,
                "DETAILS-CONTRACT-123",
                Instant.parse("2026-09-01T00:00:00Z"));
        fieldErrors.getFirst().put("reason", SENSITIVE_FRAGMENT);
        fieldErrors.add(Map.of("field", "token", "reason", SENSITIVE_FRAGMENT));
        details.put("resourceId", "late-mutation");

        assertThat(envelope.details()).containsOnlyKeys("fieldErrors", "retryable");
        assertThat(envelope.details().toString()).doesNotContain(SENSITIVE_FRAGMENT);
        assertThatThrownBy(() -> envelope.details().put("token", SENSITIVE_FRAGMENT))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void envelopeRejectsNonAllowlistedDetailFields() {
        assertThatThrownBy(() -> new ErrorEnvelope(
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                Map.of("providerPayload", SENSITIVE_FRAGMENT),
                "DETAILS-CONTRACT-456",
                Instant.parse("2026-09-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerPayload");
    }

    private MvcResult expectError(String fixture, String correlationId, int status, String code)
            throws Exception {
        return mockMvc.perform(get("/__errors/{fixture}", fixture)
                        .with(authenticatedAs(USER_ID))
                        .header(CORRELATION_HEADER, correlationId))
                .andExpect(status().is(status))
                .andExpect(header().string(CORRELATION_HEADER, correlationId))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andExpect(jsonPath("$.timestamp").isString())
                .andReturn();
    }

    private static RequestPostProcessor authenticatedAs(UUID userId) {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUserContext(userId),
                "fixture",
                List.of());
        return authentication(authentication);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixtureConfiguration {
        @Bean
        ErrorFixtureController errorFixtureController() {
            return new ErrorFixtureController();
        }
    }

    @RestController
    static class ErrorFixtureController {
        @GetMapping("/__errors/{fixture}")
        void fail(@PathVariable String fixture) {
            switch (fixture) {
                case "validation" -> throw new IllegalArgumentException(
                        "invalid token=" + SENSITIVE_FRAGMENT);
                case "inaccessible" -> throw new com.cryptostrategy.platform.experiment.api.error
                        .ResourceInaccessibleException(
                        "foreign-experiment-id owner=" + USER_ID);
                case "idempotency" -> throw new IdempotencyConflictException(
                        "key=" + SENSITIVE_FRAGMENT + " was reused with another payload");
                case "state" -> throw new InvalidStateTransitionException(
                        "internal-state=QUEUED cannot transition");
                default -> throw new IllegalStateException(
                        "token=" + SENSITIVE_FRAGMENT
                                + " SQL=SELECT * FROM private_credentials"
                                + " path=/srv/private/provider-response.json");
            }
        }
    }
}
