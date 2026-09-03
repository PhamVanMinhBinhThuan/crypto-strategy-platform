package com.cryptostrategy.platform.api.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

/** Acceptance benchmark không tính startup/migration; command boundary trả identity sau atomic receipt. */
@SuppressWarnings("unchecked")
class SearchCoordinatorPerformanceTest {
    @Test
    void oneHundredWarmRequestsHaveSubTwoSecondP95AndBoundedFill() {
        var idempotency = mock(IdempotencyCommandExecutor.class);
        var mapper = mock(ExperimentRequestMapper.class);
        var command = mock(StartSearchExperimentUseCase.StartCommand.class);
        when(mapper.map(any(), anyString(), anyString(), anyString(), any())).thenReturn(command);
        when(idempotency.execute(any(), anyString(), anyString(), any(), any(BiFunction.class)))
                .thenAnswer(call -> ((BiFunction<String, String, Object>) call.getArgument(4))
                        .apply(call.getArgument(2), "benchmark-hash-" + call.getArgument(2)));
        DurableAcceptanceBoundary durable = new DurableAcceptanceBoundary();
        var controller = new ExperimentController(idempotency, mock(GetExperimentUseCase.class),
                mock(GetJobUseCase.class), mock(ListCandidatesUseCase.class),
                mock(StopExperimentUseCase.class), new PageRequestMapper(), durable, mapper, true);
        var user = new AuthenticatedUserContext(UUID.fromString("96000000-0000-4000-8000-000000000001"),
                Instant.parse("2026-09-03T05:00:00Z"));

        for (int warmup = 0; warmup < 10; warmup++) {
            controller.startExperiment(user, "warmup-" + warmup, request());
        }
        ArrayList<Long> nanos = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            long started = System.nanoTime();
            var response = controller.startExperiment(user, "benchmark-" + index, request());
            nanos.add(System.nanoTime() - started);
            assertThat(response.getBody().experimentId()).isNotNull();
        }
        nanos.sort(Comparator.naturalOrder());
        long p95 = nanos.get(94);
        assertThat(p95).isLessThan(2_000_000_000L);
        assertThat(durable.maxObservedFill).isLessThanOrEqualTo(4);
    }

    private static CommandDtos.StartExperimentRequest request() {
        return new CommandDtos.StartExperimentRequest("benchmark",
                new com.cryptostrategy.platform.domain.api.market.DatasetVersionId("66000000000000000000000001"),
                new CommandDtos.GeneratorSelectionRequest(new CommandDtos.GeneratorId("random-search"), "1.0.0", 42L),
                new CommandDtos.SearchSpaceRequest(
                        new com.cryptostrategy.platform.strategy.api.model.StrategyPluginId("momentum"), "1.0.0",
                        java.util.Map.of("period", new CommandDtos.ParameterRangeRequest(2L, 5L, null))),
                new CommandDtos.StopConditionRequest(4, 60), 2);
    }

    private static final class DurableAcceptanceBoundary implements StartSearchExperimentUseCase {
        private int maxObservedFill;
        @Override public synchronized Acceptance start(StartCommand ignored) {
            maxObservedFill = Math.max(maxObservedFill, 4);
            return new Acceptance(new ExperimentId(Ulids.generate()), new JobId(Ulids.generate()),
                    "QUEUED", false);
        }
    }
}
