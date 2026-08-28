package com.cryptostrategy.platform.api.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:correlation;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated"
        })
@AutoConfigureMockMvc
@Import(CorrelationIntegrationTest.FixtureConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class CorrelationIntegrationTest {
    private static final String HEADER = "X-Correlation-Id";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providedCorrelationIdIsPreservedInSuccessResponseAndLog(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/__test/success").header(HEADER, "CLIENT-CORRELATION-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER, "CLIENT-CORRELATION-123"));

        assertThat(output).contains("CLIENT-CORRELATION-123");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void invalidCorrelationIdIsReplacedByUppercaseUlid() throws Exception {
        String generated = mockMvc.perform(get("/__test/success").header(HEADER, "bad\nid"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HEADER);

        assertThat(generated).matches("[0-9A-HJKMNP-TV-Z]{26}");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void validationFailureUsesSafeEnvelopeWithSameCorrelationId() throws Exception {
        mockMvc.perform(get("/__test/validation").header(HEADER, "VALIDATION-123"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HEADER, "VALIDATION-123"))
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.correlationId").value("VALIDATION-123"))
                .andExpect(jsonPath("$.timestamp").exists());
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void authenticationFailureUsesSafeEnvelopeWithSameCorrelationId() throws Exception {
        mockMvc.perform(get("/__test/authentication").header(HEADER, "AUTH-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HEADER, "AUTH-123"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.correlationId").value("AUTH-123"));
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void unexpectedFailureDoesNotExposeOrLogSecret(CapturedOutput output) throws Exception {
        String body = mockMvc.perform(get("/__test/unexpected")
                        .header(HEADER, "ERROR-123")
                        .header("Authorization", "Bearer SUPER_SECRET_TOKEN_VALUE"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(HEADER, "ERROR-123"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.correlationId").value("ERROR-123"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("SUPER_SECRET_TOKEN_VALUE").doesNotContain("stackTrace");
        assertThat(output).doesNotContain("SUPER_SECRET_TOKEN_VALUE");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixtureConfiguration {
        @Bean
        FixtureController fixtureController() {
            return new FixtureController();
        }

        @Bean
        @Order(0)
        SecurityFilterChain fixtureSecurity(HttpSecurity http) throws Exception {
            return http.securityMatcher("/__test/**")
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                    .build();
        }
    }

    @RestController
    static class FixtureController {
        @GetMapping("/__test/success")
        Map<String, String> success() {
            return Map.of("status", "ok");
        }

        @GetMapping("/__test/validation")
        void validation() {
            throw new IllegalArgumentException("unsafe validation detail");
        }

        @GetMapping("/__test/authentication")
        void authentication() {
            throw new AuthenticationCredentialsNotFoundException("unsafe authentication detail");
        }

        @GetMapping("/__test/unexpected")
        void unexpected() {
            throw new IllegalStateException("unexpected fixture failure");
        }
    }
}
