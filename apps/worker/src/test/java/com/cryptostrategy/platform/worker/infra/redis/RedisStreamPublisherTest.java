package com.cryptostrategy.platform.worker.infra.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisStreamPublisherTest {

    private StringRedisTemplate redisTemplate;
    private StreamOperations<String, Object, Object> streamOps;
    private RedisStreamPublisher publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        streamOps = (StreamOperations<String, Object, Object>) mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        publisher = new RedisStreamPublisher(redisTemplate);
    }

    @Test
    void publishesRecordWithStreamKeyMessageIdAndPayload() {
        RecordId expectedRecordId = RecordId.of("1700000000000-0");
        when(streamOps.add(any(StringRecord.class))).thenReturn(expectedRecordId);

        RecordId actual = publisher.publish("crypto.jobs.backtest.v1", "01J7K8M9N0P1Q2R3S4T5A6V7W1", "{\"data\":\"val\"}", Map.of("correlationId", "corr-1"));

        assertThat(actual).isEqualTo(expectedRecordId);
        verify(streamOps).add(any(StringRecord.class));
    }
}
