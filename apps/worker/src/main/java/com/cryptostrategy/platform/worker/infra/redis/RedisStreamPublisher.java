package com.cryptostrategy.platform.worker.infra.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class RedisStreamPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamPublisher.class);

    private final StringRedisTemplate redisTemplate;

    public RedisStreamPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate cannot be null");
    }

    public RecordId publish(String streamKey, String messageId, String payload, Map<String, String> headers) {
        Objects.requireNonNull(streamKey, "streamKey cannot be null");
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");

        Map<String, String> body = new HashMap<>();
        body.put("messageId", messageId);
        body.put("payload", payload);
        if (headers != null) {
            headers.forEach((k, v) -> body.put("header:" + k, v));
        }

        StringRecord record = StreamRecords.string(body).withStreamKey(streamKey);
        RecordId recordId = redisTemplate.opsForStream().add(record);
        log.debug("Published message '{}' to stream '{}' with recordId '{}'", messageId, streamKey, recordId);
        return recordId;
    }
}
