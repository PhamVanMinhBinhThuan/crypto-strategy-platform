package com.cryptostrategy.platform.api.realtime;

import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Authorizes snapshot refresh targets and creates activation-scoped markers. */
@Component
public final class SnapshotCoordinator {
    private final GetExperimentUseCase experiments;
    private final GetLeaderboardUseCase leaderboards;

    public SnapshotCoordinator(
            GetExperimentUseCase experiments,
            GetLeaderboardUseCase leaderboards) {
        this.experiments = Objects.requireNonNull(experiments, "experiments");
        this.leaderboards = Objects.requireNonNull(leaderboards, "leaderboards");
    }

    Map<String, Object> authorizeExperiment(UUID ownerUserId, String experimentId) {
        ExperimentId id = parse(experimentId);
        if (experiments.getExperiment(ownerUserId, id).isEmpty()) {
            throw inaccessible("EXPERIMENT_NOT_FOUND");
        }
        return Map.of(
                "syncMarker", Ulids.generate(),
                "snapshotUrl", "/api/v1/experiments/" + experimentId);
    }

    Map<String, Object> authorizeLeaderboard(UUID ownerUserId, String experimentId) {
        ExperimentId id = parse(experimentId);
        if (experiments.getExperiment(ownerUserId, id).isEmpty()
                || leaderboards.getLatest(id).isEmpty()) {
            throw inaccessible("LEADERBOARD_NOT_FOUND");
        }
        return Map.of(
                "syncMarker", Ulids.generate(),
                "snapshotUrl", "/api/v1/experiments/" + experimentId + "/leaderboard");
    }

    Map<String, Object> candleRecovery(String pair, String timeframe) {
        String safePair = required(pair, "pair");
        String safeTimeframe = required(timeframe, "timeframe");
        return Map.of(
                "syncMarker", Ulids.generate(),
                "snapshotUrl", "/api/v1/candles",
                "snapshotQuery", Map.of(
                        "pair", safePair,
                        "timeframe", safeTimeframe),
                "freshness", "STALE_UNTIL_RECONCILED",
                "reconciliation", "REST_BACKFILL_THEN_REALTIME");
    }

    private static ExperimentId parse(String value) {
        try {
            return new ExperimentId(value);
        } catch (RuntimeException exception) {
            throw inaccessible("EXPERIMENT_NOT_FOUND");
        }
    }

    private static RealtimeProtocolException inaccessible(String code) {
        return new RealtimeProtocolException(code, "The requested resource was not found", false);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new RealtimeProtocolException(
                    "INVALID_MARKET_QUERY", "The requested market subscription is invalid", false);
        }
        return value.trim();
    }
}
