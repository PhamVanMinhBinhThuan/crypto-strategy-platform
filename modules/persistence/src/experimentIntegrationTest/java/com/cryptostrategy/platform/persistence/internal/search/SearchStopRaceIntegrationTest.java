package com.cryptostrategy.platform.persistence.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationResult;
import com.cryptostrategy.platform.execution.api.port.out.TrustedSearchCoordinationGateway;
import com.cryptostrategy.platform.persistence.internal.execution.JdbcSearchExperimentTransaction;
import com.cryptostrategy.platform.search.api.port.out.SearchRunClaim;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class SearchStopRaceIntegrationTest {
    @Test
    void durableStopFencePreventsEveryLaterCandidateCommit() throws Exception {
        var dataSource = SearchAllocationConcurrencyIntegrationTest.dataSource();
        var jdbc = new JdbcTemplate(dataSource);
        SearchAllocationConcurrencyIntegrationTest.seed(jdbc);
        try {
            var allocationGateway = new JdbcSearchExperimentTransaction(dataSource);
            var stopGateway = new JdbcTrustedSearchCoordinationGateway(dataSource);
            var running = SearchAllocationConcurrencyIntegrationTest.initialRun()
                    .start(SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(1));
            var claim = new SearchRunClaim(running, running.version());
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);
            try (var pool = Executors.newFixedThreadPool(2)) {
                var allocation = pool.submit(() -> {
                    ready.countDown();
                    release.await();
                    return SearchAllocationConcurrencyIntegrationTest.allocation(
                            allocationGateway, claim, "A", new CountDownLatch(0), new CountDownLatch(0)).call();
                });
                var stop = pool.submit(() -> {
                    ready.countDown();
                    release.await();
                    return stopGateway.commit(new TrustedSearchCoordinationGateway.Transition(
                            running.version(), running.requestStop(SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(2)),
                            0, 0, null));
                });
                ready.await();
                release.countDown();
                SearchAllocationResult allocationResult = allocation.get();
                boolean stopCommitted = stop.get();
                assertThat(stopCommitted || allocationResult.status() == SearchAllocationResult.Status.ALLOCATED).isTrue();
            }

            var durable = stopGateway.load(SearchAllocationConcurrencyIntegrationTest.EXPERIMENT).orElseThrow();
            if (!durable.run().status().name().equals("STOPPING")) {
                assertThat(stopGateway.commit(new TrustedSearchCoordinationGateway.Transition(
                        durable.run().version(), durable.run().requestStop(SearchAllocationConcurrencyIntegrationTest.NOW.plusSeconds(3)),
                        durable.completedWork(), durable.failedWork(), null))).isTrue();
            }
            int candidatesAtStop = jdbc.queryForObject(
                    "select count(*) from experiment.candidate_definition where experiment_id=?",
                    Integer.class, SearchAllocationConcurrencyIntegrationTest.EXPERIMENT);

            SearchAllocationResult afterStop = SearchAllocationConcurrencyIntegrationTest.allocation(
                    allocationGateway, claim, "B", new CountDownLatch(0), new CountDownLatch(0)).call();

            assertThat(afterStop.status()).isEqualTo(SearchAllocationResult.Status.STALE_FENCE);
            assertThat(jdbc.queryForObject(
                    "select count(*) from experiment.candidate_definition where experiment_id=?",
                    Integer.class, SearchAllocationConcurrencyIntegrationTest.EXPERIMENT)).isEqualTo(candidatesAtStop);
        } finally {
            SearchAllocationConcurrencyIntegrationTest.cleanup(jdbc);
        }
    }
}
