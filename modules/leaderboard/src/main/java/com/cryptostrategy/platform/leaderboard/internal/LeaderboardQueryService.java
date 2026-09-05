package com.cryptostrategy.platform.leaderboard.internal;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardSnapshot;
import com.cryptostrategy.platform.leaderboard.api.model.LeaderboardBacktestResultId;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.out.LeaderboardStore;
import java.util.Objects;
import java.util.Optional;

public final class LeaderboardQueryService implements GetLeaderboardUseCase {
    private final LeaderboardStore store;

    public LeaderboardQueryService(LeaderboardStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override
    public Optional<LeaderboardSnapshot> getLatest(ExperimentId experimentId) {
        return store.latest(Objects.requireNonNull(experimentId, "experimentId"))
                .map(revision -> new LeaderboardSnapshot(
                        revision.revisionId(),
                        revision.experimentId(),
                        revision.revisionNumber(),
                        revision.topK(),
                        revision.rankingVersion(),
                        revision.entries().stream().map(entry -> {
                            var evaluation = store.findEvaluation(entry.evaluationResultId())
                                    .orElseThrow(() -> new IllegalStateException(
                                            "Leaderboard entry has no evaluation evidence"));
                            var evidence = store.findCandidateEvidence(
                                    revision.experimentId(), entry.evaluationResultId()).orElse(null);
                            return new LeaderboardSnapshot.Entry(
                                    entry.rank(),
                                    entry.evaluationResultId(),
                                    new LeaderboardBacktestResultId(evaluation.backtestResultValue()),
                                    entry.score(),
                                    entry.maximumDrawdown(),
                                    entry.evaluationFingerprint(),
                                    evidence == null ? null : evidence.candidateId(),
                                    evidence == null ? null : evidence.candidateFingerprint(),
                                    evidence == null ? java.util.Map.of() : evidence.definition(),
                                    evaluation.totalReturn(), evaluation.winRate(),
                                    evaluation.numberOfTrades(), evaluation.metricVersion().value());
                        }).toList(),
                        revision.fingerprint(),
                        revision.createdAt()));
    }

    @Override
    public Optional<com.cryptostrategy.platform.leaderboard.api.model.LeaderboardCandidateEvidence>
            getCandidate(ExperimentId experimentId, CandidateId candidateId) {
        Objects.requireNonNull(experimentId, "experimentId");
        Objects.requireNonNull(candidateId, "candidateId");
        return store.findCandidateEvidence(experimentId, candidateId);
    }
}
