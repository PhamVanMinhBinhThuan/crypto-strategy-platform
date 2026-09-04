package com.cryptostrategy.platform.worker.integration;

import com.cryptostrategy.platform.backtesting.api.port.in.PrepareBacktestUseCase;
import com.cryptostrategy.platform.contracts.api.BacktestJobPayload;
import com.cryptostrategy.platform.contracts.api.MessageEnvelope;
import com.cryptostrategy.platform.contracts.api.MessageTypes;
import com.cryptostrategy.platform.execution.api.port.in.CompleteBacktestAttemptUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.TrustedWorkerExperimentUseCase;
import com.cryptostrategy.platform.persistence.api.WorkerPersistenceFactory;
import com.cryptostrategy.platform.persistence.api.worker.ProcessedMessageStore;
import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.consumer.BacktestJobHandler;
import com.cryptostrategy.platform.worker.consumer.DualLayerIdempotencyGuard;
import com.cryptostrategy.platform.worker.infra.redis.CandidateEvaluatedPublisher;
import com.cryptostrategy.platform.worker.infra.redis.DeadLetterPublisher;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DualLayerDedupIntegrationTest {

    private DataSource dataSource;
    private ProcessedMessageStore processedMessageStore;
    private DualLayerIdempotencyGuard idempotencyGuard;
    private TrustedWorkerExperimentUseCase experimentUseCase;
    private PrepareBacktestUseCase prepareBacktestUseCase;
    private CompleteBacktestAttemptUseCase completeBacktestAttemptUseCase;
    private CandidateEvaluatedPublisher candidateEvaluatedPublisher;
    private DeadLetterPublisher deadLetterPublisher;
    private RedisStreamMessageReader messageReader;
    private WorkerProperties workerProperties;
    private ObjectMapper objectMapper;
    private BacktestJobHandler handler;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:worker_dedup_integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");
        this.dataSource = ds;

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS platform");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS platform.processed_message (
                consumer_name VARCHAR(128) NOT NULL,
                message_id VARCHAR(128) NOT NULL,
                processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                PRIMARY KEY (consumer_name, message_id)
            )
        """);
        jdbc.execute("DELETE FROM platform.processed_message");

        WorkerPersistenceFactory factory = new WorkerPersistenceFactory(dataSource);
        this.processedMessageStore = factory.createProcessedMessageStore();
        this.idempotencyGuard = new DualLayerIdempotencyGuard(processedMessageStore);
        this.experimentUseCase = mock(TrustedWorkerExperimentUseCase.class);
        this.prepareBacktestUseCase = mock(PrepareBacktestUseCase.class);
        this.completeBacktestAttemptUseCase = mock(CompleteBacktestAttemptUseCase.class);
        this.candidateEvaluatedPublisher = mock(CandidateEvaluatedPublisher.class);
        this.deadLetterPublisher = mock(DeadLetterPublisher.class);
        this.messageReader = mock(RedisStreamMessageReader.class);
        this.workerProperties = new WorkerProperties(null, null, null, null, null, null, null, null);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        this.handler = new BacktestJobHandler(
                idempotencyGuard,
                experimentUseCase,
                prepareBacktestUseCase,
                completeBacktestAttemptUseCase,
                candidateEvaluatedPublisher,
                deadLetterPublisher,
                messageReader,
                workerProperties,
                objectMapper
        );
    }

    @Test
    void duplicateMessageIdIsSkippedWithoutInvokingExecutionUseCase() throws Exception {
        String msgId = "01J7K8M9N0P1Q2R3S4T5A6V7W1";
        String expId = "01J7K8M9N0P1Q2R3S4T5A6V7W2";
        String candId = "01J7K8M9N0P1Q2R3S4T5A6V7W3";
        String jId = "01J7K8M9N0P1Q2R3S4T5A6V7W4";

        // Pre-insert into processed_message table
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
            INSERT INTO platform.processed_message (consumer_name, message_id, processed_at, expires_at)
            VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7' DAY)
        """, workerProperties.consumer().backtestGroup(), msgId);

        BacktestJobPayload payload = new BacktestJobPayload(expId, jId, candId);
        MessageEnvelope<BacktestJobPayload> envelope = new MessageEnvelope<>(
                msgId, 1, MessageTypes.BACKTEST_JOB, Instant.now(), "corr-103", payload
        );
        String rawJson = objectMapper.writeValueAsString(envelope);

        MapRecord<String, String, String> record = MapRecord.create(
                workerProperties.streams().getBacktestJobsStream(),
                Map.of("messageId", msgId, "payload", rawJson)
        ).withId(RecordId.of("1700000000002-0"));

        handler.handle(record);

        verify(experimentUseCase, never()).startNextAttempt(any(), any());
        verify(prepareBacktestUseCase, never()).prepare(any());
        verify(messageReader).ack(any(), any(), eq(record.getId()));
    }
}
