package com.cryptostrategy.platform.persistence.internal.execution;

import com.cryptostrategy.platform.execution.api.port.out.AllocateSearchCandidateCommand;
import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationResult;
import com.cryptostrategy.platform.execution.api.port.out.SearchExperimentTransactionGateway;
import com.cryptostrategy.platform.execution.api.port.out.StartSearchGraphCommand;
import com.cryptostrategy.platform.execution.api.port.out.StartSearchGraphResult;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.persistence.internal.experiment.ExperimentJsonMapper;
import com.cryptostrategy.platform.persistence.internal.experiment.ExperimentSql;
import com.cryptostrategy.platform.search.api.model.CoordinationDecision;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** PostgreSQL composite transaction cho dữ liệu thuộc Experiment và Search. */
public final class JdbcSearchExperimentTransaction implements SearchExperimentTransactionGateway, SearchReproductionGateway {
    private static final String INSERT_SEARCH_RUN = """
            insert into search.search_run (
                search_run_id, experiment_id, search_job_id, mode, source_experiment_id,
                generator_id, generator_version, seed, search_space_fingerprint,
                generator_state_contract_version, generator_state, generator_state_fingerprint,
                next_generation_index, maximum_candidates, maximum_duration_ms, max_in_flight,
                status, version, started_at, deadline_at, finished_at, failure_code,
                failure_message, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String LOCK_RUN = """
            select sr.version, sr.status
            from search.search_run sr
            join experiment.experiment e on e.experiment_id = sr.experiment_id
            where sr.search_run_id = ? and e.owner_user_id = ?
            for update of sr
            """;
    private static final String UPDATE_RUN = """
            update search.search_run set
                generator_state_contract_version=?, generator_state=?::jsonb,
                generator_state_fingerprint=?, next_generation_index=?, status=?, version=?,
                started_at=?, deadline_at=?, finished_at=?, failure_code=?, failure_message=?, updated_at=?
            where search_run_id=? and version=?
            """;
    private static final String INSERT_DECISION = """
            insert into search.coordination_decision (
                decision_id, search_run_id, sequence, decision_type, candidate_id,
                backtest_job_id, candidate_fingerprint, state_before_fingerprint,
                state_after_fingerprint, reason_code, decided_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ExperimentJsonMapper json = new ExperimentJsonMapper();

    public JdbcSearchExperimentTransaction(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource");
        this.jdbc = new JdbcTemplate(dataSource);
        this.transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Override
    public StartSearchGraphResult start(StartSearchGraphCommand command) {
        Objects.requireNonNull(command, "command");
        validateStartGraph(command);
        return transaction.execute(status -> startInTransaction(command));
    }

    @Override
    public SearchAllocationResult allocate(AllocateSearchCandidateCommand command) {
        Objects.requireNonNull(command, "command");
        validateAllocationGraph(command);
        return transaction.execute(status -> allocateInTransaction(command));
    }

    @Override
    public java.util.Optional<SourceSnapshot> loadSource(UUID ownerUserId,
            com.cryptostrategy.platform.experiment.api.ExperimentId sourceExperimentId) {
        var rows = jdbc.query("""
                select e.status,
                    not exists (
                        select 1 from experiment.candidate_definition c
                        where c.experiment_id=e.experiment_id and not exists (
                            select 1 from experiment.job j
                            join experiment.backtest_result br on br.job_id=j.job_id
                            join experiment.evaluation_result er on er.backtest_result_id=br.backtest_result_id
                            where j.experiment_id=e.experiment_id and j.candidate_id=c.candidate_id
                              and j.job_type='BACKTEST' and j.status='SUCCEEDED'))
                    and exists (select 1 from experiment.leaderboard_revision lr
                                where lr.experiment_id=e.experiment_id) evidence_complete
                from experiment.experiment e where e.experiment_id=? and e.owner_user_id=?
                """, (rs, row) -> new SourceSnapshot(sourceExperimentId, rs.getString("status"),
                rs.getBoolean("evidence_complete"), jdbc.queryForList(
                        "select candidate_id from experiment.candidate_definition where experiment_id=? order by generation_index,candidate_id",
                        String.class, sourceExperimentId.value())), sourceExperimentId.value(), ownerUserId);
        return rows.stream().findFirst();
    }

    @Override
    public Result create(CreateCommand command) {
        Objects.requireNonNull(command, "command");
        return transaction.execute(status -> createReproduction(command));
    }

    private Result createReproduction(CreateCommand command) {
        int claimed = jdbc.update(ExperimentSql.INSERT_IDEMPOTENCY_CLAIM,
                command.ownerUserId(), "REPRODUCE_SEARCH", command.idempotencyKey(), command.requestHash(),
                timestamp(command.requestedAt()), timestamp(command.receiptExpiresAt()));
        if (claimed == 0) return replayReproduction(command);

        int experiment = jdbc.update("""
                insert into experiment.experiment(experiment_id,owner_user_id,reproduces_experiment_id,name,status,created_at)
                select ?,owner_user_id,experiment_id,?,'QUEUED',? from experiment.experiment
                where experiment_id=? and owner_user_id=? and status in ('COMPLETED','STOPPED')
                """, command.experimentId().value(), command.name(), timestamp(command.requestedAt()),
                command.sourceExperimentId().value(), command.ownerUserId());
        if (experiment != 1) throw new IllegalArgumentException("Source Experiment is inaccessible or non-terminal");
        jdbc.update("""
                insert into experiment.experiment_manifest(experiment_id,manifest_version,dataset_version_id,
                    strategy_kind,strategy_ref_id,strategy_version,strategy_parameters,backtest_config,search_config,
                    evaluation_config,sentiment_config,software_version,git_commit,fingerprint,created_at,
                    source_user_strategy_version_id,dataset_provenance,strategy_provenance)
                select ?,manifest_version,dataset_version_id,strategy_kind,strategy_ref_id,strategy_version,
                    strategy_parameters,backtest_config,search_config,evaluation_config,sentiment_config,
                    software_version,git_commit,fingerprint,?,source_user_strategy_version_id,
                    dataset_provenance,strategy_provenance
                from experiment.experiment_manifest where experiment_id=?
                """, command.experimentId().value(), timestamp(command.requestedAt()),
                command.sourceExperimentId().value());
        jdbc.update("""
                insert into experiment.job(job_id,experiment_id,job_type,status,correlation_id,total_work,
                    completed_work,failed_work,queued_at,created_at,updated_at)
                values (?,?,'SEARCH','QUEUED',?,?,0,0,?,?,?)
                """, command.searchJobId().value(), command.experimentId().value(), command.correlationId(),
                Math.max(1, command.candidates().size()), timestamp(command.requestedAt()),
                timestamp(command.requestedAt()), timestamp(command.requestedAt()));
        jdbc.update("""
                insert into search.search_run(search_run_id,experiment_id,search_job_id,mode,source_experiment_id,
                    generator_id,generator_version,seed,search_space_fingerprint,generator_state_contract_version,
                    generator_state,generator_state_fingerprint,next_generation_index,maximum_candidates,
                    maximum_duration_ms,max_in_flight,status,version,created_at,updated_at)
                select ?,?,?, 'REPRODUCTION',sr.experiment_id,sr.generator_id,sr.generator_version,sr.seed,
                    sr.search_space_fingerprint,sr.generator_state_contract_version,sr.generator_state,
                    sr.generator_state_fingerprint,?,greatest(1,?),sr.maximum_duration_ms,sr.max_in_flight,
                    'PENDING',0,?,? from search.search_run sr where sr.experiment_id=?
                """, command.searchRunId(), command.experimentId().value(), command.searchJobId().value(),
                command.candidates().size(), command.candidates().size(), timestamp(command.requestedAt()),
                timestamp(command.requestedAt()), command.sourceExperimentId().value());
        for (var copy : command.candidates()) {
            jdbc.update("""
                    insert into experiment.candidate_definition(candidate_id,experiment_id,generation_index,
                        definition,generator_state,fingerprint,created_at)
                    select ?,?,generation_index,definition,generator_state,fingerprint,?
                    from experiment.candidate_definition where candidate_id=? and experiment_id=?
                    """, copy.candidateId(), command.experimentId().value(), timestamp(command.requestedAt()),
                    copy.sourceCandidateId(), command.sourceExperimentId().value());
            jdbc.update("""
                    insert into experiment.job(job_id,experiment_id,candidate_id,job_type,status,correlation_id,
                        total_work,completed_work,failed_work,queued_at,created_at,updated_at)
                    values (?,?,?,'BACKTEST','QUEUED',?,1,0,0,?,?,?)
                    """, copy.backtestJobId(), command.experimentId().value(), copy.candidateId(),
                    command.correlationId(), timestamp(command.requestedAt()), timestamp(command.requestedAt()),
                    timestamp(command.requestedAt()));
            String payload = "{\"messageId\":\"" + copy.messageId() + "\",\"messageVersion\":1,"
                    + "\"messageType\":\"BACKTEST_JOB\",\"occurredAt\":\"" + command.requestedAt() + "\","
                    + "\"correlationId\":\"" + command.correlationId().replace("\"", "") + "\","
                    + "\"payload\":{\"experimentId\":\"" + command.experimentId().value() + "\","
                    + "\"jobId\":\"" + copy.backtestJobId() + "\",\"candidateId\":\""
                    + copy.candidateId() + "\"}}";
            jdbc.update(ExperimentSql.INSERT_OUTBOX_EVENT, copy.outboxEventId(), copy.messageId(), "JOB",
                    copy.backtestJobId(), "BACKTEST_JOB", "1", payload,
                    json.writeJson(Map.of("correlationId", command.correlationId())),
                    timestamp(command.requestedAt()), timestamp(command.requestedAt()));
        }
        jdbc.update("""
                insert into search.reproduction_verification(verification_id,source_experiment_id,
                    reproduction_experiment_id,status,version,created_at,updated_at)
                values (?,?,?,'PENDING',0,?,?)
                """, command.verificationId(), command.sourceExperimentId().value(),
                command.experimentId().value(), timestamp(command.requestedAt()), timestamp(command.requestedAt()));
        String body = json.writeJson(Map.of("experimentId", command.experimentId().value(),
                "jobId", command.searchJobId().value()));
        jdbc.update(ExperimentSql.COMPLETE_IDEMPOTENCY_RECORD, 202, body, command.ownerUserId(),
                "REPRODUCE_SEARCH", command.idempotencyKey());
        return new Result(Result.Status.CREATED, command.experimentId(), command.searchJobId());
    }

    private Result replayReproduction(CreateCommand command) {
        Map<String, String> receipt = jdbc.queryForObject(ExperimentSql.SELECT_IDEMPOTENCY_RECORD,
                (rs, row) -> Map.of("hash", rs.getString("request_hash"), "state", rs.getString("state"),
                        "body", rs.getString("response_body") == null ? "" : rs.getString("response_body")),
                command.ownerUserId(), "REPRODUCE_SEARCH", command.idempotencyKey());
        if (!command.requestHash().equals(receipt.get("hash"))) {
            return new Result(Result.Status.CONFLICT, null, null);
        }
        if (!"COMPLETED".equals(receipt.get("state"))) throw new IllegalStateException("Reproduction is in progress");
        Map<String, Object> body = json.readMap(receipt.get("body"));
        return new Result(Result.Status.REPLAY,
                new com.cryptostrategy.platform.experiment.api.ExperimentId(required(body, "experimentId")),
                new com.cryptostrategy.platform.experiment.api.job.JobId(required(body, "jobId")));
    }

    private StartSearchGraphResult startInTransaction(StartSearchGraphCommand command) {
        int claimed = jdbc.update(
                ExperimentSql.INSERT_IDEMPOTENCY_CLAIM,
                command.ownerUserId(), command.operation(), command.idempotencyKey(), command.requestHash(),
                timestamp(command.experiment().createdAt()), timestamp(command.receiptExpiresAt()));
        if (claimed == 0) return replay(command);

        var experiment = command.experiment();
        jdbc.update(ExperimentSql.INSERT_EXPERIMENT,
                experiment.experimentId().value(), command.ownerUserId(), experiment.name(),
                experiment.status().name(), nullableId(experiment.derivedFromExperimentId()),
                nullableId(experiment.reproducesExperimentId()), timestamp(experiment.startedAt()),
                timestamp(experiment.completedAt()), experiment.failureCode(), experiment.failureMessage(),
                timestamp(experiment.createdAt()));
        insertManifest(command.manifest());
        insertJob(command.searchJob());
        insertSearchRun(command.searchRun());
        insertOutbox(command.searchRequest(), command.experiment().createdAt());

        String body = json.writeJson(Map.of(
                "experimentId", experiment.experimentId().value(),
                "jobId", command.searchJob().jobId().value()));
        if (jdbc.update(ExperimentSql.COMPLETE_IDEMPOTENCY_RECORD, 202, body,
                command.ownerUserId(), command.operation(), command.idempotencyKey()) != 1) {
            throw new IllegalStateException("Failed to complete Start Search idempotency receipt");
        }
        return new StartSearchGraphResult(StartSearchGraphResult.Status.CREATED,
                experiment.experimentId(), command.searchJob().jobId());
    }

    private StartSearchGraphResult replay(StartSearchGraphCommand command) {
        Map<String, String> receipt;
        try {
            receipt = jdbc.queryForObject(ExperimentSql.SELECT_IDEMPOTENCY_RECORD,
                    (rs, row) -> Map.of(
                            "hash", rs.getString("request_hash"),
                            "state", rs.getString("state"),
                            "body", rs.getString("response_body") == null ? "" : rs.getString("response_body")),
                    command.ownerUserId(), command.operation(), command.idempotencyKey());
        } catch (EmptyResultDataAccessException missing) {
            throw new IllegalStateException("Start Search idempotency claim disappeared", missing);
        }
        if (!command.requestHash().equals(receipt.get("hash"))) {
            return StartSearchGraphResult.conflict();
        }
        if (!"COMPLETED".equals(receipt.get("state"))) {
            throw new IllegalStateException("Start Search idempotency receipt is still in progress");
        }
        Map<String, Object> body = json.readMap(receipt.get("body"));
        return new StartSearchGraphResult(
                StartSearchGraphResult.Status.REPLAY,
                new com.cryptostrategy.platform.experiment.api.ExperimentId(required(body, "experimentId")),
                new com.cryptostrategy.platform.experiment.api.job.JobId(required(body, "jobId")));
    }

    private SearchAllocationResult allocateInTransaction(AllocateSearchCandidateCommand command) {
        RunFence fence;
        try {
            fence = jdbc.queryForObject(LOCK_RUN,
                    (rs, row) -> new RunFence(rs.getLong("version"), rs.getString("status")),
                    command.claim().snapshot().searchRunId().value(), command.ownerUserId());
        } catch (EmptyResultDataAccessException missing) {
            return SearchAllocationResult.stale(-1);
        }
        if (fence.version() != command.claim().expectedVersion() || !"RUNNING".equals(fence.status())) {
            return SearchAllocationResult.stale(fence.version());
        }

        insertCandidate(command.candidate());
        insertJob(command.backtestJob());
        insertDecision(command.decision());
        SearchRun replacement = command.replacementRun();
        int updated = jdbc.update(UPDATE_RUN,
                replacement.generatorState().contractVersion(), replacement.generatorState().canonicalState(),
                replacement.generatorState().fingerprint(), replacement.nextGenerationIndex(),
                replacement.status().name(), replacement.version(), timestamp(replacement.startedAt()),
                timestamp(replacement.deadlineAt()), timestamp(replacement.finishedAt()), replacement.failureCode(),
                replacement.failureMessage(), timestamp(replacement.updatedAt()),
                replacement.searchRunId().value(), command.claim().expectedVersion());
        if (updated != 1) throw new IllegalStateException("Search Run fence changed while locked");
        insertOutbox(command.outboxEvent(), command.candidate().createdAt());
        return SearchAllocationResult.allocated(
                command.candidate().candidateId(), command.backtestJob().jobId(), replacement.version());
    }

    private void insertManifest(ExperimentManifest manifest) {
        var provenance = manifest.strategyProvenance();
        String reference = provenance.singleStrategy().map(value -> value.pluginId().value())
                .orElseGet(() -> provenance.compositePolicyId().orElseThrow().value());
        String version = provenance.singleStrategy().map(value -> value.implementationVersion().toString())
                .orElseGet(() -> provenance.compositePolicyVersion().orElseThrow().toString());
        jdbc.update(ExperimentSql.INSERT_MANIFEST,
                manifest.experimentId().value(), manifest.manifestVersion(),
                manifest.datasetProvenance().datasetVersionId().value(), provenance.kind().name(),
                reference, version, json.writeJson(provenance.parameters()),
                json.writeJson(manifest.backtestConfig()), json.writeJson(manifest.searchConfig()),
                json.writeJson(manifest.evaluationConfig()), json.writeJson(manifest.sentimentConfig()),
                manifest.softwareVersion(), manifest.gitCommit(), manifest.fingerprint(),
                timestamp(manifest.createdAt()), provenance.sourceUserStrategyVersionId()
                        .map(value -> value.value()).orElse(null),
                json.writeDatasetProvenance(manifest.datasetProvenance()),
                json.writeStrategyProvenance(provenance));
    }

    private void insertSearchRun(SearchRun run) {
        jdbc.update(INSERT_SEARCH_RUN,
                run.searchRunId().value(), run.experimentId().value(), run.searchJobId().value(), run.mode().name(),
                run.sourceExperimentId() == null ? null : run.sourceExperimentId().value(), run.generatorId().value(), run.generatorVersion().toString(), run.seed(),
                run.searchSpaceFingerprint(), run.generatorState().contractVersion(),
                run.generatorState().canonicalState(), run.generatorState().fingerprint(),
                run.nextGenerationIndex(), run.stopConditions().maximumCandidates(),
                run.stopConditions().maximumDuration().toMillis(), run.maxInFlight(), run.status().name(),
                run.version(), timestamp(run.startedAt()), timestamp(run.deadlineAt()), timestamp(run.finishedAt()),
                run.failureCode(), run.failureMessage(), timestamp(run.createdAt()), timestamp(run.updatedAt()));
    }

    private void insertCandidate(CandidateDefinition candidate) {
        jdbc.update(ExperimentSql.INSERT_CANDIDATE,
                candidate.candidateId().value(), candidate.experimentId().value(), candidate.generationIndex(),
                json.writeJson(candidate.definition()), json.writeJson(candidate.generatorState()),
                candidate.fingerprint(), timestamp(candidate.createdAt()));
    }

    private void insertJob(Job job) {
        jdbc.update(ExperimentSql.INSERT_JOB,
                job.jobId().value(), job.experimentId().value(),
                job.candidateId() == null ? null : job.candidateId().value(), job.jobType().name(),
                job.status().name(), job.correlationId(), job.totalWork(), job.completedWork(), job.failedWork(),
                job.bestScore(), timestamp(job.queuedAt()), timestamp(job.startedAt()), timestamp(job.finishedAt()),
                timestamp(job.nextRetryAt()), job.failureCode(), job.failureMessage(),
                timestamp(job.createdAt()), timestamp(job.updatedAt()));
    }

    private void insertDecision(CoordinationDecision decision) {
        jdbc.update(INSERT_DECISION,
                decision.decisionId().value(), decision.searchRunId().value(), decision.sequence(),
                decision.type().name(), decision.candidateId() == null ? null : decision.candidateId().value(),
                decision.backtestJobId() == null ? null : decision.backtestJobId().value(),
                decision.candidateFingerprint(), decision.stateBeforeFingerprint(),
                decision.stateAfterFingerprint(), decision.reasonCode(), timestamp(decision.decidedAt()));
    }

    private void insertOutbox(OutboxEvent event, Instant createdAt) {
        jdbc.update(ExperimentSql.INSERT_OUTBOX_EVENT,
                event.outboxEventId(), event.messageId(), event.aggregateType(), event.aggregateId(),
                event.eventType(), event.eventVersion(), event.payloadJson(), json.writeJson(event.headers()),
                timestamp(event.occurredAt()), timestamp(createdAt));
    }

    private static void validateStartGraph(StartSearchGraphCommand command) {
        var experimentId = command.experiment().experimentId();
        if (!command.ownerUserId().equals(command.experiment().ownerUserId())
                || !experimentId.equals(command.manifest().experimentId())
                || !experimentId.equals(command.searchJob().experimentId())
                || command.searchJob().jobType() != JobType.SEARCH
                || !experimentId.value().equals(command.searchRun().experimentId().value())
                || !command.searchJob().jobId().value().equals(command.searchRun().searchJobId().value())) {
            throw new IllegalArgumentException("Start Search graph is inconsistent");
        }
    }

    private static void validateAllocationGraph(AllocateSearchCandidateCommand command) {
        String experimentId = command.claim().snapshot().experimentId().value();
        if (!experimentId.equals(command.replacementRun().experimentId().value())
                || !experimentId.equals(command.candidate().experimentId().value())
                || !experimentId.equals(command.backtestJob().experimentId().value())
                || command.backtestJob().jobType() != JobType.BACKTEST
                || !command.candidate().candidateId().equals(command.backtestJob().candidateId())
                || !command.claim().snapshot().searchRunId().equals(command.decision().searchRunId())) {
            throw new IllegalArgumentException("Search allocation graph is inconsistent");
        }
    }

    private static String nullableId(com.cryptostrategy.platform.experiment.api.ExperimentId id) {
        return id == null ? null : id.value();
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static String required(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || value.toString().isBlank()) throw new IllegalStateException("Missing receipt field: " + key);
        return value.toString();
    }

    private record RunFence(long version, String status) {}
}
