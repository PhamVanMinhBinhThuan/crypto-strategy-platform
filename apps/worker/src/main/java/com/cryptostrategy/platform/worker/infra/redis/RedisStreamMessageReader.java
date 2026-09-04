package com.cryptostrategy.platform.worker.infra.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class RedisStreamMessageReader {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamMessageReader.class);

    private final StringRedisTemplate redisTemplate;

    public RedisStreamMessageReader(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate cannot be null");
    }

    public List<MapRecord<String, String, String>> readGroupBatch(
            String streamKey,
            String consumerGroup,
            String consumerName,
            int count,
            Duration block
    ) {
        Objects.requireNonNull(streamKey, "streamKey cannot be null");
        Objects.requireNonNull(consumerGroup, "consumerGroup cannot be null");
        Objects.requireNonNull(consumerName, "consumerName cannot be null");

        StreamReadOptions options = StreamReadOptions.empty()
                .count(Math.max(1, count))
                .block(block != null ? block : Duration.ofMillis(2000));

        Consumer consumer = Consumer.from(consumerGroup, consumerName);
        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

        List<MapRecord<String, Object, Object>> raw = redisTemplate.opsForStream().read(consumer, options, streamOffset);
        if (raw == null) {
            return List.of();
        }
        return raw.stream().map(this::convertRecord).collect(Collectors.toList());
    }

    public List<MapRecord<String, String, String>> readPendingBatch(
            String streamKey,
            String consumerGroup,
            String consumerName,
            int count
    ) {
        Objects.requireNonNull(streamKey, "streamKey cannot be null");
        Objects.requireNonNull(consumerGroup, "consumerGroup cannot be null");
        Objects.requireNonNull(consumerName, "consumerName cannot be null");

        StreamReadOptions options = StreamReadOptions.empty().count(Math.max(1, count));
        Consumer consumer = Consumer.from(consumerGroup, consumerName);
        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.from("0-0"));

        List<MapRecord<String, Object, Object>> raw = redisTemplate.opsForStream().read(consumer, options, streamOffset);
        if (raw == null) {
            return List.of();
        }
        return raw.stream().map(this::convertRecord).collect(Collectors.toList());
    }

    /**
     * Transfers messages abandoned by another consumer to the current worker after
     * the configured idle boundary. Reading {@code 0-0} only sees pending messages
     * already owned by the same consumer, so it cannot recover work from a crashed
     * worker with a different instance name.
     */
    public List<MapRecord<String, String, String>> claimStaleBatch(
            String streamKey,
            String consumerGroup,
            String consumerName,
            Duration minimumIdleTime,
            int count
    ) {
        Objects.requireNonNull(streamKey, "streamKey cannot be null");
        Objects.requireNonNull(consumerGroup, "consumerGroup cannot be null");
        Objects.requireNonNull(consumerName, "consumerName cannot be null");
        Objects.requireNonNull(minimumIdleTime, "minimumIdleTime cannot be null");
        if (minimumIdleTime.isNegative()) {
            throw new IllegalArgumentException("minimumIdleTime cannot be negative");
        }

        int batchSize = Math.max(1, count);
        var pending = redisTemplate.opsForStream()
                .pending(streamKey, consumerGroup, Range.unbounded(), batchSize);
        if (pending == null || pending.isEmpty()) {
            return List.of();
        }
        RecordId[] eligible = pending.stream()
                .filter(message -> !message.getConsumerName().equals(consumerName))
                .filter(message -> message.getElapsedTimeSinceLastDelivery().compareTo(minimumIdleTime) >= 0)
                .map(message -> message.getId())
                .toArray(RecordId[]::new);
        if (eligible.length == 0) {
            return List.of();
        }
        List<MapRecord<String, Object, Object>> claimed = redisTemplate.opsForStream()
                .claim(streamKey, consumerGroup, consumerName, minimumIdleTime, eligible);
        if (claimed == null) {
            return List.of();
        }
        return claimed.stream().map(this::convertRecord).collect(Collectors.toList());
    }

    public void ack(String streamKey, String consumerGroup, RecordId... recordIds) {
        Objects.requireNonNull(streamKey, "streamKey cannot be null");
        Objects.requireNonNull(consumerGroup, "consumerGroup cannot be null");
        if (recordIds != null && recordIds.length > 0) {
            redisTemplate.opsForStream().acknowledge(streamKey, consumerGroup, recordIds);
        }
    }

    public PendingMessagesSummary pendingSummary(String streamKey, String consumerGroup) {
        Objects.requireNonNull(streamKey, "streamKey cannot be null");
        Objects.requireNonNull(consumerGroup, "consumerGroup cannot be null");
        return redisTemplate.opsForStream().pending(streamKey, consumerGroup);
    }

    public void trimStream(String streamKey, long maxLen) {
        Objects.requireNonNull(streamKey, "streamKey cannot be null");
        if (maxLen > 0) {
            redisTemplate.opsForStream().trim(streamKey, maxLen, true);
        }
    }

    private MapRecord<String, String, String> convertRecord(MapRecord<String, Object, Object> raw) {
        var stringMap = raw.getValue().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue())
                ));
        return MapRecord.create(raw.getStream(), stringMap).withId(raw.getId());
    }
}
