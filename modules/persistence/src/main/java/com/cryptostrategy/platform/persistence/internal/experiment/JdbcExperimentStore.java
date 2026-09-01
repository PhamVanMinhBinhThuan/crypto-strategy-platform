package com.cryptostrategy.platform.persistence.internal.experiment;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.port.out.ExperimentStore;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JdbcExperimentStore implements ExperimentStore {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final ExperimentRows rows;
    private final ExperimentJsonMapper jsonMapper;

    public JdbcExperimentStore(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            ExperimentRows rows,
            ExperimentJsonMapper jsonMapper
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate cannot be null");
        this.rows = Objects.requireNonNull(rows, "rows cannot be null");
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper cannot be null");
    }

    @Override
    public void insertExperiment(UUID ownerUserId, Experiment experiment, ExperimentManifest draftManifest) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    ExperimentSql.INSERT_EXPERIMENT,
                    experiment.experimentId().value(),
                    ownerUserId,
                    experiment.name(),
                    experiment.status().name(),
                    experiment.derivedFromExperimentId() != null ? experiment.derivedFromExperimentId().value() : null,
                    experiment.reproducesExperimentId() != null ? experiment.reproducesExperimentId().value() : null,
                    toTimestamp(experiment.startedAt()),
                    toTimestamp(experiment.completedAt()),
                    experiment.failureCode(),
                    experiment.failureMessage(),
                    toTimestamp(experiment.createdAt())
            );

            jdbcTemplate.update(
                    ExperimentSql.INSERT_MANIFEST,
                    draftManifest.experimentId().value(),
                    draftManifest.manifestVersion(),
                    draftManifest.datasetProvenance().datasetVersionId().value(),
                    draftManifest.strategyProvenance().kind().name(),
                    legacyStrategyReference(draftManifest),
                    legacyStrategyVersion(draftManifest),
                    jsonMapper.writeJson(draftManifest.strategyProvenance().parameters()),
                    jsonMapper.writeJson(draftManifest.backtestConfig()),
                    jsonMapper.writeJson(draftManifest.searchConfig()),
                    jsonMapper.writeJson(draftManifest.evaluationConfig()),
                    draftManifest.sentimentConfig() != null ? jsonMapper.writeJson(draftManifest.sentimentConfig()) : null,
                    draftManifest.softwareVersion(),
                    draftManifest.gitCommit(),
                    draftManifest.fingerprint(),
                    toTimestamp(draftManifest.createdAt()),
                    draftManifest.strategyProvenance().sourceUserStrategyVersionId().map(id -> id.value()).orElse(null),
                    jsonMapper.writeDatasetProvenance(draftManifest.datasetProvenance()),
                    jsonMapper.writeStrategyProvenance(draftManifest.strategyProvenance())
            );
        });
    }

    @Override
    public Optional<Experiment> findExperimentById(UUID ownerUserId, ExperimentId experimentId) {
        try {
            Experiment experiment = jdbcTemplate.queryForObject(
                    ExperimentSql.SELECT_EXPERIMENT_BY_ID,
                    rows::mapExperiment,
                    experimentId.value(),
                    ownerUserId
            );
            return Optional.ofNullable(experiment);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ExperimentManifest> findManifestByExperimentId(UUID ownerUserId, ExperimentId experimentId) {
        try {
            ExperimentManifest manifest = jdbcTemplate.queryForObject(
                    ExperimentSql.SELECT_MANIFEST_BY_EXPERIMENT_ID,
                    rows::mapManifest,
                    experimentId.value(),
                    ownerUserId
            );
            return Optional.ofNullable(manifest);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public void updateManifest(UUID ownerUserId, ExperimentId experimentId, ExperimentManifest updatedManifest) {
        jdbcTemplate.update(
                ExperimentSql.UPDATE_MANIFEST,
                updatedManifest.datasetProvenance().datasetVersionId().value(),
                updatedManifest.strategyProvenance().kind().name(),
                legacyStrategyReference(updatedManifest),
                legacyStrategyVersion(updatedManifest),
                jsonMapper.writeJson(updatedManifest.strategyProvenance().parameters()),
                jsonMapper.writeJson(updatedManifest.backtestConfig()),
                jsonMapper.writeJson(updatedManifest.searchConfig()),
                jsonMapper.writeJson(updatedManifest.evaluationConfig()),
                updatedManifest.sentimentConfig() != null ? jsonMapper.writeJson(updatedManifest.sentimentConfig()) : null,
                updatedManifest.softwareVersion(),
                updatedManifest.gitCommit(),
                updatedManifest.strategyProvenance().sourceUserStrategyVersionId().map(id -> id.value()).orElse(null),
                jsonMapper.writeDatasetProvenance(updatedManifest.datasetProvenance()),
                jsonMapper.writeStrategyProvenance(updatedManifest.strategyProvenance()),
                experimentId.value(),
                ownerUserId
        );
    }

    @Override
    public void freezeAndQueueExperiment(
            UUID ownerUserId,
            ExperimentId experimentId,
            String fingerprint,
            Instant queuedAt,
            OutboxEvent outboxEvent
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            int updated = jdbcTemplate.update(
                    ExperimentSql.FREEZE_AND_QUEUE_EXPERIMENT,
                    toTimestamp(queuedAt),
                    experimentId.value(),
                    ownerUserId
            );
            if (updated == 0) {
                throw new IllegalStateException("Failed to freeze experiment: not found or not in CREATED status");
            }

            jdbcTemplate.update(
                    ExperimentSql.UPDATE_MANIFEST_FINGERPRINT,
                    fingerprint,
                    experimentId.value(),
                    ownerUserId
            );

            if (outboxEvent != null) {
                jdbcTemplate.update(
                        ExperimentSql.INSERT_OUTBOX_EVENT,
                        outboxEvent.outboxEventId(),
                        outboxEvent.messageId(),
                        outboxEvent.aggregateType(),
                        outboxEvent.aggregateId(),
                        outboxEvent.eventType(),
                        outboxEvent.eventVersion(),
                        outboxEvent.payloadJson(),
                        jsonMapper.writeJson(outboxEvent.headers()),
                        toTimestamp(outboxEvent.occurredAt()),
                        toTimestamp(Instant.now())
                );
            }
        });
    }

    @Override
    public void updateExperimentStatus(UUID ownerUserId, ExperimentId experimentId, ExperimentStatus newStatus, Instant updatedAt) {
        jdbcTemplate.update(
                ExperimentSql.UPDATE_EXPERIMENT_STATUS,
                newStatus.name(),
                toTimestamp(updatedAt),
                newStatus.isTerminal() ? toTimestamp(updatedAt) : null,
                experimentId.value(),
                ownerUserId
        );
    }

    @Override
    public void stopExperimentWithOutbox(UUID ownerUserId, ExperimentId experimentId, OutboxEvent outboxEvent, Instant updatedAt) {
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update(
                    ExperimentSql.UPDATE_EXPERIMENT_STATUS,
                    ExperimentStatus.STOP_REQUESTED.name(),
                    toTimestamp(updatedAt),
                    null,
                    experimentId.value(),
                    ownerUserId
            );

            if (outboxEvent != null) {
                jdbcTemplate.update(
                        ExperimentSql.INSERT_OUTBOX_EVENT,
                        outboxEvent.outboxEventId(),
                        outboxEvent.messageId(),
                        outboxEvent.aggregateType(),
                        outboxEvent.aggregateId(),
                        outboxEvent.eventType(),
                        outboxEvent.eventVersion(),
                        outboxEvent.payloadJson(),
                        jsonMapper.writeJson(outboxEvent.headers()),
                        toTimestamp(outboxEvent.occurredAt()),
                        toTimestamp(Instant.now())
                );
            }
        });
    }

    @Override
    public void insertCandidate(UUID ownerUserId, CandidateDefinition candidate) {
        jdbcTemplate.update(
                ExperimentSql.INSERT_CANDIDATE,
                candidate.candidateId().value(),
                candidate.experimentId().value(),
                candidate.generationIndex(),
                jsonMapper.writeJson(candidate.definition()),
                candidate.generatorState() != null ? jsonMapper.writeJson(candidate.generatorState()) : null,
                candidate.fingerprint(),
                toTimestamp(candidate.createdAt())
        );
    }

    @Override
    public List<CandidateDefinition> listCandidatesByExperimentId(UUID ownerUserId, ExperimentId experimentId) {
        return jdbcTemplate.query(
                ExperimentSql.SELECT_CANDIDATES_BY_EXPERIMENT_ID,
                rows::mapCandidate,
                experimentId.value(),
                ownerUserId
        );
    }

    @Override
    public Optional<CandidateDefinition> findCandidateById(UUID ownerUserId, CandidateId candidateId) {
        try {
            CandidateDefinition candidate = jdbcTemplate.queryForObject(
                    ExperimentSql.SELECT_CANDIDATE_BY_ID,
                    rows::mapCandidate,
                    candidateId.value(),
                    ownerUserId
            );
            return Optional.ofNullable(candidate);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UUID> findOwnerUserIdByExperimentId(ExperimentId experimentId) {
        try {
            UUID ownerUserId = jdbcTemplate.queryForObject(
                    ExperimentSql.SELECT_OWNER_BY_EXPERIMENT_ID,
                    UUID.class,
                    experimentId.value()
            );
            return Optional.ofNullable(ownerUserId);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<com.cryptostrategy.platform.experiment.api.job.StopCandidateExperiment> findStopCompletionCandidates(int limit) {
        return jdbcTemplate.query(
                ExperimentSql.SELECT_STOP_COMPLETION_CANDIDATES,
                (rs, rowNum) -> new com.cryptostrategy.platform.experiment.api.job.StopCandidateExperiment(
                        new ExperimentId(rs.getString("experiment_id")),
                        rs.getTimestamp("completed_at") != null ? rs.getTimestamp("completed_at").toInstant() : null
                ),
                limit
        );
    }

    @Override
    public List<com.cryptostrategy.platform.experiment.api.job.Job> listAllJobsByExperimentId(ExperimentId experimentId) {
        return jdbcTemplate.query(
                ExperimentSql.SELECT_ALL_JOBS_BY_EXPERIMENT_ID,
                rows::mapJob,
                experimentId.value()
        );
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }

    private static String legacyStrategyReference(ExperimentManifest manifest) {
        var provenance = manifest.strategyProvenance();
        return provenance.singleStrategy().map(reference -> reference.pluginId().value())
                .orElseGet(() -> provenance.compositePolicyId().orElseThrow().value());
    }

    private static String legacyStrategyVersion(ExperimentManifest manifest) {
        var provenance = manifest.strategyProvenance();
        return provenance.singleStrategy().map(reference -> reference.implementationVersion().toString())
                .orElseGet(() -> provenance.compositePolicyVersion().orElseThrow().toString());
    }
}
