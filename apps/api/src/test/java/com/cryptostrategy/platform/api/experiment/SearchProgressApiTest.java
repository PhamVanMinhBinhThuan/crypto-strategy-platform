package com.cryptostrategy.platform.api.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.execution.api.port.in.GetSearchProgressUseCase.SearchProgressSnapshot;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.ExperimentStatus;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobType;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SearchProgressApiTest {
    private static final Instant NOW = Instant.parse("2026-09-05T00:00:00Z");

    @Test
    void exposesFailureInclusiveAuthoritativeCountersAndDurableTerminalReason() {
        ExperimentId experimentId = new ExperimentId("01J00000000000000000000601");
        Experiment experiment = new Experiment(experimentId,
                UUID.fromString("00000000-0000-4000-8000-000000000601"), "F-015",
                ExperimentStatus.COMPLETED, null, null, NOW, NOW.plusSeconds(60),
                null, null, NOW.minusSeconds(1));
        ExperimentManifest manifest = mock(ExperimentManifest.class);
        DatasetProvenanceSnapshot dataset = mock(DatasetProvenanceSnapshot.class);
        when(dataset.datasetVersionId()).thenReturn(
                new DatasetVersionId("01J00000000000000000000602"));
        when(manifest.datasetProvenance()).thenReturn(dataset);
        when(manifest.searchConfig()).thenReturn(Map.of("maximumCandidates", 100, "topK", 10));

        Job search = job("01J00000000000000000000603", experimentId, JobType.SEARCH);
        when(search.completedWork()).thenReturn(2);
        when(search.failedWork()).thenReturn(1);
        when(search.bestScore()).thenReturn(new BigDecimal("84.1"));
        when(search.startedAt()).thenReturn(NOW);
        List<Job> jobs = new ArrayList<>();
        jobs.add(search);
        for (int index = 0; index < 4; index++) {
            jobs.add(job("01J0000000000000000000061" + index, experimentId, JobType.BACKTEST));
        }

        var response = ReadDtos.ExperimentResponse.from(experiment, manifest, jobs,
                new SearchProgressSnapshot("NO_IMPROVEMENT"));

        assertThat(response.searchProgress()).isEqualTo(new ReadDtos.SearchProgressResponse(
                4, 1, 2, 1, 96, 100, 10, "84.1", NOW, "NO_IMPROVEMENT"));
    }

    private static Job job(String id, ExperimentId experimentId, JobType type) {
        Job job = mock(Job.class);
        when(job.jobId()).thenReturn(new JobId(id));
        when(job.experimentId()).thenReturn(experimentId);
        when(job.jobType()).thenReturn(type);
        return job;
    }
}
