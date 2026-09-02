package com.cryptostrategy.platform.api.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.cryptostrategy.platform.api.auth.OwnerAuthorizationService;
import com.cryptostrategy.platform.api.auth.WebSocketTicketService;
import com.cryptostrategy.platform.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.api.idempotency.IdempotencyService;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.experiment.api.port.out.IdempotencyStore;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Test;

class FoundationalBoundaryTest {
    private static final UUID USER_A =
            UUID.fromString("d1203948-8ff9-4916-9964-fecbed13d4db");
    private static final UUID USER_B =
            UUID.fromString("9a3b2b5e-6e60-494d-b62e-e576e31361ad");
    private static final String ALLOWED_ORIGIN = "https://dashboard.example.test";

    @Test
    void websocketTicketIsOriginBoundSingleUseAndExpires() {
        WebSocketTicketService service = new WebSocketTicketService(Duration.ofMinutes(1));
        var issued = service.issue(USER_A, ALLOWED_ORIGIN);

        assertThatThrownBy(() -> service.consume(issued.ticket(), "https://evil.example.test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WebSocket ticket is invalid or expired");
        assertThat(service.consume(issued.ticket(), ALLOWED_ORIGIN).userId())
                .isEqualTo(USER_A);
        assertThatThrownBy(() -> service.consume(issued.ticket(), ALLOWED_ORIGIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WebSocket ticket is invalid or expired");

        WebSocketTicketService expiringService =
                new WebSocketTicketService(Duration.ofMillis(5));
        var expiringTicket = expiringService.issue(USER_A, ALLOWED_ORIGIN);
        awaitExpiry(expiringTicket.expiresAt());

        assertThatThrownBy(
                        () -> expiringService.consume(expiringTicket.ticket(), ALLOWED_ORIGIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("WebSocket ticket is invalid or expired");
    }

    @Test
    void ownershipLookupConcealsForeignMissingAndWrongParentResources() {
        OwnerAuthorizationService authorization = new OwnerAuthorizationService();
        UUID strategyId = UUID.fromString("0dcd4566-bba4-4f60-9e4d-1a9c8ad31a96");
        UUID missingStrategyId = UUID.fromString("a32f4e55-bd3e-4318-ac1f-932dd39bd1ba");
        UUID experimentId = UUID.fromString("b810c02d-964b-4456-a745-60c56b45ad0b");
        UUID wrongExperimentId = UUID.fromString("b7e94308-7b17-4220-8b37-78ae68b56c28");
        UUID jobId = UUID.fromString("5c92341a-7b26-4448-a064-3e95e8d5ddf6");
        OwnerAuthorizationService.OwnedResourceLookup<UUID, String> strategies =
                (userId, resourceId) -> ownedValue(
                        userId.equals(USER_A) && resourceId.equals(strategyId),
                        "strategy-a");
        OwnerAuthorizationService.ParentOwnedResourceLookup<UUID, UUID, String> jobs =
                (userId, parentId, childId) -> ownedValue(
                        userId.equals(USER_A)
                                && parentId.equals(experimentId)
                                && childId.equals(jobId),
                        "job-a");

        String ownedStrategy = authorization.requireOwned(USER_A, strategyId, strategies);
        String ownedJob = authorization.requireOwnedChild(
                USER_A, experimentId, jobId, jobs);

        assertThat(ownedStrategy).isEqualTo("strategy-a");
        assertThat(ownedJob).isEqualTo("job-a");
        assertInaccessible(() -> authorization.requireOwned(USER_B, strategyId, strategies));
        assertInaccessible(() -> authorization.requireOwned(
                USER_A, missingStrategyId, strategies));
        assertInaccessible(() -> authorization.requireOwnedChild(
                USER_A, wrongExperimentId, jobId, jobs));
    }

    @Test
    void canonicalHashIsStableButStillScopedByOwnerOperationAndPayloadMeaning() {
        IdempotencyService idempotency = new IdempotencyService(
                JsonMapper.builder().build(),
                mock(IdempotencyStore.class),
                Duration.ofHours(24));
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("strategy", new LinkedHashMap<>(Map.of("slow", 50, "fast", 20)));
        first.put("datasetId", "01JDATASET00000000000000001");
        Map<String, Object> reordered = new LinkedHashMap<>();
        reordered.put("datasetId", "01JDATASET00000000000000001");
        reordered.put("strategy", new LinkedHashMap<>(Map.of("fast", 20, "slow", 50)));

        String canonical =
                idempotency.canonicalRequestHash(USER_A, "START_BACKTEST", first);

        assertThat(idempotency.canonicalRequestHash(USER_A, "START_BACKTEST", reordered))
                .isEqualTo(canonical);
        assertThat(idempotency.canonicalRequestHash(USER_B, "START_BACKTEST", first))
                .isNotEqualTo(canonical);
        assertThat(idempotency.canonicalRequestHash(USER_A, "START_EXPERIMENT", first))
                .isNotEqualTo(canonical);
        assertThat(idempotency.canonicalRequestHash(
                        USER_A,
                        "START_BACKTEST",
                        Map.of("datasetId", "01JDATASET00000000000000002")))
                .isNotEqualTo(canonical);
    }

    @Test
    void paginationAcceptsOnlyBoundedLimitsAndOpaqueCursors() {
        PageRequestMapper pages = new PageRequestMapper();

        assertThat(pages.map(null, null))
                .isEqualTo(new PageRequestMapper.PageRequest(50, Optional.empty()));
        assertThat(pages.map(200, "next_PAGE-2").cursor())
                .contains("next_PAGE-2");
        assertThatThrownBy(() -> pages.map(0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 200");
        assertThatThrownBy(() -> pages.map(201, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit must be between 1 and 200");

        for (String malformed : List.of("", "cursor/value", "cursor=", "cửa-sổ")) {
            assertThatThrownBy(() -> pages.map(50, malformed))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("cursor is malformed");
        }
    }

    private static Optional<String> ownedValue(boolean accessible, String value) {
        return accessible ? Optional.of(value) : Optional.empty();
    }

    private static void assertInaccessible(Runnable lookup) {
        assertThatThrownBy(lookup::run)
                .isInstanceOf(ResourceInaccessibleException.class)
                .hasMessage("The requested resource was not found.");
    }

    private static void awaitExpiry(Instant expiresAt) {
        long deadline = System.nanoTime() + Duration.ofSeconds(1).toNanos();
        while (Instant.now().isBefore(expiresAt) && System.nanoTime() < deadline) {
            LockSupport.parkNanos(Duration.ofNanos(100_000).toNanos());
        }
        assertThat(Instant.now()).isAfterOrEqualTo(expiresAt);
    }
}
