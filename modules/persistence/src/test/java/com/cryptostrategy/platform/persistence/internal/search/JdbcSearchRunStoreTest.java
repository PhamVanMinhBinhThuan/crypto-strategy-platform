package com.cryptostrategy.platform.persistence.internal.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.model.SearchRunMode;
import com.cryptostrategy.platform.search.api.model.SearchStopConditions;
import com.cryptostrategy.platform.search.api.port.out.SearchRunClaim;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcSearchRunStoreTest {
    @Test
    void fencedSaveReturnsFalseWhenExpectedVersionIsStale() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(0);
        JdbcSearchRunStore store = new JdbcSearchRunStore(jdbc);
        SearchRun running = pending().start(Instant.parse("2026-09-03T00:00:01Z"));
        SearchRun replacement = running.advance(
                new GeneratorState("random-state-v1", "{\"drawIndex\":1}", "state-1"),
                1,
                Instant.parse("2026-09-03T00:00:02Z"));

        assertThat(store.save(new SearchRunClaim(running, running.version()), replacement)).isFalse();
    }

    @Test
    void recoveryScanRejectsUnboundedRequests() {
        JdbcSearchRunStore store = new JdbcSearchRunStore(mock(JdbcTemplate.class));
        assertThatThrownBy(() -> store.findRecoverable(Instant.now(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SearchRun pending() {
        GeneratorDescriptor descriptor = new GeneratorDescriptor(
                new GeneratorId("random-search"), GeneratorVersion.parse("1.0.0"),
                "random-state-v1", Set.of(ParameterType.INTEGER), "descriptor-fingerprint");
        return SearchRun.pending(
                new SearchRunId("01J7K8M9N0P1Q2R3S4T5A6V7W8"),
                "01J7K8M9N0P1Q2R3S4T5A6V7W9",
                "01J7K8M9N0P1Q2R3S4T5A6V7WA",
                SearchRunMode.GENERATION,
                null,
                descriptor,
                42,
                "space-fingerprint",
                new GeneratorState("random-state-v1", "{\"drawIndex\":0}", "state-0"),
                new SearchStopConditions(2, Duration.ofMinutes(10)),
                1,
                Instant.parse("2026-09-03T00:00:00Z"));
    }
}
