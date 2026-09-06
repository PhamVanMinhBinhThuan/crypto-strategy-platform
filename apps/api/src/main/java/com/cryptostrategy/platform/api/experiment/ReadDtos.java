package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentSummary;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.api.transport.TypedUlidSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.cryptostrategy.platform.execution.api.port.in.GetSearchProgressUseCase.SearchProgressSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.leaderboard.api.model.CandidatePipelineEntry;

public final class ReadDtos {
    private ReadDtos() {}

    public record ExperimentResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId experimentId,
            String name,
            String status,
            @JsonSerialize(using = TypedUlidSerializer.class) DatasetVersionId datasetId,
            DatasetSummaryResponse dataset,
            @JsonSerialize(contentUsing = TypedUlidSerializer.class) List<JobId> jobIds,
            JobResponse searchJob,
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId derivedFromExperimentId,
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId reproducesExperimentId,
            Instant startedAt,
            Instant completedAt,
            FailureResponse failure,
            SearchProgressResponse searchProgress,
            Instant createdAt) {
        static ExperimentResponse from(
                Experiment experiment, ExperimentManifest manifest, List<Job> jobs) {
            return from(experiment, manifest, jobs, null);
        }

        static ExperimentResponse from(
                Experiment experiment, ExperimentManifest manifest, List<Job> jobs,
                SearchProgressSnapshot run) {
            FailureResponse failure = experiment.failureCode() == null
                    ? null
                    : new FailureResponse(experiment.failureCode(), experiment.failureMessage());
            Job search = jobs.stream().filter(job -> job.jobType().name().equals("SEARCH"))
                    .findFirst().orElse(null);
            long allocated = jobs.stream().filter(job -> job.jobType().name().equals("BACKTEST")).count();
            int configuredMaximum = integer(manifest.searchConfig(), "maximumCandidates",
                    search == null ? 0 : search.totalWork());
            SearchProgressResponse progress = search == null ? null : new SearchProgressResponse(
                    Math.toIntExact(allocated),
                    Math.max(0, Math.toIntExact(allocated) - search.completedWork() - search.failedWork()),
                    search.completedWork(), search.failedWork(),
                    Math.max(0, configuredMaximum - Math.toIntExact(allocated)),
                    configuredMaximum, integer(manifest.searchConfig(), "topK", 10),
                    decimal(search.bestScore()), search.startedAt(),
                    terminalReason(experiment, allocated, configuredMaximum, search, run));
            return new ExperimentResponse(
                    experiment.experimentId(),
                    experiment.name(),
                    experiment.status().name(),
                    manifest.datasetProvenance().datasetVersionId(),
                    DatasetSummaryResponse.from(manifest.datasetProvenance()),
                    jobs.stream().map(Job::jobId).toList(),
                    search == null ? null : JobResponse.from(search),
                    experiment.derivedFromExperimentId(),
                    experiment.reproducesExperimentId(),
                    experiment.startedAt(),
                    experiment.completedAt(),
                    failure,
                    progress,
                    experiment.createdAt());
        }
    }

    public record SearchProgressResponse(int allocated, int active, int completed, int failed,
            int remainingCapacity, int configuredMaximum, int topK, String bestScore,
            Instant startedAt, String terminalReason) {}

    public record ExperimentHistoryPage(
            List<ExperimentHistoryItem> items,
            String nextCursor,
            boolean hasMore,
            long totalCount) {}

    public record ExperimentHistoryItem(
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId experimentId,
            String name,
            String status,
            HistoryDatasetResponse dataset,
            HistoryProgressResponse progress,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
        static ExperimentHistoryItem from(ExperimentSummary value) {
            return new ExperimentHistoryItem(
                    value.experimentId(), value.name(), value.status().name(),
                    new HistoryDatasetResponse(value.datasetProvider(), value.datasetPair(),
                            value.datasetTimeframe(), value.candleCount()),
                    new HistoryProgressResponse(value.processedCandidates(), value.totalCandidates(),
                            value.succeededCandidates(), value.failedCandidates()),
                    value.startedAt(), value.completedAt(), value.createdAt());
        }
    }

    public record HistoryDatasetResponse(
            String provider, String pair, String timeframe, long candleCount) {}

    public record HistoryProgressResponse(
            int processed, int total, int succeeded, int failed) {}

    public record FailureResponse(String code, String message) {}

    public record DatasetSummaryResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) DatasetVersionId datasetId,
            String provider, String pair, String timeframe, Instant startTime, Instant endTime,
            long candleCount, String checksum, String normalizationVersion) {
        static DatasetSummaryResponse from(DatasetProvenanceSnapshot value) {
            return new DatasetSummaryResponse(value.datasetVersionId(), value.provider(),
                    value.tradingPair(), value.timeframe(), value.rangeStart(), value.rangeEnd(),
                    value.candleCount(), value.checksum(), value.normalizationVersion());
        }
    }

    public record JobResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) JobId jobId,
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId experimentId,
            @JsonSerialize(using = TypedUlidSerializer.class) CandidateId candidateId,
            String type,
            String status,
            int totalWork,
            int completedWork,
            int failedWork,
            String bestScore,
            Instant queuedAt,
            Instant startedAt,
            Instant finishedAt,
            Instant nextRetryAt,
            FailureResponse failure,
            Instant createdAt,
            Instant updatedAt) {
        static JobResponse from(Job job) {
            FailureResponse failure = job.failureCode() == null
                    ? null
                    : new FailureResponse(job.failureCode(), job.failureMessage());
            return new JobResponse(
                    job.jobId(),
                    job.experimentId(),
                    job.candidateId(),
                    job.jobType().name(),
                    job.status().name(),
                    job.totalWork(),
                    job.completedWork(),
                    job.failedWork(),
                    decimal(job.bestScore()),
                    job.queuedAt(),
                    job.startedAt(),
                    job.finishedAt(),
                    job.nextRetryAt(),
                    failure,
                    job.createdAt(),
                    job.updatedAt());
        }
    }

    public record CandidateResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) CandidateId candidateId,
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId experimentId,
            int generationIndex,
            Map<String, Object> definition,
            Map<String, Object> generatorState,
            String fingerprint,
            Instant createdAt) {
        static CandidateResponse from(CandidateDefinition candidate) {
            return new CandidateResponse(
                    candidate.candidateId(),
                    candidate.experimentId(),
                    candidate.generationIndex(),
                    candidate.definition(),
                    candidate.generatorState(),
                    candidate.fingerprint(),
                    candidate.createdAt());
        }
    }

    public record CandidatePage(
            List<CandidateResponse> items, String nextCursor, boolean hasMore) {}

    public record CandidatePipelinePage(List<CandidatePipelineResponse> items, String nextCursor,
            boolean hasMore, int resultCount, int failedCount, int totalCount) {}

    public record CandidatePipelineResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) CandidateId candidateId,
            int generationIndex, Map<String, Object> definition, String candidateSummary,
            String candidateFingerprint, Instant createdAt, PipelineBacktestResponse backtest,
            PipelineEvaluationResponse evaluation, PipelineRankingResponse ranking,
            String failureStage) {
        static CandidatePipelineResponse from(CandidatePipelineEntry value) {
            return new CandidatePipelineResponse(value.candidateId(), value.generationIndex(),
                    value.definition(), ReadDtos.candidateSummary(value.definition()),
                    value.candidateFingerprint(), value.createdAt(),
                    PipelineBacktestResponse.from(value.backtest()),
                    PipelineEvaluationResponse.from(value.evaluation()),
                    PipelineRankingResponse.from(value.ranking()), value.failureStage());
        }
    }

    public record PipelineBacktestResponse(String jobId, String status, String backtestResultId,
            Instant startedAt, Instant finishedAt, Integer attemptNo, Instant nextRetryAt,
            boolean retryable, FailureResponse failure) {
        public static PipelineBacktestResponse from(CandidatePipelineEntry.BacktestStage value) {
            return new PipelineBacktestResponse(value.jobId(), value.status(),
                    value.backtestResultId(), value.startedAt(), value.finishedAt(),
                    value.attemptNo(), value.nextRetryAt(), value.retryable(),
                    value.failure() == null ? null
                            : new FailureResponse(value.failure().code(), value.failure().message()));
        }
    }

    public record PipelineEvaluationResponse(String status, String evaluationResultId, String score,
            String totalReturn, String winRate, String maximumDrawdown, Integer numberOfTrades,
            String metricVersion, Boolean eligible, EligibilityReasonResponse eligibilityReason,
            Instant evaluatedAt) {
        public static PipelineEvaluationResponse from(CandidatePipelineEntry.EvaluationStage value) {
            return new PipelineEvaluationResponse(value.status(), value.evaluationResultId(),
                    decimal(value.score()), decimal(value.totalReturn()), decimal(value.winRate()),
                    decimal(value.maximumDrawdown()), value.numberOfTrades(), value.metricVersion(),
                    value.eligible(), EligibilityReasonResponse.from(value.eligibilityReason()),
                    value.evaluatedAt());
        }
    }

    public record PipelineRankingResponse(String status, Integer rank, String rankingVersion) {
        public static PipelineRankingResponse from(CandidatePipelineEntry.RankingStage value) {
            return new PipelineRankingResponse(value.status(), value.rank(), value.rankingVersion());
        }
    }

    public record EligibilityReasonResponse(
            String code, Integer actualTrades, Integer requiredTrades) {
        static EligibilityReasonResponse from(CandidatePipelineEntry.EligibilityReason value) {
            return value == null ? null : new EligibilityReasonResponse(
                    value.code(), value.actualTrades(), value.requiredTrades());
        }
    }

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    @SuppressWarnings("unchecked")
    private static String candidateSummary(Map<String, Object> definition) {
        Object components = definition.get("components");
        if (components instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(Map.class::cast)
                    .map(item -> String.valueOf(item.getOrDefault("strategyId", "Strategy")))
                    .collect(java.util.stream.Collectors.joining(" + "));
        }
        return String.valueOf(definition.getOrDefault("strategyId", "Strategy"));
    }

    private static int integer(Map<String, Object> values, String name, int fallback) {
        Object value = values.get(name);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static String terminalReason(Experiment experiment, long allocated,
            int configuredMaximum, Job search, SearchProgressSnapshot run) {
        if (run != null && run.terminalReason() != null) return run.terminalReason();
        if (experiment.failureCode() != null) return experiment.failureCode();
        return switch (experiment.status().name()) {
            case "STOPPED" -> "EXPLICIT_STOP";
            case "COMPLETED" -> allocated < configuredMaximum
                    ? "SEARCH_SPACE_EXHAUSTED" : "MAXIMUM_CANDIDATES";
            default -> null;
        };
    }
}
