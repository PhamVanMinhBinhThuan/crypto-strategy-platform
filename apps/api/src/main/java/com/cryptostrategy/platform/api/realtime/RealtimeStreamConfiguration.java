package com.cryptostrategy.platform.api.realtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

/** Broadcast consumer: every API instance receives future transient events for its own clients. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RealtimeStreamProperties.class)
@ConditionalOnProperty(
        name = "platform.realtime.streams.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RealtimeStreamConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    StreamMessageListenerContainer<String, MapRecord<String, String, String>>
            realtimeStreamListenerContainer(
                    RedisConnectionFactory connectionFactory,
                    RealtimeStreamProperties properties,
                    WorkEventStreamConsumer consumer) {
        var options = StreamMessageListenerContainerOptions
                .<String, MapRecord<String, String, String>>builder()
                .pollTimeout(properties.pollTimeout())
                .errorHandler(consumer::onConsumerError)
                .build();
        var container = StreamMessageListenerContainer.create(connectionFactory, options);
        register(container, properties.progress(), consumer);
        register(container, properties.lifecycle(), consumer);
        register(container, properties.candidateEvaluated(), consumer);
        return container;
    }

    private static void register(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            String stream,
            WorkEventStreamConsumer consumer) {
        // These notifications are hints, not durable truth. Starting at '$' avoids replaying an
        // unbounded history; reconnect recovery comes from owner-authorized REST snapshots.
        container.receive(StreamOffset.create(stream, ReadOffset.latest()), consumer);
    }
}
