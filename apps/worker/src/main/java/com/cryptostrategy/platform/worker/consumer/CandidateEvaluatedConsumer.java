package com.cryptostrategy.platform.worker.consumer;

import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.engine.BacktestExecutionPipeline;
import com.cryptostrategy.platform.worker.infra.redis.RedisStreamMessageReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class CandidateEvaluatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(CandidateEvaluatedConsumer.class);

    private final RedisStreamMessageReader messageReader;
    private final MessageDispatcher messageDispatcher;
    private final BacktestExecutionPipeline executionPipeline;
    private final WorkerProperties workerProperties;

    public CandidateEvaluatedConsumer(
            RedisStreamMessageReader messageReader,
            MessageDispatcher messageDispatcher,
            BacktestExecutionPipeline executionPipeline,
            WorkerProperties workerProperties
    ) {
        this.messageReader = Objects.requireNonNull(messageReader, "messageReader cannot be null");
        this.messageDispatcher = Objects.requireNonNull(messageDispatcher, "messageDispatcher cannot be null");
        this.executionPipeline = Objects.requireNonNull(executionPipeline, "executionPipeline cannot be null");
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
    }

    @Scheduled(fixedDelayString = "${worker.consumer.ranking-poll-interval-ms:500}")
    public void pollCandidateEvaluated() {
        String streamKey = workerProperties.streams().getCandidateEvaluatedStream();
        String groupName = workerProperties.consumer().rankingGroup();
        String consumerName = workerProperties.consumer().consumerName();
        int batchSize = workerProperties.consumer().readBatchSize();

        try {
            // 1. Process pending messages if any
            List<MapRecord<String, String, String>> pendingRecords = messageReader.readPendingBatch(
                    streamKey,
                    groupName,
                    consumerName,
                    workerProperties.consumer().pendingBatchSize()
            );
            for (var record : pendingRecords) {
                executionPipeline.executeAsync(() -> messageDispatcher.dispatch(record));
            }

            // 2. Read new messages from group
            List<MapRecord<String, String, String>> newRecords = messageReader.readGroupBatch(
                    streamKey,
                    groupName,
                    consumerName,
                    batchSize,
                    workerProperties.consumer().pollTimeout()
            );
            for (var record : newRecords) {
                executionPipeline.executeAsync(() -> messageDispatcher.dispatch(record));
            }
        } catch (Exception ex) {
            log.error("Error polling CandidateEvaluated stream '{}': {}", streamKey, ex.getMessage());
        }
    }
}
