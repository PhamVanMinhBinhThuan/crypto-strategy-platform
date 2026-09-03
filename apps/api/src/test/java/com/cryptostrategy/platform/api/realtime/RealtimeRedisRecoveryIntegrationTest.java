package com.cryptostrategy.platform.api.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.AdditionalAnswers.delegatesTo;

import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Real Redis reads with an injected connection-acquisition outage; uses disposable streams. */
@EnabledIfEnvironmentVariable(named = "F009_REDIS_SMOKE", matches = "true")
class RealtimeRedisRecoveryIntegrationTest {
    @Test
    void resumesNotificationDeliveryAfterRedisClientConnectionLoss() throws Exception {
        var factory = new LettuceConnectionFactory("localhost", 6379);
        factory.afterPropertiesSet();
        factory.start();
        var redis = new StringRedisTemplate(factory);
        var disconnected = new AtomicBoolean();
        var readerFactory = mock(RedisConnectionFactory.class, delegatesTo(factory));
        doAnswer(ignored -> {
            if (disconnected.get()) {
                throw new RedisConnectionFailureException("Simulated test connection outage");
            }
            return factory.getConnection();
        }).when(readerFactory).getConnection();
        String prefix = "f009-smoke-" + UUID.randomUUID();
        var properties = new RealtimeStreamProperties(true, prefix + ":progress", prefix + ":lifecycle",
                prefix + ":candidate", Duration.ofMillis(100));
        var bridge = new WorkEventBridge();
        var errors = new AtomicInteger();
        var consumer = spy(new WorkEventStreamConsumer(new ObjectMapper().findAndRegisterModules(),
                bridge, mock(GetLeaderboardUseCase.class)));
        doAnswer(ignored -> { errors.incrementAndGet(); return null; }).when(consumer).onConsumerError(any());
        var container = new RealtimeStreamConfiguration()
                .realtimeStreamListenerContainer(readerFactory, properties, consumer);
        var received = new CopyOnWriteArrayList<RealtimeMessageMapper.ServerEvent>();
        try (var handle = bridge.subscribe(WorkEventBridge.Kind.EXPERIMENT,
                "01J00000000000000000000001", "corr", "progress", received::add)) {
            assertThat(handle).isNotNull();
            container.start();
            deliver(redis, properties.lifecycle(), received, "RUNNING");
            disconnected.set(true);
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(errors.get()).isGreaterThanOrEqualTo(3));
            disconnected.set(false);
            deliver(redis, properties.lifecycle(), received, "COMPLETED");
            assertThat(received).extracting(event -> (String) event.payload().get("status"))
                    .contains("RUNNING", "COMPLETED");
        } finally {
            container.stop();
            redis.delete(List.of(properties.progress(), properties.lifecycle(), properties.candidateEvaluated()));
            factory.destroy();
        }
    }

    private static void deliver(StringRedisTemplate redis, String stream,
            List<RealtimeMessageMapper.ServerEvent> received, String status) {
        String payload = """
                {"messageId":"01J00000000000000000000009","messageType":"LIFECYCLE_NOTIFICATION",
                 "messageVersion":1,"occurredAt":"2026-09-03T00:00:00Z","correlationId":"corr",
                 "payload":{"aggregateType":"EXPERIMENT","aggregateId":"01J00000000000000000000001",
                            "experimentId":"01J00000000000000000000001",
                            "jobId":"01J00000000000000000000002","lifecycleEventType":"%s"}}
                """.formatted(status);
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            redis.opsForStream().add(stream, Map.of("payload", payload));
            assertThat(received).anyMatch(event -> status.equals(event.payload().get("status")));
        });
    }
}
