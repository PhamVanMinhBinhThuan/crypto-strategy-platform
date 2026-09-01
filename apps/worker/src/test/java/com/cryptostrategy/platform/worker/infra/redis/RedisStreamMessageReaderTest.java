package com.cryptostrategy.platform.worker.infra.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisStreamMessageReaderTest {

    private StringRedisTemplate redisTemplate;
    private StreamOperations<String, Object, Object> streamOps;
    private RedisStreamMessageReader reader;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        streamOps = (StreamOperations<String, Object, Object>) mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOps);
        reader = new RedisStreamMessageReader(redisTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void readGroupBatchReadsFromLastConsumedOffset() {
        MapRecord<String, String, String> mockRecord = MapRecord.create("stream-1", Map.of("key", "val")).withId(RecordId.of("1700000000000-0"));
        when(streamOps.read(any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of(mockRecord));

        List<MapRecord<String, String, String>> records = reader.readGroupBatch("stream-1", "group-1", "worker-1", 10, Duration.ofSeconds(1));
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getId()).isEqualTo(RecordId.of("1700000000000-0"));
    }

    @Test
    void ackAcknowledgesRecordIds() {
        RecordId recordId = RecordId.of("1700000000000-0");
        reader.ack("stream-1", "group-1", recordId);
        verify(streamOps).acknowledge("stream-1", "group-1", recordId);
    }

    @Test
    void trimStreamCallsRedisTrim() {
        reader.trimStream("stream-1", 10000);
        verify(streamOps).trim(eq("stream-1"), eq(10000L), eq(true));
    }
}
