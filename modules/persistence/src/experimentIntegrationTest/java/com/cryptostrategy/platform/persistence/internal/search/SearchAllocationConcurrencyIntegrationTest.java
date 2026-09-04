package com.cryptostrategy.platform.persistence.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.execution.api.port.out.AllocateSearchCandidateCommand;
import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationResult;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.persistence.internal.execution.JdbcSearchExperimentTransaction;
import com.cryptostrategy.platform.search.api.model.CoordinationDecision;
import com.cryptostrategy.platform.search.api.model.CoordinationDecisionId;
import com.cryptostrategy.platform.search.api.model.CoordinationDecisionType;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.model.SearchRunMode;
import com.cryptostrategy.platform.search.api.model.SearchStopConditions;
import com.cryptostrategy.platform.search.api.port.out.SearchRunClaim;
import com.cryptostrategy.platform.search.internal.RandomStrategyGenerator;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class SearchAllocationConcurrencyIntegrationTest {
    public static final UUID OWNER = UUID.fromString("92000000-0000-4000-8000-000000000010");
    public static final String EXPERIMENT = "62000000000000000000000001";
    public static final String SEARCH_JOB = "62000000000000000000000002";
    public static final String RUN = "62000000000000000000000003";
    public static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");

    @Test
    void twoAllocatorsWithTheSameFenceCommitAtMostOneGenerationIndex() throws Exception {
        DataSource dataSource = dataSource();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        seed(jdbc);
        try {
            var gateway = new JdbcSearchExperimentTransaction(dataSource);
            SearchRun claimedRun = initialRun().start(NOW.plusSeconds(1));
            SearchRunClaim claim = new SearchRunClaim(claimedRun, claimedRun.version());
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);

            Callable<SearchAllocationResult> first = allocation(gateway, claim, "A", ready, release);
            Callable<SearchAllocationResult> second = allocation(gateway, claim, "B", ready, release);
            try (var pool = Executors.newFixedThreadPool(2)) {
                var firstResult = pool.submit(first);
                var secondResult = pool.submit(second);
                ready.await();
                release.countDown();

                assertThat(List.of(firstResult.get().status(), secondResult.get().status()))
                        .containsExactlyInAnyOrder(
                                SearchAllocationResult.Status.ALLOCATED,
                                SearchAllocationResult.Status.STALE_FENCE);
            }

            assertThat(jdbc.queryForObject(
                    "select count(*) from experiment.candidate_definition where experiment_id = ?",
                    Integer.class, EXPERIMENT)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "select count(*) from experiment.job where experiment_id = ? and job_type = 'BACKTEST'",
                    Integer.class, EXPERIMENT)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "select count(*) from search.coordination_decision where search_run_id = ?",
                    Integer.class, RUN)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "select next_generation_index from search.search_run where search_run_id = ?",
                    Integer.class, RUN)).isEqualTo(1);
        } finally {
            cleanup(jdbc);
        }
    }

    public static Callable<SearchAllocationResult> allocation(
            JdbcSearchExperimentTransaction gateway,
            SearchRunClaim claim,
            String suffix,
            CountDownLatch ready,
            CountDownLatch release
    ) {
        return () -> {
            ready.countDown();
            release.await();
            String candidateId = "6200000000000000000000001" + suffix;
            String backtestJobId = "6200000000000000000000002" + suffix;
            String decisionId = "6200000000000000000000003" + suffix;
            CandidateDefinition candidate = new CandidateDefinition(
                    new CandidateId(candidateId), new ExperimentId(EXPERIMENT), 0,
                    Map.of("period", suffix.equals("A") ? 5 : 10), Map.of("drawIndex", 1),
                    "candidate-" + suffix, NOW.plusSeconds(2));
            Job job = Job.createBacktestJob(
                    new JobId(backtestJobId), new ExperimentId(EXPERIMENT), candidate.candidateId(),
                    "6200000000000000000000006" + suffix, NOW.plusSeconds(2));
            GeneratorState nextState = new GeneratorState(
                    "random-state-v1", "{\"drawIndex\":1,\"branch\":\"" + suffix + "\"}", "state-" + suffix);
            SearchRun replacement = claim.snapshot().advance(nextState, 1, NOW.plusSeconds(2));
            CoordinationDecision decision = new CoordinationDecision(
                    new CoordinationDecisionId(decisionId), new SearchRunId(RUN), 0,
                    CoordinationDecisionType.ALLOCATED,
                    new com.cryptostrategy.platform.search.api.model.SearchCandidateId(candidateId),
                    new com.cryptostrategy.platform.search.api.model.SearchJobId(backtestJobId),
                    candidate.fingerprint(), claim.snapshot().generatorState().fingerprint(),
                    nextState.fingerprint(), "CANDIDATE_ALLOCATED", NOW.plusSeconds(2));
            OutboxEvent outbox = new OutboxEvent(
                    "6200000000000000000000004" + suffix,
                    "6200000000000000000000005" + suffix,
                    "JOB", backtestJobId, "JOB_QUEUED", "1", "{}", Map.of(), NOW.plusSeconds(2));
            return gateway.allocate(new AllocateSearchCandidateCommand(
                    OWNER, claim, replacement, candidate, job, decision, outbox));
        };
    }

    public static SearchRun initialRun() {
        GeneratorDescriptor descriptor = new GeneratorDescriptor(
                new GeneratorId("random-search"), GeneratorVersion.parse("1.0.0"),
                "random-state-v1", Set.of(ParameterType.INTEGER), "descriptor-fingerprint");
        return SearchRun.pending(
                new SearchRunId(RUN), new com.cryptostrategy.platform.search.api.model.SearchExperimentId(EXPERIMENT),
                new com.cryptostrategy.platform.search.api.model.SearchJobId(SEARCH_JOB), SearchRunMode.GENERATION, null,
                descriptor, 42, "space-fingerprint",
                RandomStrategyGenerator.initialState(42),
                new SearchStopConditions(2, Duration.ofMinutes(10)), 1, NOW);
    }

    public static void seed(JdbcTemplate jdbc) {
        cleanup(jdbc);
        jdbc.update("insert into auth.users(id) values (?)", OWNER);
        jdbc.update("insert into experiment.experiment(experiment_id,owner_user_id,name,status) values (?,?,'F010 allocation','RUNNING')",
                EXPERIMENT, OWNER);
        jdbc.update("insert into experiment.job(job_id,experiment_id,job_type,status,correlation_id,total_work,completed_work,failed_work) "
                + "values (?,?,'SEARCH','RUNNING','62000000000000000000000009',2,0,0)", SEARCH_JOB, EXPERIMENT);
        SearchRun running = initialRun().start(NOW.plusSeconds(1));
        jdbc.update("insert into search.search_run(search_run_id,experiment_id,search_job_id,mode,generator_id,generator_version,seed,"
                + "search_space_fingerprint,generator_state_contract_version,generator_state,generator_state_fingerprint,"
                + "next_generation_index,maximum_candidates,maximum_duration_ms,max_in_flight,status,version,started_at,deadline_at,created_at,updated_at) "
                + "values (?,?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?,?,?,?,?,?,?,?)",
                RUN, EXPERIMENT, SEARCH_JOB, "GENERATION", "random-search", "1.0.0", 42,
                "space-fingerprint", "random-state-v1", running.generatorState().canonicalState(),
                running.generatorState().fingerprint(), 0, 2,
                Duration.ofMinutes(10).toMillis(), 1, "RUNNING", running.version(),
                Timestamp.from(running.startedAt()), Timestamp.from(running.deadlineAt()),
                Timestamp.from(NOW), Timestamp.from(running.updatedAt()));
    }

    static void cleanup(JdbcTemplate jdbc) {
        jdbc.update("delete from platform.outbox_event where aggregate_id like '6200000000000000000000002%'");
        jdbc.update("delete from search.coordination_decision where search_run_id = ?", RUN);
        jdbc.update("delete from experiment.job where experiment_id = ? and job_type = 'BACKTEST'", EXPERIMENT);
        jdbc.update("delete from experiment.candidate_definition where experiment_id = ?", EXPERIMENT);
        jdbc.update("delete from search.search_run where search_run_id = ?", RUN);
        jdbc.update("delete from experiment.job where job_id = ?", SEARCH_JOB);
        jdbc.update("delete from experiment.experiment where experiment_id = ?", EXPERIMENT);
        jdbc.update("delete from auth.users where id = ?", OWNER);
    }

    public static DataSource dataSource() {
        return new DriverManagerDataSource(
                System.getenv("DATABASE_URL"),
                System.getenv("DATABASE_USERNAME"),
                System.getenv("DATABASE_PASSWORD"));
    }
}
