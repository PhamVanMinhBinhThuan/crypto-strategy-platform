package com.cryptostrategy.platform.search.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cryptostrategy.platform.search.api.model.CoordinationDecisionId;
import com.cryptostrategy.platform.search.api.model.CoordinationDecisionType;
import com.cryptostrategy.platform.search.api.model.GeneratorId;
import com.cryptostrategy.platform.search.api.model.GeneratorDescriptor;
import com.cryptostrategy.platform.search.api.model.GeneratorState;
import com.cryptostrategy.platform.search.api.model.GeneratorVersion;
import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.model.SearchRunMode;
import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import com.cryptostrategy.platform.search.api.model.SearchSpace;
import com.cryptostrategy.platform.search.api.model.SearchStopConditions;
import com.cryptostrategy.platform.search.api.model.SearchExperimentId;
import com.cryptostrategy.platform.search.api.model.SearchJobId;
import com.cryptostrategy.platform.search.api.port.in.StrategyGenerator;
import com.cryptostrategy.platform.search.api.port.in.StrategyGeneratorRegistry;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SearchModelContractTest {
    private static final String ULID_A = "01J7K8M9N0P1Q2R3S4T5A6V7W8";
    private static final String ULID_B = "01J7K8M9N0P1Q2R3S4T5A6V7W9";

    @Test
    void identitiesAndVersionsAreStronglyTypedAndValidated() {
        assertThat(new SearchRunId(ULID_A).value()).isEqualTo(ULID_A);
        assertThat(new CoordinationDecisionId(ULID_B).value()).isEqualTo(ULID_B);
        assertThat(new GeneratorId("random-search").value()).isEqualTo("random-search");
        assertThat(GeneratorVersion.parse("1.0.0").toString()).isEqualTo("1.0.0");

        assertThatThrownBy(() -> new SearchRunId("not-an-ulid"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeneratorId("Random Search"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeneratorVersion.parse("latest"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchSpaceCanonicalizesParameterNamesOptionsAndExactDecimals() {
        SearchParameterDomain periods = new SearchParameterDomain(
                ParameterType.INTEGER,
                List.of(
                        new StrategyParameterValue.IntegerValue(20),
                        new StrategyParameterValue.IntegerValue(5)));
        SearchParameterDomain thresholds = new SearchParameterDomain(
                ParameterType.DECIMAL,
                List.of(
                        new StrategyParameterValue.DecimalValue(new BigDecimal("0.20")),
                        new StrategyParameterValue.DecimalValue(new BigDecimal("0.1"))));

        SearchSpace space = new SearchSpace(Map.of("threshold", thresholds, "period", periods));

        assertThat(new ArrayList<>(space.parameters().keySet()))
                .containsExactly("period", "threshold");
        assertThat(space.parameters().get("period").options())
                .extracting(StrategyParameterValue::canonicalText)
                .containsExactly("5", "20");
        assertThat(space.parameters().get("threshold").options())
                .extracting(StrategyParameterValue::canonicalText)
                .containsExactly("0.1", "0.2");

        assertThatThrownBy(() -> new SearchParameterDomain(
                ParameterType.DECIMAL,
                List.of(
                        new StrategyParameterValue.DecimalValue(new BigDecimal("1.0")),
                        new StrategyParameterValue.DecimalValue(new BigDecimal("1.00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void stopConditionsAndLifecycleEnumsExposeStableInvariants() {
        SearchStopConditions conditions = new SearchStopConditions(25, Duration.ofMinutes(10));
        assertThat(conditions.maximumCandidates()).isEqualTo(25);
        assertThat(conditions.maximumDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(SearchRunStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(SearchRunStatus.STOPPED.isTerminal()).isTrue();
        assertThat(SearchRunStatus.FAILED.isTerminal()).isTrue();
        assertThat(SearchRunStatus.RUNNING.isTerminal()).isFalse();
        assertThat(CoordinationDecisionType.values())
                .containsExactly(
                        CoordinationDecisionType.ALLOCATED,
                        CoordinationDecisionType.DUPLICATE_SKIPPED,
                        CoordinationDecisionType.STOP_REACHED,
                        CoordinationDecisionType.FAILED);

        assertThatThrownBy(() -> new SearchStopConditions(0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchStopConditions(1, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generatorAndDurableStateBoundariesRemainPublishedInterfaces() {
        assertThat(StrategyGenerator.class.isInterface()).isTrue();
        assertThat(StrategyGeneratorRegistry.class.isInterface()).isTrue();
        assertThat(SearchRunStore.class.isInterface()).isTrue();
    }

    @Test
    void searchRunFreezesDeadlineAndEnforcesMonotonicTransitions() {
        Instant createdAt = Instant.parse("2026-09-03T00:00:00Z");
        GeneratorDescriptor descriptor = new GeneratorDescriptor(
                new GeneratorId("random-search"),
                GeneratorVersion.parse("1.0.0"),
                "random-state-v1",
                Set.of(ParameterType.INTEGER),
                "descriptor-fingerprint");
        SearchRun pending = SearchRun.pending(
                new SearchRunId(ULID_A),
                new SearchExperimentId(ULID_B),
                new SearchJobId("01J7K8M9N0P1Q2R3S4T5A6V7WA"),
                SearchRunMode.GENERATION,
                null,
                descriptor,
                42L,
                "space-fingerprint",
                new GeneratorState("random-state-v1", "{\"drawIndex\":0}", "state-0"),
                new SearchStopConditions(2, Duration.ofMinutes(10)),
                1,
                createdAt);

        SearchRun running = pending.start(createdAt.plusSeconds(5));
        SearchRun advanced = running.advance(
                new GeneratorState("random-state-v1", "{\"drawIndex\":1}", "state-1"),
                1,
                createdAt.plusSeconds(6));
        SearchRun completed = advanced.complete(createdAt.plusSeconds(7));

        assertThat(running.deadlineAt()).isEqualTo(createdAt.plusSeconds(605));
        assertThat(advanced.nextGenerationIndex()).isEqualTo(1);
        assertThat(advanced.version()).isEqualTo(2);
        assertThat(completed.status()).isEqualTo(SearchRunStatus.COMPLETED);
        assertThat(completed.complete(createdAt.plusSeconds(8))).isSameAs(completed);
        assertThatThrownBy(() -> completed.advance(
                new GeneratorState("random-state-v1", "{}", "state-2"), 2,
                createdAt.plusSeconds(8)))
                .isInstanceOf(IllegalStateException.class);
    }
}
