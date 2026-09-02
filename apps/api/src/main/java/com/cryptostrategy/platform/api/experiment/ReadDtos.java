package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.api.transport.TypedUlidSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ReadDtos {
    private ReadDtos() {}

    public record ExperimentResponse(
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId experimentId,
            String name,
            String status,
            @JsonSerialize(using = TypedUlidSerializer.class) DatasetVersionId datasetId,
            @JsonSerialize(contentUsing = TypedUlidSerializer.class) List<JobId> jobIds,
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId derivedFromExperimentId,
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId reproducesExperimentId,
            Instant startedAt,
            Instant completedAt,
            FailureResponse failure,
            Instant createdAt) {
        static ExperimentResponse from(
                Experiment experiment, ExperimentManifest manifest, List<Job> jobs) {
            FailureResponse failure = experiment.failureCode() == null
                    ? null
                    : new FailureResponse(experiment.failureCode(), experiment.failureMessage());
            return new ExperimentResponse(
                    experiment.experimentId(),
                    experiment.name(),
                    experiment.status().name(),
                    manifest.datasetProvenance().datasetVersionId(),
                    jobs.stream().map(Job::jobId).toList(),
                    experiment.derivedFromExperimentId(),
                    experiment.reproducesExperimentId(),
                    experiment.startedAt(),
                    experiment.completedAt(),
                    failure,
                    experiment.createdAt());
        }
    }

    public record FailureResponse(String code, String message) {}

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

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
