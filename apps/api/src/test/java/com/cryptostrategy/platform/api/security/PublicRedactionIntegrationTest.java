package com.cryptostrategy.platform.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataErrorCode;
import com.cryptostrategy.platform.marketdata.api.error.MarketDataException;
import com.cryptostrategy.platform.news.api.error.NewsErrorCode;
import com.cryptostrategy.platform.news.api.error.NewsException;
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
            "spring.datasource.url=jdbc:h2:mem:public-redaction;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated"
        })
@AutoConfigureMockMvc
@Import(PublicRedactionIntegrationTest.FixtureConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class PublicRedactionIntegrationTest {
    private static final UUID USER_ID =
            UUID.fromString("ff19185c-5cd7-4ccc-ad9d-ed0b4dd25782");
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.SUPER_SECRET_JWT";
    private static final String SECRET = "SUPER_SECRET_DATABASE_PASSWORD";
    private static final String SQL = "SELECT api_key FROM private_credentials";
    private static final String PATH = "/srv/private/provider-response.json";
    private static final String STACK_TRACE = "at private.provider.Client.call(Client.java:42)";
    private static final String PROVIDER_PAYLOAD = "RAW_PROVIDER_PAYLOAD_WITH_API_KEY";
    private static final List<String> SENSITIVE_FRAGMENTS =
            List.of(TOKEN, SECRET, SQL, PATH, STACK_TRACE, PROVIDER_PAYLOAD);

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicErrorsAndLogsRedactExceptionMessagesCausesAndProviderContext(
            CapturedOutput output) throws Exception {
        List<ExpectedFailure> failures = List.of(
                new ExpectedFailure("validation", 400, "REQUEST_VALIDATION_FAILED"),
                new ExpectedFailure("inaccessible", 404, "RESOURCE_NOT_FOUND"),
                new ExpectedFailure("market-provider", 502, "MARKET_DATA_MAPPING_FAILED"),
                new ExpectedFailure("sentiment-provider", 502, "SENTIMENT_RESPONSE_INVALID"),
                new ExpectedFailure("unexpected", 500, "INTERNAL_ERROR"));

        for (ExpectedFailure failure : failures) {
            MvcResult result = mockMvc.perform(get("/__redaction/{fixture}", failure.fixture())
                            .with(authenticatedAs(USER_ID))
                            .header("X-Correlation-Id", "REDACTION-" + failure.status()))
                    .andExpect(status().is(failure.status()))
                    .andExpect(header().string(
                            "X-Correlation-Id", "REDACTION-" + failure.status()))
                    .andExpect(jsonPath("$.code").value(failure.code()))
                    .andExpect(jsonPath("$.details").isMap())
                    .andReturn();

            assertPublicResponseRedacted(result.getResponse().getContentAsString());
        }

        assertSensitiveFragmentsRedacted(output.getAll());
    }

    private static void assertPublicResponseRedacted(String value) {
        assertSensitiveFragmentsRedacted(value);
        assertThat(value)
                .doesNotContain("java.lang.IllegalStateException")
                .doesNotContain("MarketDataException")
                .doesNotContain("NewsException");
    }

    private static void assertSensitiveFragmentsRedacted(String value) {
        assertThat(value).doesNotContain(SENSITIVE_FRAGMENTS.toArray(String[]::new));
    }

    private static RequestPostProcessor authenticatedAs(UUID userId) {
        var user = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUserContext(userId),
                "fixture",
                List.of());
        return authentication(user);
    }

    private record ExpectedFailure(String fixture, int status, String code) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class FixtureConfiguration {
        @Bean
        RedactionFixtureController redactionFixtureController() {
            return new RedactionFixtureController();
        }
    }

    @RestController
    static class RedactionFixtureController {
        @GetMapping("/__redaction/{fixture}")
        void fail(@PathVariable String fixture) {
            String sensitiveMessage = String.join(
                    " ", TOKEN, SECRET, SQL, PATH, STACK_TRACE, PROVIDER_PAYLOAD);
            switch (fixture) {
                case "validation" -> throw new IllegalArgumentException(sensitiveMessage);
                case "inaccessible" -> throw new ResourceInaccessibleException();
                case "market-provider" -> throw new MarketDataException(
                        MarketDataErrorCode.MARKET_DATA_MAPPING_FAILED,
                        sensitiveMessage,
                        Map.of("providerPayload", PROVIDER_PAYLOAD),
                        new IllegalStateException(sensitiveMessage));
                case "sentiment-provider" -> throw new NewsException(
                        NewsErrorCode.INVALID_SENTIMENT_RESPONSE,
                        sensitiveMessage,
                        Map.of("providerPayload", PROVIDER_PAYLOAD),
                        new IllegalStateException(sensitiveMessage));
                default -> throw new IllegalStateException(sensitiveMessage);
            }
        }
    }
}
