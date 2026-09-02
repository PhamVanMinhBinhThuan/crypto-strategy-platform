package com.cryptostrategy.platform.persistence.internal.experiment;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.backtest.BacktestId;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktest;
import com.cryptostrategy.platform.experiment.api.backtest.StandaloneBacktestAcceptance;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.port.out.StandaloneBacktestStore;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** PostgreSQL transaction boundary for standalone Backtest acceptance. */
public final class JdbcStandaloneBacktestStore implements StandaloneBacktestStore {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ExperimentRows rows;
    private final ExperimentJsonMapper jsonMapper;

    public JdbcStandaloneBacktestStore(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            ExperimentRows rows,
            ExperimentJsonMapper jsonMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
        this.rows = Objects.requireNonNull(rows, "rows");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

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
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        requireConsistentGraph(ownerUserId, backtest, experiment, manifest, candidate, job);

        return transactionTemplate.execute(status -> {
            int claimed = jdbcTemplate.update(
                    ExperimentSql.INSERT_IDEMPOTENCY_CLAIM,
                    ownerUserId,
                    operation,
                    idempotencyKey,
                    requestHash,
                    Timestamp.from(backtest.createdAt()),
                    Timestamp.from(receiptExpiresAt));
            if (claimed == 0) {
                return replay(ownerUserId, operation, idempotencyKey, requestHash);
            }

            insertExperiment(ownerUserId, experiment);
            insertManifest(manifest);
            insertCandidate(candidate);
            insertJob(job);
            insertStandaloneBacktest(backtest);
            insertOutbox(outboxEvent, backtest.createdAt());

            String responseBody = jsonMapper.writeJson(Map.of(
                    "backtestId", backtest.backtestId().value(),
                    "jobId", job.jobId().value()));
            int completed = jdbcTemplate.update(
                    ExperimentSql.COMPLETE_IDEMPOTENCY_RECORD,
                    202,
                    responseBody,
                    ownerUserId,
                    operation,
                    idempotencyKey);
            if (completed != 1) {
                throw new IllegalStateException("Failed to complete standalone Backtest receipt");
            }
            return new StandaloneBacktestAcceptance(
                    backtest, job.jobId(), job.status(), false);
        });
    }

    private StandaloneBacktestAcceptance replay(
            UUID ownerUserId,
            String operation,
            String idempotencyKey,
            String requestHash) {
        Map<String, Object> receipt;
        try {
            receipt = jdbcTemplate.queryForObject(
                    ExperimentSql.SELECT_IDEMPOTENCY_RECORD,
                    (rs, rowNum) -> Map.of(
                            "requestHash", rs.getString("request_hash"),
                            "state", rs.getString("state"),
                            "responseBody", rs.getString("response_body") == null
                                    ? "" : rs.getString("response_body")),
                    ownerUserId,
                    operation,
                    idempotencyKey);
        } catch (EmptyResultDataAccessException exception) {
            throw new IllegalStateException("Idempotency claim disappeared during replay", exception);
        }
        if (!requestHash.equals(receipt.get("requestHash"))) {
            throw new IdempotencyConflictException(
                    "Idempotency key was already used with a different request payload");
        }
        if (!"COMPLETED".equals(receipt.get("state"))) {
            throw new IllegalStateException(
                    "Standalone Backtest receipt is unexpectedly still in progress");
        }

        Map<String, Object> outcome = jsonMapper.readMap((String) receipt.get("responseBody"));
        Object rawBacktestId = outcome.get("backtestId");
        if (rawBacktestId == null) {
            throw new IllegalStateException("Completed Backtest receipt has no backtestId");
        }
        BacktestId backtestId = new BacktestId(rawBacktestId.toString());
        StandaloneBacktest persisted = jdbcTemplate.queryForObject(
                ExperimentSql.SELECT_STANDALONE_BACKTEST_BY_ID,
                rows::mapStandaloneBacktest,
                backtestId.value(),
                ownerUserId);
        if (persisted == null) {
            throw new IllegalStateException("Completed Backtest receipt points to no resource");
        }
        Object rawJobId = outcome.get("jobId");
        if (rawJobId == null || !persisted.jobId().value().equals(rawJobId.toString())) {
            throw new IllegalStateException("Completed Backtest receipt has inconsistent jobId");
        }
        return new StandaloneBacktestAcceptance(
                persisted,
                persisted.jobId(),
                com.cryptostrategy.platform.experiment.api.job.JobStatus.QUEUED,
                true);
    }

    private void insertExperiment(UUID ownerUserId, Experiment experiment) {
        jdbcTemplate.update(
                ExperimentSql.INSERT_EXPERIMENT,
                experiment.experimentId().value(),
                ownerUserId,
                experiment.name(),
                experiment.status().name(),
                null,
                null,
                toTimestamp(experiment.startedAt()),
                null,
                null,
                null,
                toTimestamp(experiment.createdAt()));
    }

