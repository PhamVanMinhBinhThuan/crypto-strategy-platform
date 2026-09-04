package com.cryptostrategy.platform.worker.infra.redis;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cryptostrategy.platform.worker.config.WorkerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

class RedisStreamTopologyInitializerTest {
    @Test
    void createsIndependentRankingAndSearchGroupsForCandidateEvaluatedEvents() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        StreamOperations<String, Object, Object> streams = mock(StreamOperations.class);
        org.mockito.Mockito.when(redis.opsForStream()).thenReturn(streams);
        WorkerProperties properties = new WorkerProperties(null, null, null, null, null, null, null, null);

        new RedisStreamTopologyInitializer(redis, properties).initializeTopology();

        verify(streams, atLeastOnce()).createGroup(
                properties.streams().getCandidateEvaluatedStream(),
                ReadOffset.from("0-0"),
                properties.consumer().rankingGroup());
        verify(streams, atLeastOnce()).createGroup(
                properties.streams().getCandidateEvaluatedStream(),
                ReadOffset.from("0-0"),
                properties.consumer().searchGroup());
    }
}
