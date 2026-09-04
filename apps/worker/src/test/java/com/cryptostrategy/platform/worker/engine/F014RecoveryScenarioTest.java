package com.cryptostrategy.platform.worker.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.cryptostrategy.platform.backtesting.api.port.in.PrepareBacktestUseCase;
import com.cryptostrategy.platform.contracts.api.BacktestJobPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.execution.api.port.in.CompleteBacktestAttemptUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.persistence.api.WorkerPersistenceFactory;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.consumer.BacktestJobHandler;
import com.cryptostrategy.platform.worker.consumer.DualLayerIdempotencyGuard;
import com.cryptostrategy.platform.worker.infra.redis.CandidateEvaluatedPublisher;
import com.cryptostrategy.platform.worker.infra.redis.DeadLetterPublisher;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Dependency-backed crash/reclaim scenario. Every Redis key is disposable. */
@EnabledIfEnvironmentVariable(named = "F014_REDIS_SMOKE", matches = "true")
class F014RecoveryScenarioTest {
    private static final String MESSAGE_ID = "01J7K8M9N0P1Q2R3S4T5A6V7W1";
    private static final String EXPERIMENT_ID = "01J7K8M9N0P1Q2R3S4T5A6V7W2";
    private static final String CANDIDATE_ID = "01J7K8M9N0P1Q2R3S4T5A6V7W3";
    private static final String JOB_ID = "01J7K8M9N0P1Q2R3S4T5A6V7W4";

    @Test
    void crashedConsumerIsReclaimedWithoutDuplicateOutcomeAndRedisLossDoesNotDeleteDurableState()
            throws Exception {
        var jdbc = durableState();
        var processed = new WorkerPersistenceFactory(jdbc.getDataSource()).createProcessedMessageStore();
        var dedup = new DualLayerIdempotencyGuard(processed);
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        String testPrefix = "f014-recovery-" + UUID.randomUUID();
        String group = testPrefix + "-backtest-workers";
        var streams = new WorkerProperties.Streams(
                testPrefix, null, null, null, null, null, null);
        var consumer = new WorkerProperties.Consumer(
                group,
                testPrefix + "-ranking",
                testPrefix + "-search",
                "worker-restarted",
                10,
                Duration.ofMillis(50),
                Duration.ofMillis(5),
                10);
        var properties = new WorkerProperties(
                null, streams, consumer, null, null, null, null, null);
        String stream = properties.streams().getBacktestJobsStream();

        var factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        factory.start();
        var redis = new StringRedisTemplate(factory);
        var publisher = new RedisStreamPublisher(redis);
        var reader = new RedisStreamMessageReader(redis);

        try {
            var payload = new BacktestJobPayload(EXPERIMENT_ID, JOB_ID, CANDIDATE_ID);
            var envelope = new MessageEnvelope<>(
                    MESSAGE_ID,
                    1,
                    MessageTypes.BACKTEST_JOB,
                    Instant.parse("2026-09-04T05:00:00Z"),
                    JOB_ID,
                    payload);
            publisher.publish(stream, MESSAGE_ID, mapper.writeValueAsString(envelope), Map.of());
            redis.opsForStream().createGroup(stream, ReadOffset.from("0-0"), group);

            List<MapRecord<String, String, String>> delivered = reader.readGroupBatch(
                    stream, group, "worker-crashed", 1, Duration.ofMillis(50));
            assertThat(delivered).hasSize(1);
            assertThat(reader.pendingSummary(stream, group).getTotalPendingMessages()).isEqualTo(1);

            // The business transaction and durable dedup marker committed, but the
            // worker disappears before ACK. Redis therefore redelivers this record.
            jdbc.update(
                    "update f014_durable_state set state='ACCEPTED' where entity_type='RESULT'");
            jdbc.update(
                    """
                    insert into platform.processed_message(
                        consumer_name, message_id, processed_at, expires_at
                    ) values (?, ?, current_timestamp, DATEADD('HOUR', 1, current_timestamp))
                    """,
                    group,
                    MESSAGE_ID);
            assertThat(dedup.isAlreadyProcessed(group, MESSAGE_ID)).isTrue();

            var reclaimedRef = new AtomicReference<List<MapRecord<String, String, String>>>(List.of());
            await().atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(10))
                    .untilAsserted(() -> {
                        var reclaimed = reader.claimStaleBatch(
                                stream,
                                group,
                                properties.consumer().consumerName(),
                                properties.consumer().pendingIdleTime(),
                                10);
                        assertThat(reclaimed).hasSize(1);
                        reclaimedRef.set(reclaimed);
                    });

            TrustedWorkerExperimentUseCase experiments = mock(TrustedWorkerExperimentUseCase.class);
            var handler = new BacktestJobHandler(
                    dedup,
                    experiments,
                    mock(PrepareBacktestUseCase.class),
                    mock(CompleteBacktestAttemptUseCase.class),
                    mock(CandidateEvaluatedPublisher.class),
                    mock(DeadLetterPublisher.class),
                    reader,
                    properties,
                    mapper);

            handler.handle(reclaimedRef.get().getFirst());

            verify(experiments, never()).startNextAttempt(any(), any());
            assertThat(reader.pendingSummary(stream, group).getTotalPendingMessages()).isZero();
            assertThat(jdbc.queryForObject(
                            "select count(*) from f014_durable_state where entity_type='RESULT' and state='ACCEPTED'",
                            Integer.class))
                    .isEqualTo(1);

            // Redis is queue/cache only. Losing the disposable stream must not
            // remove accepted state from the durable database.
            redis.delete(stream);
            assertThat(jdbc.queryForObject(
                            "select count(*) from f014_durable_state",
                            Integer.class))
                    .isEqualTo(6);
            assertThat(jdbc.queryForList(
                            "select entity_type from f014_durable_state order by entity_type",
                            String.class))
                    .containsExactly(
                            "EXPERIMENT", "JOB", "NEWS", "PUBLICATION", "RESULT", "SENTIMENT");
        } finally {
            redis.delete(stream);
            factory.destroy();
        }
    }

    private static JdbcTemplate durableState() {
        var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(
                "jdbc:h2:mem:f014_recovery;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                        + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create schema if not exists platform");
        jdbc.execute("""
                create table if not exists platform.processed_message (
                    consumer_name varchar(128) not null,
                    message_id varchar(128) not null,
                    processed_at timestamp with time zone not null,
                    expires_at timestamp with time zone not null,
                    primary key (consumer_name, message_id)
                )
                """);
        jdbc.execute("""
                create table if not exists f014_durable_state (
                    entity_type varchar(32) primary key,
                    entity_id varchar(64) not null,
                    state varchar(32) not null
                )
                """);
        jdbc.execute("delete from platform.processed_message");
        jdbc.execute("delete from f014_durable_state");
        for (String entity : List.of(
                "EXPERIMENT", "JOB", "RESULT", "NEWS", "SENTIMENT", "PUBLICATION")) {
            jdbc.update(
                    "insert into f014_durable_state(entity_type, entity_id, state) values (?, ?, ?)",
                    entity,
                    entity.toLowerCase() + "-001",
                    "DURABLE");
        }
        return jdbc;
    }
}
