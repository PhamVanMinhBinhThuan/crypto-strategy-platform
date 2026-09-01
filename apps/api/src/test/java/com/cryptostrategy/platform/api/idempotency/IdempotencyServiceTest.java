package com.cryptostrategy.platform.api.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyClaim;
import com.cryptostrategy.platform.experiment.api.idempotency.IdempotencyOutcome;
import com.cryptostrategy.platform.experiment.api.port.out.IdempotencyStore;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyServiceTest {
    private static final UUID USER_A = UUID.fromString("9d69fd8d-2942-4fb1-b981-74aa17199105");
    private static final UUID USER_B = UUID.fromString("322464d8-bc15-4f7c-9237-1749178d63bc");
    private static final Instant NOW = Instant.parse("2026-09-02T08:00:00Z");

    private final RecordingIdempotencyStore store = new RecordingIdempotencyStore();
    private final IdempotencyService service = new IdempotencyService(
            JsonMapper.builder()
                    .addModule(new Jdk8Module())
                    .addModule(new JavaTimeModule())
                    .build(),
            store,
            Duration.ofHours(24),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void canonicalHashIsStableAcrossPropertyAndNestedMapOrder() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("strategy", new LinkedHashMap<>(Map.of("slow", 50, "fast", 20)));
        first.put("datasetId", "01JDATASET00000000000000001");
        first.put("symbols", List.of("BTC/USDT", "ETH/USDT"));

        Map<String, Object> second = new LinkedHashMap<>();
        second.put("symbols", List.of("BTC/USDT", "ETH/USDT"));
        second.put("datasetId", "01JDATASET00000000000000001");
        second.put("strategy", new LinkedHashMap<>(Map.of("fast", 20, "slow", 50)));

        String firstHash = service.canonicalRequestHash(USER_A, "START_BACKTEST", first);
        String secondHash = service.canonicalRequestHash(USER_A, "START_BACKTEST", second);

        assertThat(firstHash)
                .isEqualTo(secondHash)
                .startsWith("sha256:")
                .hasSize(71);
    }

    @Test
    void hashScopeIncludesAuthenticatedOwnerAndOperationWhileArrayOrderRemainsMeaningful() {
        Map<String, Object> request = Map.of("symbols", List.of("BTC/USDT", "ETH/USDT"));

        String baseline = service.canonicalRequestHash(USER_A, "START_BACKTEST", request);

        assertThat(service.canonicalRequestHash(USER_B, "START_BACKTEST", request))
                .isNotEqualTo(baseline);
        assertThat(service.canonicalRequestHash(USER_A, "START_EXPERIMENT", request))
                .isNotEqualTo(baseline);
        assertThat(service.canonicalRequestHash(
                        USER_A,
                        "START_BACKTEST",
                        Map.of("symbols", List.of("ETH/USDT", "BTC/USDT"))))
                .isNotEqualTo(baseline);
    }

    @Test
    void claimUsesCanonicalHashOwnerOperationKeyAndConfiguredExpiry() {
        IdempotencyClaim result = service.claim(
                USER_A,
                "START_BACKTEST",
                "backtest-001",
                Map.of("datasetId", "01JDATASET00000000000000001"));

        assertThat(result.status().name()).isEqualTo("ACQUIRED");
        assertThat(store.ownerUserId).isEqualTo(USER_A);
        assertThat(store.operation).isEqualTo("START_BACKTEST");
        assertThat(store.idempotencyKey).isEqualTo("backtest-001");
        assertThat(store.requestHash).startsWith("sha256:");
        assertThat(store.expiresAt).isEqualTo(NOW.plus(Duration.ofHours(24)));
    }

    @Test
    void completionAndOutcomeLookupDelegateToTheF005Port() {
        service.complete(USER_A, "START_BACKTEST", "backtest-001", "202", "{\"jobId\":\"job-1\"}");

        Optional<IdempotencyOutcome> outcome = service.getOutcome(
                USER_A, "START_BACKTEST", "backtest-001");

        assertThat(store.completedOutcomeCode).isEqualTo("202");
        assertThat(store.completedResponseBody).isEqualTo("{\"jobId\":\"job-1\"}");
        assertThat(outcome).contains(store.outcome);
    }

    @Test
    void blankOrOversizedKeysAndBlankOperationsAreRejectedBeforePersistence() {
        assertThatThrownBy(() -> service.claim(USER_A, "START_BACKTEST", " ", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency-Key must contain between 1 and 255 characters");
        assertThatThrownBy(() -> service.claim(USER_A, "START_BACKTEST", "k".repeat(256), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency-Key must contain between 1 and 255 characters");
        assertThatThrownBy(() -> service.claim(USER_A, " ", "key-1", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency operation is required");
        assertThat(store.claimCount).isZero();
    }

    private static final class RecordingIdempotencyStore implements IdempotencyStore {
        private final IdempotencyOutcome outcome = new IdempotencyOutcome("202", "{}", NOW);
        private UUID ownerUserId;
        private String operation;
        private String idempotencyKey;
        private String requestHash;
        private Instant expiresAt;
        private String completedOutcomeCode;
        private String completedResponseBody;
        private int claimCount;

        @Override
        public IdempotencyClaim claim(
                UUID ownerUserId,
                String scope,
                String idempotencyKey,
                String requestHash,
                Instant expiresAt) {
            claimCount++;
            this.ownerUserId = ownerUserId;
            this.operation = scope;
            this.idempotencyKey = idempotencyKey;
            this.requestHash = requestHash;
            this.expiresAt = expiresAt;
            return IdempotencyClaim.acquired();
        }

        @Override
        public void complete(
                UUID ownerUserId,
                String scope,
                String idempotencyKey,
                String outcomeCode,
                String responseBody) {
            this.completedOutcomeCode = outcomeCode;
            this.completedResponseBody = responseBody;
        }

        @Override
        public Optional<IdempotencyOutcome> getOutcome(
                UUID ownerUserId,
                String scope,
                String idempotencyKey) {
            return Optional.of(outcome);
        }
    }
}
