package com.cryptostrategy.platform.persistence.internal.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.execution.api.port.out.SearchExperimentTransactionGateway;
import com.cryptostrategy.platform.execution.api.port.out.StartSearchGraphCommand;
import com.cryptostrategy.platform.execution.api.port.out.StartSearchGraphResult;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import com.cryptostrategy.platform.persistence.internal.execution.JdbcSearchExperimentTransaction;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.model.SearchRunMode;
import com.cryptostrategy.platform.search.api.model.SearchStopConditions;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class SearchStartTransactionIntegrationTest {
    private static final UUID OWNER = UUID.fromString("91000000-0000-4000-8000-000000000010");
    private static final String EXPERIMENT = "61000000000000000000000001";
    private static final String JOB = "61000000000000000000000002";
    private static final String RUN = "61000000000000000000000003";
    private static final String DATASET = "61000000000000000000000004";
    private static final String STRATEGY = "61000000000000000000000005";
    private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");

    @Test
    void exactReplayReturnsTheSameIdentityAndPayloadConflictCreatesNothingElse() {
        withRollback((jdbc, gateway) -> {
            seedPrerequisites(jdbc);
            StartSearchGraphCommand command = graph("request-hash-a", "{\"drawIndex\":0}");

            StartSearchGraphResult created = gateway.start(command);
            StartSearchGraphResult replay = gateway.start(command);
            StartSearchGraphResult conflict = gateway.start(graph("request-hash-b", "{\"drawIndex\":0}"));

            assertThat(created.status()).isEqualTo(StartSearchGraphResult.Status.CREATED);
            assertThat(replay).isEqualTo(created.asReplay());
            assertThat(conflict.status()).isEqualTo(StartSearchGraphResult.Status.CONFLICT);
            assertThat(count(jdbc, "experiment.experiment")).isEqualTo(1);
            assertThat(count(jdbc, "experiment.job")).isEqualTo(1);
            assertThat(count(jdbc, "search.search_run")).isEqualTo(1);
            assertThat(count(jdbc, "platform.outbox_event")).isEqualTo(1);
        });
    }

    @Test
    void failureAfterExperimentWritesRollsBackTheWholeGraphAndIdempotencyClaim() {
        DataSource dataSource = dataSource();
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.executeWithoutResult(status -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            seedPrerequisites(jdbc);
        });

        assertThatThrownBy(() -> new JdbcSearchExperimentTransaction(dataSource)
                        .start(graph("request-hash-a", "not-json")))
                .isInstanceOf(RuntimeException.class);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            assertThat(count(jdbc, "experiment.experiment")).isZero();
            assertThat(count(jdbc, "experiment.job")).isZero();
            assertThat(count(jdbc, "search.search_run")).isZero();
            assertThat(count(jdbc, "platform.outbox_event")).isZero();
            assertThat(count(jdbc, "platform.idempotency_record")).isZero();
        } finally {
            jdbc.update("delete from strategy.strategy_version where strategy_version_id = ?", STRATEGY);
            jdbc.update("delete from market.dataset_version where dataset_version_id = ?", DATASET);
            jdbc.update("delete from market.trading_pair where trading_pair_id = '6100000000000000000000000A'");
            jdbc.update("delete from market.asset where asset_id in "
                    + "('61000000000000000000000008','61000000000000000000000009')");
            jdbc.update("delete from auth.users where id = ?", OWNER);
        }
    }

    private static StartSearchGraphCommand graph(String requestHash, String canonicalState) {
        ExperimentId experimentId = new ExperimentId(EXPERIMENT);
        JobId jobId = new JobId(JOB);
        Experiment experiment = Experiment.create(experimentId, OWNER, "F010 atomic start", null, null, NOW);
        StrategyReference strategy = new StrategyReference(
                new StrategyVersionId(STRATEGY), new StrategyPluginId("momentum"), new SemanticVersion(1, 0, 0));
        ExperimentManifest manifest = new ExperimentManifest(
                experimentId,
                "manifest-v1",
                new DatasetProvenanceSnapshot(
                        new DatasetVersionId(DATASET), "dataset-v1", "sha256:" + "1".repeat(64),
                        "fixture", "BTCUSDT", "1m", "normalization-v1",
                        NOW.minusSeconds(60), NOW, 1),
                StrategyProvenanceSnapshot.single(
                        strategy, StrategyParameterSet.empty(), Optional.empty(),
                        "strategy-v1:sha256:" + "2".repeat(64)),
                Map.of("initialCapital", "1000"), Map.of("generator", "random-search"),
                Map.of("metricVersion", "metrics-v1"), null,
                "1.0.0", "commit-f010", "manifest-fingerprint", NOW);
        Job job = Job.createSearchJob(jobId, experimentId, "correlation-f010", 2, NOW);
        GeneratorDescriptor descriptor = new GeneratorDescriptor(
                new GeneratorId("random-search"), GeneratorVersion.parse("1.0.0"),
                "random-state-v1", Set.of(ParameterType.INTEGER), "descriptor-fingerprint");
        SearchRun run = SearchRun.pending(
                new SearchRunId(RUN), EXPERIMENT, JOB, SearchRunMode.GENERATION, null, descriptor,
                42L, "space-fingerprint",
                new GeneratorState("random-state-v1", canonicalState, "state-0"),
                new SearchStopConditions(2, Duration.ofMinutes(10)), 1, NOW);
        OutboxEvent event = new OutboxEvent(
                "61000000000000000000000006", "61000000000000000000000007",
                "JOB", JOB, "SEARCH_REQUEST", "1", "{}", Map.of("correlationId", "correlation-f010"), NOW);
        return new StartSearchGraphCommand(
                OWNER, "START_SEARCH", "idempotency-f010", requestHash, NOW.plus(Duration.ofHours(1)),
                experiment, manifest, job, run, event);
    }

    private static void seedPrerequisites(JdbcTemplate jdbc) {
        jdbc.update("insert into auth.users(id) values (?)", OWNER);
        jdbc.update("insert into market.asset(asset_id,symbol) values ('61000000000000000000000008','BTC')");
        jdbc.update("insert into market.asset(asset_id,symbol) values ('61000000000000000000000009','USDT')");
        jdbc.update("insert into market.trading_pair(trading_pair_id,base_asset_id,quote_asset_id,symbol) "
                + "values ('6100000000000000000000000A','61000000000000000000000008','61000000000000000000000009','BTCUSDT')");
        jdbc.update("insert into market.dataset_version(dataset_version_id,version,provider,trading_pair_id,timeframe,"
                + "normalization_version,range_start,range_end,candle_count,checksum) values (?,?,?,?,'1m',?,?,?,1,?)",
                DATASET, "dataset-v1", "fixture", "6100000000000000000000000A",
                "normalization-v1", Timestamp.from(NOW.minusSeconds(60)), Timestamp.from(NOW),
                "sha256:" + "1".repeat(64));
        jdbc.update("insert into strategy.strategy_version(strategy_version_id,plugin_id,version,display_name,"
                + "parameter_schema,default_parameters,supported_signals,fingerprint) "
                + "values (?,?,'1.0.0','Momentum','{}','{}','[]',?)",
                STRATEGY, "momentum", "strategy-v1:sha256:" + "2".repeat(64));
    }

    private static int count(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private static void withRollback(TestBody body) {
        DataSource dataSource = dataSource();
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transaction.executeWithoutResult(status -> {
            status.setRollbackOnly();
            body.run(new JdbcTemplate(dataSource), new JdbcSearchExperimentTransaction(dataSource));
        });
    }

    private static DataSource dataSource() {
        return new DriverManagerDataSource(
                System.getenv("DATABASE_URL"),
                System.getenv("DATABASE_USERNAME"),
                System.getenv("DATABASE_PASSWORD"));
    }

    @FunctionalInterface
    private interface TestBody {
        void run(JdbcTemplate jdbc, SearchExperimentTransactionGateway gateway);
    }
}
