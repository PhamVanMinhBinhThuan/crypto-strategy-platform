package com.cryptostrategy.platform.experiment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktest;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktestAcceptance;
import com.cryptostrategy.platform.experiment.api.backtest.StartStandaloneBacktestCommand;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.port.out.StandaloneBacktestStore;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StandaloneBacktestServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-02T09:00:00Z");
    private static final UUID USER_A =
            UUID.fromString("d1203948-8ff9-4916-9964-fecbed13d4db");
    private static final UUID USER_B =
            UUID.fromString("9a3b2b5e-6e60-494d-b62e-e576e31361ad");
    private static final String HASH_A = "sha256:" + "1".repeat(64);
    private static final String HASH_B = "sha256:" + "2".repeat(64);

    private final RecordingAtomicStore store = new RecordingAtomicStore();
    private final StandaloneBacktestService service = new StandaloneBacktestService(
            store,
            new CanonicalFingerprintCalculator(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void acceptsOneFrozenSingleRunGraphWithDistinctPublicIdentity() {
        StandaloneBacktestAcceptance acceptance = service.startStandaloneBacktest(
                USER_A, command("backtest-key", HASH_A));

        assertThat(acceptance.acceptedStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(acceptance.replayed()).isFalse();
        assertThat(acceptance.backtest().backtestId().value()).isNotEqualTo(
                acceptance.backtest().experimentId().value());
        assertThat(acceptance.backtest().backtestId().value()).isNotEqualTo(
                store.candidate.candidateId().value());
        assertThat(acceptance.backtest().backtestId().value()).isNotEqualTo(
                acceptance.jobId().value());
        assertThat(store.experiment.ownerUserId()).isEqualTo(USER_A);
        assertThat(store.experiment.status().name()).isEqualTo("QUEUED");
        assertThat(store.manifest.fingerprint()).startsWith("sha256:");
        assertThat(store.manifest.searchConfig()).containsEntry("mode", "SINGLE_BACKTEST");
        assertThat(store.candidate.generationIndex()).isZero();
        assertThat(store.candidate.fingerprint())
                .isEqualTo(store.manifest.strategyProvenance().strategyFingerprint());
        assertThat(store.job.candidateId()).isEqualTo(store.candidate.candidateId());
        assertThat(store.outbox.payloadJson())
                .contains(store.job.jobId().value(), store.candidate.candidateId().value());
    }

    @Test
    void replaysOneLogicalOutcomeOneHundredTimesAndRejectsChangedPayload() {
        StandaloneBacktestAcceptance original = service.startStandaloneBacktest(
                USER_A, command("same-key", HASH_A));

        for (int replay = 0; replay < 99; replay++) {
            StandaloneBacktestAcceptance repeated = service.startStandaloneBacktest(
                    USER_A, command("same-key", HASH_A));
            assertThat(repeated.backtest()).isEqualTo(original.backtest());
            assertThat(repeated.jobId()).isEqualTo(original.jobId());
            assertThat(repeated.replayed()).isTrue();
        }

        assertThat(store.logicalOutcomes).isEqualTo(1);
        assertThatThrownBy(() -> service.startStandaloneBacktest(
                        USER_A, command("same-key", HASH_B)))
                .isInstanceOf(IdempotencyConflictException.class);
        assertThat(store.logicalOutcomes).isEqualTo(1);
    }

    @Test
    void scopesTheSameIdempotencyKeyByOwner() {
        var userA = service.startStandaloneBacktest(USER_A, command("shared-key", HASH_A));
        var userB = service.startStandaloneBacktest(USER_B, command("shared-key", HASH_A));

        assertThat(userA.backtest().backtestId()).isNotEqualTo(userB.backtest().backtestId());
        assertThat(store.logicalOutcomes).isEqualTo(2);
    }

    private static StartStandaloneBacktestCommand command(String key, String hash) {
        return new StartStandaloneBacktestCommand(
                key,
                hash,
                new DatasetProvenanceSnapshot(
                        new DatasetVersionId("01J00000000000000000000001"),
                        "candle-v1",
                        "sha256:" + "a".repeat(64),
                        "BINANCE",
                        "BTC/USDT",
                        "5m",
                        "binance-v1",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-02-01T00:00:00Z"),
                        8_928),
                ProvenanceTestFixtures.single(
                        "ma-crossover", "1.0.0", Map.of("fastPeriod", 20, "slowPeriod", 50), null),
                Map.of(
                        "assumptionsVersion", "backtest-assumptions-v1",
                        "initialCapital", "10000",
                        "feeRate", "0.001",
                        "slippageRate", "0",
                        "executionPriceRule", "NEXT_CANDLE_OPEN",
                        "positionMode", "LONG_ONLY",
                        "forceCloseAtEnd", true,
                        "roundingMode", "HALF_EVEN"),
                Map.of("metricVersion", "metric-v1", "rankingVersion", "ranking-v1"),
                "test",
                "abcdef1",
                "F009-BACKTEST-START");
    }

    private static final class RecordingAtomicStore implements StandaloneBacktestStore {
        private final Map<String, Entry> receipts = new HashMap<>();
        private int logicalOutcomes;
        private Experiment experiment;
        private ExperimentManifest manifest;
        private CandidateDefinition candidate;
        private Job job;
        private OutboxEvent outbox;

        @Override
        public StandaloneBacktestAcceptance accept(
                UUID ownerUserId,
                String operation,
                String idempotencyKey,
                String requestHash,
                Instant receiptExpiresAt,
                StandaloneBacktest backtest,
                Experiment experiment,
                ExperimentManifest manifest,
                CandidateDefinition candidate,
                Job job,
                OutboxEvent outboxEvent) {
            String receiptKey = ownerUserId + ":" + operation + ":" + idempotencyKey;
            Entry existing = receipts.get(receiptKey);
            if (existing != null) {
                if (!existing.requestHash.equals(requestHash)) {
                    throw new IdempotencyConflictException("conflicting payload");
                }
                var original = existing.acceptance;
                return new StandaloneBacktestAcceptance(
                        original.backtest(), original.jobId(), original.acceptedStatus(), true);
            }
            this.experiment = experiment;
            this.manifest = manifest;
            this.candidate = candidate;
            this.job = job;
            this.outbox = outboxEvent;
            logicalOutcomes++;
            var accepted = new StandaloneBacktestAcceptance(
                    backtest, job.jobId(), job.status(), false);
            receipts.put(receiptKey, new Entry(requestHash, accepted));
            return accepted;
        }

        private record Entry(
                String requestHash, StandaloneBacktestAcceptance acceptance) {}
    }
}
