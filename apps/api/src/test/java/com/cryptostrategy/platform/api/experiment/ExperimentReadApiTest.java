package com.cryptostrategy.platform.api.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExperimentReadApiTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FOREIGN = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-09-02T00:00:00Z");
    private static final ExperimentId EXPERIMENT_ID = new ExperimentId("01J00000000000000000000001");

    @Test
    void returnsAuthoritativeExperimentJobAndDeterministicCandidatePage() {
        GetExperimentUseCase experiments = mock(GetExperimentUseCase.class);
        GetJobUseCase jobs = mock(GetJobUseCase.class);
        ListCandidatesUseCase candidates = mock(ListCandidatesUseCase.class);
        Experiment experiment = Experiment.create(EXPERIMENT_ID, OWNER, "search", null, null, NOW);
        ExperimentManifest manifest = manifest();
        Job job = Job.createSearchJob(
                new JobId("01J00000000000000000000002"), EXPERIMENT_ID, "corr", 2, NOW);
        when(experiments.getExperiment(OWNER, EXPERIMENT_ID)).thenReturn(Optional.of(experiment));
        when(experiments.getManifest(OWNER, EXPERIMENT_ID)).thenReturn(Optional.of(manifest));
        when(jobs.listJobs(OWNER, EXPERIMENT_ID)).thenReturn(List.of(job));
        when(candidates.listCandidates(OWNER, EXPERIMENT_ID, -1, "", 2)).thenReturn(List.of(
                candidate("01J00000000000000000000003", 0),
                candidate("01J00000000000000000000004", 1)));
        var controller = controller(experiments, jobs, candidates);
        var user = new AuthenticatedUserContext(OWNER, NOW.plusSeconds(60));

        var response = controller.getExperiment(user, EXPERIMENT_ID.value());
        var page = controller.listCandidates(user, EXPERIMENT_ID.value(), 1, null);

        assertThat(response.datasetId().value()).isEqualTo("01J00000000000000000000005");
        assertThat(response.jobIds()).containsExactly(job.jobId());
        assertThat(page.items()).extracting(ReadDtos.CandidateResponse::generationIndex)
                .containsExactly(0);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isNotBlank();
        verify(candidates).listCandidates(OWNER, EXPERIMENT_ID, -1, "", 2);
        verify(candidates, never()).listCandidates(OWNER, EXPERIMENT_ID);
    }

    @Test
    void foreignOwnerGetsTheSameInaccessibleOutcomeAsMissingOwner() {
        GetExperimentUseCase experiments = mock(GetExperimentUseCase.class);
        var controller = controller(
                experiments, mock(GetJobUseCase.class), mock(ListCandidatesUseCase.class));

        assertThatThrownBy(() -> controller.getExperiment(
                        new AuthenticatedUserContext(FOREIGN, NOW.plusSeconds(60)),
                        EXPERIMENT_ID.value()))
                .isInstanceOf(ResourceInaccessibleException.class);
    }

    private static ExperimentController controller(
            GetExperimentUseCase experiments,
            GetJobUseCase jobs,
            ListCandidatesUseCase candidates) {
        return new ExperimentController(
                mock(IdempotencyCommandExecutor.class),
                experiments,
                jobs,
                candidates,
                mock(StopExperimentUseCase.class),
                new PageRequestMapper());
    }

    private static CandidateDefinition candidate(String id, int generation) {
        return new CandidateDefinition(
                new CandidateId(id),
                EXPERIMENT_ID,
                generation,
                Map.of("period", generation + 1),
                Map.of("seed", 42),
                "sha256:" + Integer.toHexString(generation).repeat(64),
                NOW.plusSeconds(generation));
    }

    private static ExperimentManifest manifest() {
        ExperimentManifest manifest = mock(ExperimentManifest.class);
        DatasetProvenanceSnapshot provenance = mock(DatasetProvenanceSnapshot.class);
        when(provenance.datasetVersionId())
                .thenReturn(new DatasetVersionId("01J00000000000000000000005"));
        when(manifest.datasetProvenance()).thenReturn(provenance);
        return manifest;
    }
}
