package com.cryptostrategy.platform.worker.infra.redis;

import com.cryptostrategy.platform.worker.config.WorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class RedisStreamTopologyInitializer {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamTopologyInitializer.class);

    private final StringRedisTemplate redisTemplate;
    private final WorkerProperties workerProperties;

    public RedisStreamTopologyInitializer(StringRedisTemplate redisTemplate, WorkerProperties workerProperties) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate cannot be null");
        this.workerProperties = Objects.requireNonNull(workerProperties, "workerProperties cannot be null");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeTopology() {
        var streams = workerProperties.streams();
        var consumer = workerProperties.consumer();

        // 1. Backtest Jobs stream with backtest consumer group
        ensureConsumerGroup(streams.getBacktestJobsStream(), consumer.backtestGroup());

        // 2. Candidate Evaluated stream with ranking consumer group
        ensureConsumerGroup(streams.getCandidateEvaluatedStream(), consumer.rankingGroup());

        // Search completion observes the same events independently from ranking.
        ensureConsumerGroup(streams.getCandidateEvaluatedStream(), consumer.searchGroup());

        // 3. Search Request dùng group riêng, không chia pending-entry list với Ranking.
        ensureConsumerGroup(streams.getSearchRequestsStream(), consumer.searchGroup());

        // 4. Other streams initialized
        List<String> otherStreams = List.of(
                streams.getDeadLetterStream(),
                streams.getProgressEventsStream(),
                streams.getLifecycleEventsStream()
        );
        for (String streamKey : otherStreams) {
            ensureConsumerGroup(streamKey, consumer.backtestGroup());
        }
    }

    public void ensureConsumerGroup(String streamKey, String groupName) {
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0-0"), groupName);
            log.info("Created consumer group '{}' on stream '{}'", groupName, streamKey);
        } catch (RedisSystemException ex) {
            if (ex.getMessage() != null && (ex.getMessage().contains("BUSYGROUP") || ex.getMessage().contains("already exists"))) {
                log.debug("Consumer group '{}' already exists on stream '{}'", groupName, streamKey);
            } else {
                log.warn("Error creating consumer group '{}' on stream '{}': {}", groupName, streamKey, ex.getMessage());
            }
        } catch (Exception ex) {
            log.warn("Unexpected exception initializing stream '{}' group '{}': {}", streamKey, groupName, ex.getMessage());
        }
    }
}
