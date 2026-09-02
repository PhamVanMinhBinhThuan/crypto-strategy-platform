package com.cryptostrategy.platform.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.auth.OwnerAuthorizationService;
import com.cryptostrategy.platform.api.error.ResourceInaccessibleException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:ownership;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=fixture-password",
            "platform.security.jwt.issuer=https://fixture.invalid/auth/v1",
            "platform.security.jwt.jwk-set-uri=https://fixture.invalid/.well-known/jwks.json",
            "platform.security.jwt.audience=authenticated"
        })
@AutoConfigureMockMvc
@Import(OwnershipIsolationIntegrationTest.FixtureConfiguration.class)
class OwnershipIsolationIntegrationTest {
    private static final UUID USER_A = UUID.fromString("d1203948-8ff9-4916-9964-fecbed13d4db");
    private static final UUID USER_B = UUID.fromString("9a3b2b5e-6e60-494d-b62e-e576e31361ad");

    private final OwnerAuthorizationService authorization = new OwnerAuthorizationService();
    private final FakePublishedOwnershipPort resources = publishedResources();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void directResourceCanOnlyBeResolvedByItsOwner() {
        String resource = authorization.requireOwned(
                USER_A, new ResourceId("strategy-a"), resources::findDirect);

        assertThat(resource).isEqualTo("strategy-a");
        assertInaccessible(() -> authorization.requireOwned(
                USER_B, new ResourceId("strategy-a"), resources::findDirect));
        assertInaccessible(() -> authorization.requireOwned(
                USER_A, new ResourceId("missing"), resources::findDirect));
    }

    @Test
    void childResourceRequiresBothOwnedParentAndMatchingParentChain() {
        String resource = authorization.requireOwnedChild(
                USER_A,
                new ParentId("experiment-a"),
                new ChildId("job-a"),
                resources::findChild);

        assertThat(resource).isEqualTo("job-a");
        assertInaccessible(() -> authorization.requireOwnedChild(
                USER_B,
                new ParentId("experiment-a"),
                new ChildId("job-a"),
                resources::findChild));
        assertInaccessible(() -> authorization.requireOwnedChild(
                USER_A,
                new ParentId("experiment-b"),
                new ChildId("job-a"),
                resources::findChild));
        assertInaccessible(() -> authorization.requireOwnedChild(
                USER_A,
                new ParentId("experiment-a"),
                new ChildId("missing"),
                resources::findChild));
    }

    @Test
    void httpBoundaryReturnsTheSameSafeEnvelopeForMissingAndCrossOwnerDirectResource()
            throws Exception {
        mockMvc.perform(get("/__ownership/direct/strategy-a")
                        .with(authenticatedAs(USER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("strategy-a"));

        String crossOwner = inaccessibleResponse(
                "/__ownership/direct/strategy-a", USER_B, "OWNER-CROSS-123");
        String missing = inaccessibleResponse(
                "/__ownership/direct/missing", USER_A, "OWNER-MISSING-123");

        assertThat(crossOwner)
                .doesNotContain("strategy-a")
                .doesNotContain(USER_A.toString())
                .doesNotContain(USER_B.toString());
        assertThat(missing)
                .doesNotContain("missing")
                .doesNotContain(USER_A.toString());
    }

    @Test
    void httpBoundaryConcealsWrongOwnerAndWrongParentForChildResource() throws Exception {
        mockMvc.perform(get("/__ownership/parents/experiment-a/children/job-a")
                        .with(authenticatedAs(USER_A)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("job-a"));

        inaccessibleResponse(
                "/__ownership/parents/experiment-a/children/job-a",
                USER_B,
                "CHILD-OWNER-123");
        inaccessibleResponse(
                "/__ownership/parents/experiment-b/children/job-a",
                USER_A,
                "CHILD-PARENT-123");
    }

    private String inaccessibleResponse(String path, UUID userId, String correlationId)
            throws Exception {
        return mockMvc.perform(get(path)
                        .with(authenticatedAs(userId))
                        .header("X-Correlation-Id", correlationId))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Correlation-Id", correlationId))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("The requested resource was not found."))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.correlationId").value(correlationId))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static RequestPostProcessor authenticatedAs(UUID userId) {
        var token = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUserContext(userId, Instant.MAX),
                "fixture",
                List.of());
        return authentication(token);
    }

    private static void assertInaccessible(Runnable lookup) {
        assertThatThrownBy(lookup::run)
                .isInstanceOf(ResourceInaccessibleException.class)
                .hasMessage("The requested resource was not found.");
    }

    private record ResourceId(String value) {}

    private record ParentId(String value) {}

    private record ChildId(String value) {}

    private record OwnedChild(UUID ownerUserId, ParentId parentId) {}

    private record FakePublishedOwnershipPort(
            Map<ResourceId, UUID> directOwners,
            Map<ChildId, OwnedChild> childOwners) {

        Optional<String> findDirect(UUID authenticatedUserId, ResourceId resourceId) {
            return Optional.ofNullable(directOwners.get(resourceId))
                    .filter(authenticatedUserId::equals)
                    .map(ignored -> resourceId.value());
        }

        Optional<String> findChild(
                UUID authenticatedUserId,
                ParentId parentId,
                ChildId childId) {
            return Optional.ofNullable(childOwners.get(childId))
                    .filter(child -> child.ownerUserId().equals(authenticatedUserId))
                    .filter(child -> child.parentId().equals(parentId))
                    .map(ignored -> childId.value());
        }
    }

    private static FakePublishedOwnershipPort publishedResources() {
        return new FakePublishedOwnershipPort(
                Map.of(new ResourceId("strategy-a"), USER_A),
                Map.of(
                        new ChildId("job-a"), new OwnedChild(USER_A, new ParentId("experiment-a")),
                        new ChildId("job-b"), new OwnedChild(USER_B, new ParentId("experiment-b"))));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixtureConfiguration {
        @Bean
        FakePublishedOwnershipPort fakePublishedOwnershipPort() {
            return publishedResources();
        }

        @Bean
        OwnershipFixtureController ownershipFixtureController(
                OwnerAuthorizationService authorization,
                FakePublishedOwnershipPort resources) {
            return new OwnershipFixtureController(authorization, resources);
        }
    }

    @RestController
    static class OwnershipFixtureController {
        private final OwnerAuthorizationService authorization;
        private final FakePublishedOwnershipPort resources;

        OwnershipFixtureController(
                OwnerAuthorizationService authorization,
                FakePublishedOwnershipPort resources) {
            this.authorization = authorization;
            this.resources = resources;
        }

        @GetMapping("/__ownership/direct/{resourceId}")
        ResourceResponse direct(
                @AuthenticationPrincipal AuthenticatedUserContext user,
                @PathVariable String resourceId) {
            return new ResourceResponse(authorization.requireOwned(
                    user.userId(), new ResourceId(resourceId), resources::findDirect));
        }

        @GetMapping("/__ownership/parents/{parentId}/children/{childId}")
        ResourceResponse child(
                @AuthenticationPrincipal AuthenticatedUserContext user,
                @PathVariable String parentId,
                @PathVariable String childId) {
            return new ResourceResponse(authorization.requireOwnedChild(
                    user.userId(),
                    new ParentId(parentId),
                    new ChildId(childId),
                    resources::findChild));
        }
    }

    private record ResourceResponse(String value) {}
}
