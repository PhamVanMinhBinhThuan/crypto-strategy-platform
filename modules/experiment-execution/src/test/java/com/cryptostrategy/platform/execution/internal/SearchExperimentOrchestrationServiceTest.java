package com.cryptostrategy.platform.execution.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase.StartCommand;
import com.cryptostrategy.platform.execution.api.port.out.SearchExperimentTransactionGateway;
import com.cryptostrategy.platform.execution.api.port.out.StartSearchGraphResult;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SearchExperimentOrchestrationServiceTest {
    private static final UUID OWNER = UUID.fromString("93000000-0000-4000-8000-000000000010");
    private static final ExperimentId EXPERIMENT_ID = new ExperimentId("63000000000000000000000001");
    private static final JobId JOB_ID = new JobId("63000000000000000000000002");

    @Test
    void mapsCreatedAndReplayOutcomesToTheStableAcceptance() {
        SearchExperimentTransactionGateway gateway = mock(SearchExperimentTransactionGateway.class);
        when(gateway.start(any())).thenReturn(
                new StartSearchGraphResult(StartSearchGraphResult.Status.CREATED, EXPERIMENT_ID, JOB_ID),
                new StartSearchGraphResult(StartSearchGraphResult.Status.REPLAY, EXPERIMENT_ID, JOB_ID));
        var service = new SearchExperimentOrchestrationService(gateway);

        assertThat(service.start(command(OWNER)).replay()).isFalse();
        assertThat(service.start(command(OWNER)).replay()).isTrue();
    }

    @Test
    void mapsGatewayHashConflictToThePublishedDomainFailure() {
        SearchExperimentTransactionGateway gateway = mock(SearchExperimentTransactionGateway.class);
        when(gateway.start(any())).thenReturn(StartSearchGraphResult.conflict());

        assertThatThrownBy(() -> new SearchExperimentOrchestrationService(gateway).start(command(OWNER)))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void rejectsAnOwnerMismatchBeforeOpeningTheCompositeTransaction() {
        SearchExperimentTransactionGateway gateway = mock(SearchExperimentTransactionGateway.class);

        assertThatThrownBy(() -> new SearchExperimentOrchestrationService(gateway)
                        .start(command(UUID.fromString("93000000-0000-4000-8000-000000000011"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner graph");
        verifyNoInteractions(gateway);
    }

    private static StartCommand command(UUID owner) {
        Experiment experiment = mock(Experiment.class);
        when(experiment.experimentId()).thenReturn(EXPERIMENT_ID);
        when(experiment.ownerUserId()).thenReturn(OWNER);
        ExperimentManifest manifest = mock(ExperimentManifest.class);
        when(manifest.experimentId()).thenReturn(EXPERIMENT_ID);
        Job job = mock(Job.class);
        when(job.experimentId()).thenReturn(EXPERIMENT_ID);
        when(job.jobId()).thenReturn(JOB_ID);
        SearchRun run = mock(SearchRun.class);
        when(run.experimentRef()).thenReturn(EXPERIMENT_ID.value());
        when(run.searchJobRef()).thenReturn(JOB_ID.value());
        OutboxEvent outbox = mock(OutboxEvent.class);
        when(outbox.aggregateId()).thenReturn(JOB_ID.value());
        return new StartCommand(owner, "key", "sha256:request", Instant.parse("2026-09-04T00:00:00Z"),
                experiment, manifest, job, run, outbox);
    }
}
