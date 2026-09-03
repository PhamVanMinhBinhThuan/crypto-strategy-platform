package com.cryptostrategy.platform.worker.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkerPropertiesTest {

    @Test
    void defaultValuesAreValidAndHorizonIsRespected() {
        WorkerProperties props = new WorkerProperties(null, null, null, null, null, null, null, null);
        assertThat(props.redis().host()).isEqualTo("localhost");
        assertThat(props.redis().port()).isEqualTo(6379);
        assertThat(props.redis().ssl()).isFalse();
        assertThat(props.streams().getBacktestJobsStream()).isEqualTo("backtest.jobs.v1");
        assertThat(props.streams().getCandidateEvaluatedStream()).isEqualTo("candidate.evaluated.v1");
        assertThat(props.streams().getDeadLetterStream()).isEqualTo("jobs.dead-letter.v1");
        assertThat(props.streams().getProgressEventsStream()).isEqualTo("progress.events.v1");
        assertThat(props.streams().getLifecycleEventsStream()).isEqualTo("lifecycle.events.v1");
        assertThat(props.streams().getSearchRequestsStream()).isEqualTo("search.requests.v1");
        assertThat(props.consumer().searchGroup()).isEqualTo("search-coordinators");
        assertThat(props.concurrency().backtest()).isEqualTo(4);
        assertThat(props.concurrency().search()).isEqualTo(2);
        assertThat(props.concurrency().maxInFlightPerExperiment()).isEqualTo(4);
        assertThat(props.reconciliation().searchInterval()).isEqualTo(Duration.ofSeconds(5));
        assertThat(props.reconciliation().searchBatchSize()).isEqualTo(50);
        assertThat(props.retry().maxAttempts()).isEqualTo(3);
        assertThat(props.processedMessage().ttl()).isGreaterThan(props.execution().timeout().plus(props.reconciliation().staleGracePeriod()));
    }

    @Test
    void customStreamPrefixingWorks() {
        WorkerProperties.Streams streams = new WorkerProperties.Streams(
                "prod",
                "backtest.jobs.v1",
                "candidate.evaluated.v1",
                "jobs.dead-letter.v1",
                "progress.events.v1",
                "lifecycle.events.v1",
                "search.requests.v1"
        );
        assertThat(streams.getBacktestJobsStream()).isEqualTo("prod.backtest.jobs.v1");
        assertThat(streams.getCandidateEvaluatedStream()).isEqualTo("prod.candidate.evaluated.v1");
        assertThat(streams.getDeadLetterStream()).isEqualTo("prod.jobs.dead-letter.v1");
        assertThat(streams.getProgressEventsStream()).isEqualTo("prod.progress.events.v1");
        assertThat(streams.getLifecycleEventsStream()).isEqualTo("prod.lifecycle.events.v1");
    }

    @Test
    void bindsFromConfigurationPropertySource() {
        Map<String, Object> map = new HashMap<>();
        map.put("worker.redis.host", "redis.internal");
        map.put("worker.redis.port", 6380);
        map.put("worker.redis.password", "secret");
        map.put("worker.redis.ssl", true);
        map.put("worker.redis.timeout", "10s");
        map.put("worker.concurrency.backtest", 8);
        map.put("worker.consumer.search-group", "search-coordinators-test");
        map.put("worker.concurrency.search", 3);
        map.put("worker.concurrency.max-in-flight-per-experiment", 6);
        map.put("worker.reconciliation.search-interval", "7s");
        map.put("worker.reconciliation.search-batch-size", 25);
        map.put("worker.retry.max-attempts", 5);

        ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
        Binder binder = new Binder(source);
        WorkerProperties props = binder.bind("worker", WorkerProperties.class).get();

        assertThat(props.redis().host()).isEqualTo("redis.internal");
        assertThat(props.redis().port()).isEqualTo(6380);
        assertThat(props.redis().password()).isEqualTo("secret");
        assertThat(props.redis().ssl()).isTrue();
        assertThat(props.redis().timeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(props.concurrency().backtest()).isEqualTo(8);
        assertThat(props.consumer().searchGroup()).isEqualTo("search-coordinators-test");
        assertThat(props.concurrency().search()).isEqualTo(3);
        assertThat(props.concurrency().maxInFlightPerExperiment()).isEqualTo(6);
        assertThat(props.reconciliation().searchInterval()).isEqualTo(Duration.ofSeconds(7));
        assertThat(props.reconciliation().searchBatchSize()).isEqualTo(25);
        assertThat(props.retry().maxAttempts()).isEqualTo(5);
    }

    @Test
    void rejectsInvalidPort() {
        assertThatThrownBy(() -> new WorkerProperties.Redis("localhost", 70000, null, null, false, Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("redis.port");
    }

    @Test
    void rejectsInvalidProcessedMessageTtlAgainstRecoveryHorizon() {
        WorkerProperties.Execution exec = new WorkerProperties.Execution(Duration.ofHours(2), Duration.ofSeconds(30));
        WorkerProperties.Reconciliation recon = new WorkerProperties.Reconciliation(
                Duration.ofSeconds(1), 50,
                Duration.ofSeconds(30), Duration.ofMinutes(2),
                Duration.ofSeconds(30), Duration.ofHours(1),
                Duration.ofSeconds(10), 20,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5), 50
        );
        WorkerProperties.ProcessedMessage shortTtl = new WorkerProperties.ProcessedMessage(Duration.ofMinutes(30));

        assertThatThrownBy(() -> new WorkerProperties(null, null, null, null, null, exec, recon, shortTtl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("processedMessage.ttl must be strictly greater than execution recovery horizon");
    }
}
