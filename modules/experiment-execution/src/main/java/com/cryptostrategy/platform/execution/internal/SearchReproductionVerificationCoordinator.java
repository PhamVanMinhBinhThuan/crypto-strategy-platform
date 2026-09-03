package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.backtesting.api.model.Trade;
import com.cryptostrategy.platform.execution.api.ExecutionEvidence;
import com.cryptostrategy.platform.execution.api.port.out.ExecutionEvidenceReader;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionVerificationGateway;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Terminal-trigger/reconciler idempotent; comparator chạy ngoài transaction claim. */
public final class SearchReproductionVerificationCoordinator {
    private final SearchReproductionVerificationGateway gateway;
    private final ExecutionEvidenceReader evidence;
    private final Clock clock;

    public SearchReproductionVerificationCoordinator(SearchReproductionVerificationGateway gateway,
            ExecutionEvidenceReader evidence, Clock clock) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.evidence = Objects.requireNonNull(evidence, "evidence");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Result verify(ExperimentId reproductionExperimentId) {
        var work = gateway.claimReady(reproductionExperimentId, clock.instant());
        if (work.isEmpty()) return Result.NOT_READY_OR_ALREADY_TERMINAL;
        var claimed = work.orElseThrow();
        try {
            ExecutionEvidence source = evidence.load(claimed.ownerUserId(), claimed.sourceExperimentId());
            ExecutionEvidence reproduced = evidence.load(claimed.ownerUserId(), claimed.reproductionExperimentId());
            Comparison comparison = compare(source, reproduced);
            String status = comparison.matches() ? "MATCHED" : "MISMATCHED";
            boolean committed = gateway.complete(new SearchReproductionVerificationGateway.Completion(
                    claimed.verificationId(), claimed.version(), status, comparison.tradesMatched(),
                    comparison.metricsMatched(), comparison.fingerprintsMatched(), fingerprint(source),
                    fingerprint(reproduced), comparison.differences(), clock.instant(), null, null));
            if (!committed) return Result.STALE;
            return comparison.matches() ? Result.MATCHED : Result.MISMATCHED;
        } catch (RuntimeException failure) {
            gateway.complete(new SearchReproductionVerificationGateway.Completion(claimed.verificationId(),
                    claimed.version(), "FAILED", false, false, false, null, null, Map.of(), clock.instant(),
                    "VERIFICATION_FAILED", "Reproduction evidence could not be verified"));
            return Result.FAILED;
        }
    }

    public int reconcile(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        int processed = 0;
        for (ExperimentId id : gateway.findReady(limit)) {
            Result result = verify(id);
            if (result != Result.NOT_READY_OR_ALREADY_TERMINAL && result != Result.STALE) processed++;
        }
        return processed;
    }

    static Comparison compare(ExecutionEvidence source, ExecutionEvidence reproduced) {
        List<String> sourceTrades = source.backtest().trades().stream().map(SearchReproductionVerificationCoordinator::trade).toList();
        List<String> reproducedTrades = reproduced.backtest().trades().stream().map(SearchReproductionVerificationCoordinator::trade).toList();
        boolean trades = sourceTrades.equals(reproducedTrades);
        boolean metrics = List.of(source.evaluation().totalReturn(), source.evaluation().winRate(),
                source.evaluation().maximumDrawdown(), source.evaluation().numberOfTrades()).equals(List.of(
                reproduced.evaluation().totalReturn(), reproduced.evaluation().winRate(),
                reproduced.evaluation().maximumDrawdown(), reproduced.evaluation().numberOfTrades()));
        boolean fingerprints = source.backtest().fingerprint().equals(reproduced.backtest().fingerprint())
                && source.evaluation().fingerprint().equals(reproduced.evaluation().fingerprint())
                && source.leaderboard().fingerprint().equals(reproduced.leaderboard().fingerprint());
        Map<String, Object> differences = new LinkedHashMap<>();
        if (!trades) differences.put("tradeSequence", Map.of("sourceCount", sourceTrades.size(),
                "reproductionCount", reproducedTrades.size()));
        if (!metrics) differences.put("metrics", "canonical metric tuple differs");
        if (!fingerprints) differences.put("fingerprints", "evidence fingerprints differ");
        return new Comparison(trades, metrics, fingerprints, Map.copyOf(differences));
    }

    private static String trade(Trade value) {
        return value.sequence() + "|" + value.entryTime() + "|" + value.exitTime() + "|"
                + value.entryPrice() + "|" + value.exitPrice() + "|" + value.quantity() + "|"
                + value.totalFee() + "|" + value.realizedPnl();
    }

    private static String fingerprint(ExecutionEvidence value) {
        String canonical = value.backtest().fingerprint() + "|" + value.evaluation().fingerprint()
                + "|" + value.leaderboard().fingerprint();
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }

    record Comparison(boolean tradesMatched, boolean metricsMatched, boolean fingerprintsMatched,
            Map<String, Object> differences) {
        boolean matches() { return tradesMatched && metricsMatched && fingerprintsMatched; }
    }

    public enum Result { MATCHED, MISMATCHED, FAILED, STALE, NOT_READY_OR_ALREADY_TERMINAL }
}
