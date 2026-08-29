package com.cryptostrategy.platform.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.api.ApiApplication;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:authentication;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password"
        })
@AutoConfigureMockMvc
@Import(AuthenticationIntegrationTest.FixtureConfiguration.class)
class AuthenticationIntegrationTest {
    private static final JwtTestFixture JWT = JwtTestFixture.start();
    private static final UUID USER_ID = UUID.fromString("9b0f36b1-6004-49aa-a6d1-1cc2f373741f");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProtectedFixtureController controller;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("platform.security.jwt.issuer", () -> JwtTestFixture.ISSUER);
        registry.add("platform.security.jwt.jwk-set-uri", JWT::jwksUri);
        registry.add("platform.security.jwt.audience", () -> JwtTestFixture.AUDIENCE);
    }

    @BeforeEach
    void resetInvocationCount() {
        controller.invocations.set(0);
    }

    @AfterAll
    static void stopJwksServer() {
        JWT.close();
    }

    @Test
    void missingTokenIsRejectedBeforeProtectedHandler() throws Exception {
        assertUnauthorized(get("/__auth/user"), "MISSING-123");
    }

    @Test
    void malformedAuthorizationSchemeIsRejectedWithoutEcho() throws Exception {
        String response = mockMvc.perform(get("/__auth/user")
                        .header("X-Correlation-Id", "MALFORMED-123")
                        .header("Authorization", "Token raw-secret-value"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-Id", "MALFORMED-123"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("raw-secret-value");
        assertThat(controller.invocations).hasValue(0);
    }

    @ParameterizedTest(name = "rejects {0}")
    @MethodSource("invalidTokens")
    void invalidTokensAreRejectedBeforeProtectedHandler(String caseName, String token) throws Exception {
        String response = mockMvc.perform(get("/__auth/user")
                        .header("X-Correlation-Id", "INVALID-123")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-Id", "INVALID-123"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.correlationId").value("INVALID-123"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain(token);
        assertThat(controller.invocations).hasValue(0);
    }

    @Test
    void validTokenProvidesExactCanonicalUserContext() throws Exception {
        mockMvc.perform(get("/__auth/user")
                        .header("X-Correlation-Id", "VALID-123")
                        .header("Authorization", "Bearer " + JWT.validToken(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "VALID-123"))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()));

        assertThat(controller.invocations).hasValue(1);
    }

    @Test
    void missingJwtAudienceFailsFastWithConfigurationKeyOnly() {
        Exception failure = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                new SpringApplicationBuilder(ApiApplication.class)
                        .web(WebApplicationType.NONE)
                        .run(
                                "--spring.datasource.url=jdbc:h2:mem:missing-jwt",
                                "--spring.datasource.username=sa",
                                "--spring.datasource.password=fixture-password",
                                "--platform.security.jwt.issuer=" + JwtTestFixture.ISSUER,
                                "--platform.security.jwt.jwk-set-uri=" + JWT.jwksUri(),
                                "--platform.security.jwt.audience="));

        String messages = exceptionMessages(failure);
        assertThat(messages).contains("SUPABASE_JWT_AUDIENCE");
        assertThat(messages).doesNotContain("fixture-password");
    }

    private void assertUnauthorized(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                    String correlationId) throws Exception {
        mockMvc.perform(request.header("X-Correlation-Id", correlationId))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Correlation-Id", correlationId))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.correlationId").value(correlationId));
        assertThat(controller.invocations).hasValue(0);
    }

    static Stream<Arguments> invalidTokens() {
        return Stream.of(
                Arguments.of("malformed token", "not-a-jwt"),
                Arguments.of("invalid signature", JWT.invalidSignatureToken(USER_ID)),
                Arguments.of("unknown key", JWT.unknownKeyToken(USER_ID)),
                Arguments.of("expired token", JWT.expiredToken(USER_ID)),
                Arguments.of("not-yet-valid token", JWT.notYetValidToken(USER_ID)),
                Arguments.of("wrong issuer", JWT.wrongIssuerToken(USER_ID)),
                Arguments.of("wrong audience", JWT.wrongAudienceToken(USER_ID)),
                Arguments.of("missing audience", JWT.missingAudienceToken(USER_ID)),
                Arguments.of("non-UUID subject", JWT.nonUuidSubjectToken()),
                Arguments.of("missing subject", JWT.missingSubjectToken()));
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

    @TestConfiguration(proxyBeanMethods = false)
    static class FixtureConfiguration {
        @Bean
        ProtectedFixtureController protectedFixtureController() {
            return new ProtectedFixtureController();
        }
    }

    @RestController
    static class ProtectedFixtureController {
        private final AtomicInteger invocations = new AtomicInteger();

        @GetMapping("/__auth/user")
        UserResponse currentUser(@AuthenticationPrincipal AuthenticatedUserContext userContext) {
            invocations.incrementAndGet();
            return new UserResponse(userContext.userId());
        }
    }

    record UserResponse(UUID userId) {
    }
}