    private void insertManifest(ExperimentManifest manifest) {
        jdbcTemplate.update(
                ExperimentSql.INSERT_MANIFEST,
                manifest.experimentId().value(),
                manifest.manifestVersion(),
                manifest.datasetProvenance().datasetVersionId().value(),
                manifest.strategyProvenance().kind().name(),
                legacyStrategyReference(manifest),
                legacyStrategyVersion(manifest),
                jsonMapper.writeJson(manifest.strategyProvenance().parameters()),
                jsonMapper.writeJson(manifest.backtestConfig()),
                jsonMapper.writeJson(manifest.searchConfig()),
                jsonMapper.writeJson(manifest.evaluationConfig()),
                null,
                manifest.softwareVersion(),
                manifest.gitCommit(),
                manifest.fingerprint(),
                toTimestamp(manifest.createdAt()),
                manifest.strategyProvenance().sourceUserStrategyVersionId()
                        .map(value -> value.value()).orElse(null),
                jsonMapper.writeDatasetProvenance(manifest.datasetProvenance()),
                jsonMapper.writeStrategyProvenance(manifest.strategyProvenance()));
    }

    private void insertCandidate(CandidateDefinition candidate) {
        jdbcTemplate.update(
                ExperimentSql.INSERT_CANDIDATE,
                candidate.candidateId().value(),
                candidate.experimentId().value(),
                candidate.generationIndex(),
                jsonMapper.writeJson(candidate.definition()),
                jsonMapper.writeJson(candidate.generatorState()),
                candidate.fingerprint(),
                toTimestamp(candidate.createdAt()));
    }

    private void insertJob(Job job) {
        jdbcTemplate.update(
                ExperimentSql.INSERT_JOB,
                job.jobId().value(),
                job.experimentId().value(),
                job.candidateId().value(),
                job.jobType().name(),
                job.status().name(),
                job.correlationId(),
                job.totalWork(),
                job.completedWork(),
                job.failedWork(),
                job.bestScore(),
                toTimestamp(job.queuedAt()),
                toTimestamp(job.startedAt()),
                toTimestamp(job.finishedAt()),
                toTimestamp(job.nextRetryAt()),
                job.failureCode(),
                job.failureMessage(),
                toTimestamp(job.createdAt()),
                toTimestamp(job.updatedAt()));
    }

    private void insertStandaloneBacktest(StandaloneBacktest backtest) {
        jdbcTemplate.update(
                ExperimentSql.INSERT_STANDALONE_BACKTEST,
                backtest.backtestId().value(),
                backtest.experimentId().value(),
                backtest.candidateId().value(),
                backtest.jobId().value(),
                toTimestamp(backtest.createdAt()));
    }

    private void insertOutbox(OutboxEvent event, Instant createdAt) {
        jdbcTemplate.update(
                ExperimentSql.INSERT_OUTBOX_EVENT,
                event.outboxEventId(),
                event.messageId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.eventVersion(),
                event.payloadJson(),
                jsonMapper.writeJson(event.headers()),
                toTimestamp(event.occurredAt()),
                toTimestamp(createdAt));
    }

    private static void requireConsistentGraph(
            UUID ownerUserId,
            StandaloneBacktest backtest,
            Experiment experiment,
            ExperimentManifest manifest,
            CandidateDefinition candidate,
            Job job) {
        if (!ownerUserId.equals(experiment.ownerUserId())
                || !backtest.experimentId().equals(experiment.experimentId())
                || !manifest.experimentId().equals(experiment.experimentId())
                || !candidate.experimentId().equals(experiment.experimentId())
                || !backtest.candidateId().equals(candidate.candidateId())
                || !backtest.jobId().equals(job.jobId())
                || !job.experimentId().equals(experiment.experimentId())
                || !candidate.candidateId().equals(job.candidateId())) {
            throw new IllegalArgumentException("Standalone Backtest graph is inconsistent");
        }
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static String legacyStrategyReference(ExperimentManifest manifest) {
        var provenance = manifest.strategyProvenance();
        return provenance.singleStrategy().map(reference -> reference.pluginId().value())
                .orElseGet(() -> provenance.compositePolicyId().orElseThrow().value());
    }

    private static String legacyStrategyVersion(ExperimentManifest manifest) {
        var provenance = manifest.strategyProvenance();
        return provenance.singleStrategy()
                .map(reference -> reference.implementationVersion().toString())
                .orElseGet(() -> provenance.compositePolicyVersion().orElseThrow().toString());
    }
}
