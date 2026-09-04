package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.execution.api.ReproductionVerificationId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.Trade;
import com.cryptostrategy.platform.evaluation.api.model.EvaluationResult;
import com.cryptostrategy.platform.execution.api.ExecutionEvidence;
import com.cryptostrategy.platform.execution.api.port.out.ExecutionEvidenceReader;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionVerificationGateway;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardRevision;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SearchReproductionVerificationTest {
    private static final ExperimentId SOURCE = new ExperimentId("65000000000000000000000001");
    private static final ExperimentId TARGET = new ExperimentId("65000000000000000000000002");
    private static final UUID OWNER = UUID.fromString("95000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-03T04:00:00Z");

    @Test
    void exactOrderedTradesCanonicalMetricsAndFingerprintsMatch() {
        Fixture fixture = fixture(evidence("same", "0.10"), evidence("same", "0.10"));
        assertThat(fixture.coordinator.verify(TARGET))
                .isEqualTo(SearchReproductionVerificationCoordinator.Result.MATCHED);
        var captured = ArgumentCaptor.forClass(SearchReproductionVerificationGateway.Completion.class);
        org.mockito.Mockito.verify(fixture.gateway).complete(captured.capture());
        assertThat(captured.getValue().status()).isEqualTo("MATCHED");
        assertThat(captured.getValue().safeDifferences()).isEmpty();
    }

    @Test
    void changedTradeOrderOrExactMetricProducesBoundedMismatch() {
        Fixture fixture = fixture(evidence("source", "0.10"), evidence("different", "0.1001"));
        assertThat(fixture.coordinator.verify(TARGET))
                .isEqualTo(SearchReproductionVerificationCoordinator.Result.MISMATCHED);
        var captured = ArgumentCaptor.forClass(SearchReproductionVerificationGateway.Completion.class);
        org.mockito.Mockito.verify(fixture.gateway).complete(captured.capture());
        assertThat(captured.getValue().safeDifferences()).containsOnlyKeys(
                "tradeSequence", "metrics", "fingerprints");
        assertThat(captured.getValue().safeDifferences().toString()).doesNotContain("Exception", "\\", "sql");
    }

    @Test
    void duplicateTerminalTriggerAfterCompletionIsIdempotent() {
        Fixture fixture = fixture(evidence("same", "0.10"), evidence("same", "0.10"));
        when(fixture.gateway.claimReady(TARGET, NOW))
                .thenReturn(work()).thenReturn(Optional.empty());
        assertThat(fixture.coordinator.verify(TARGET))
                .isEqualTo(SearchReproductionVerificationCoordinator.Result.MATCHED);
        assertThat(fixture.coordinator.verify(TARGET))
                .isEqualTo(SearchReproductionVerificationCoordinator.Result.NOT_READY_OR_ALREADY_TERMINAL);
    }

    private static Fixture fixture(ExecutionEvidence source, ExecutionEvidence target) {
        var gateway = mock(SearchReproductionVerificationGateway.class);
        var reader = mock(ExecutionEvidenceReader.class);
        when(gateway.claimReady(TARGET, NOW)).thenReturn(work());
        when(gateway.complete(any())).thenReturn(true);
        when(reader.load(OWNER, SOURCE)).thenReturn(source);
        when(reader.load(OWNER, TARGET)).thenReturn(target);
        return new Fixture(gateway, new SearchReproductionVerificationCoordinator(gateway, reader,
                Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    private static Optional<SearchReproductionVerificationGateway.Work> work() {
        return Optional.of(new SearchReproductionVerificationGateway.Work(
                new ReproductionVerificationId("65000000000000000000000003"), 1, OWNER, SOURCE, TARGET));
    }

    private static ExecutionEvidence evidence(String fingerprint, String totalReturn) {
        BacktestResult backtest = mock(BacktestResult.class);
        EvaluationResult evaluation = mock(EvaluationResult.class);
        LeaderboardRevision leaderboard = mock(LeaderboardRevision.class);
        Trade trade = mock(Trade.class);
        when(trade.sequence()).thenReturn(0);
        when(trade.entryTime()).thenReturn(Instant.ofEpochSecond(Integer.toUnsignedLong(fingerprint.hashCode())));
        when(backtest.trades()).thenReturn(List.of(trade));
        when(backtest.fingerprint()).thenReturn(fingerprint);
        when(evaluation.totalReturn()).thenReturn(new BigDecimal(totalReturn));
        when(evaluation.winRate()).thenReturn(new BigDecimal("0.50"));
        when(evaluation.maximumDrawdown()).thenReturn(new BigDecimal("0.05"));
        when(evaluation.numberOfTrades()).thenReturn(1);
        when(evaluation.fingerprint()).thenReturn(fingerprint);
        when(leaderboard.fingerprint()).thenReturn(fingerprint);
        return new ExecutionEvidence(backtest, evaluation, leaderboard);
    }

    private record Fixture(SearchReproductionVerificationGateway gateway,
            SearchReproductionVerificationCoordinator coordinator) {}
}
