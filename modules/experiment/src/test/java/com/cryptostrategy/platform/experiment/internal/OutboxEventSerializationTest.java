package com.cryptostrategy.platform.experiment.internal;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.experiment.api.provenance.DatasetProvenanceSnapshot;
import com.cryptostrategy.platform.domain.api.market.DatasetVersionId;
import com.cryptostrategy.platform.experiment.api.provenance.StrategyProvenanceSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventSerializationTest {

    private final UUID ownerUserId = UUID.randomUUID();
    private final ExperimentId experimentId = ExperimentId.generate();
    private final JobId jobId = JobId.generate();
    private final CandidateId candidateId = CandidateId.generate();

    @Test
    @DisplayName("ExperimentQueued outbox event contains required payload and routing context")
    void experimentQueuedEvent() {
        Experiment exp = Experiment.create(experimentId, ownerUserId, "Exp 1", null, null, Instant.now());
        ExperimentManifest manifest = new ExperimentManifest(
                experimentId,
                "v1",
                new DatasetProvenanceSnapshot(new DatasetVersionId("01ARZ3NDEKTSV4RRFFQ69G5FAV"), "candle-v1", "sha256:123", "BINANCE", "BTC/USDT", "1m", "v1", Instant.EPOCH, Instant.EPOCH, 0),
                ProvenanceTestFixtures.single("sma", "1.0", Map.of(), null),
                Map.of(), Map.of(), Map.of(), null, "0.1", "git", "sha256:fingerprint", Instant.now()
        );

        Instant now = Instant.now();
        OutboxEvent event = OutboxEvents.experimentQueued(exp, manifest, now);

        assertThat(event.aggregateType()).isEqualTo("EXPERIMENT");
        assertThat(event.aggregateId()).isEqualTo(experimentId.value());
        assertThat(event.eventType()).isEqualTo("ExperimentQueued");
        assertThat(event.payloadJson()).contains(experimentId.value()).contains("sha256:fingerprint");
    }

    @Test
    @DisplayName("JobQueued, JobCancelRequested, and JobCancelled outbox events serialize correctly")
    void jobOutboxEvents() {
        Job job = Job.createBacktestJob(jobId, experimentId, candidateId, "corr-outbox", Instant.now());
        Instant now = Instant.now();

        OutboxEvent queued = OutboxEvents.jobQueued(job, now);
        assertThat(queued.eventType()).isEqualTo("JobQueued");
        assertThat(queued.payloadJson()).contains(jobId.value()).contains(candidateId.value());

        OutboxEvent cancelRequested = OutboxEvents.jobCancelRequested(job, now);
        assertThat(cancelRequested.eventType()).isEqualTo("JobCancelRequested");

        OutboxEvent cancelled = OutboxEvents.jobCancelled(job, now);
        assertThat(cancelled.eventType()).isEqualTo("JobCancelled");
    }
}
