package com.cryptostrategy.platform.execution.api.port.out;

import com.cryptostrategy.platform.execution.api.ReproductionVerificationId;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.search.api.model.SearchRunId;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.provenance.SentimentProvenanceSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Composite durable boundary cho source validation và atomic reproduction initialization. */
public interface SearchReproductionGateway {
    Optional<SourceSnapshot> loadSource(UUID ownerUserId, ExperimentId sourceExperimentId);
    Result create(CreateCommand command);

    record SourceSnapshot(ExperimentId sourceExperimentId, String status,
            boolean evidenceComplete, List<String> orderedCandidateIds,
            Optional<SentimentProvenanceSnapshot> sentimentProvenance) {
        public SourceSnapshot {
            Objects.requireNonNull(sourceExperimentId, "sourceExperimentId");
            Objects.requireNonNull(status, "status");
            orderedCandidateIds = List.copyOf(orderedCandidateIds);
            sentimentProvenance = Objects.requireNonNull(
                    sentimentProvenance, "sentimentProvenance");
        }

        public SourceSnapshot(ExperimentId sourceExperimentId, String status,
                boolean evidenceComplete, List<String> orderedCandidateIds) {
            this(sourceExperimentId, status, evidenceComplete, orderedCandidateIds, Optional.empty());
        }
    }

    record CandidateCopy(CandidateId sourceCandidateId, CandidateId candidateId, JobId backtestJobId,
            String outboxEventId, String messageId) {}

    record CreateCommand(UUID ownerUserId, ExperimentId sourceExperimentId, ExperimentId experimentId,
            JobId searchJobId, SearchRunId searchRunId, ReproductionVerificationId verificationId, String name,
            String idempotencyKey, String requestHash, String correlationId,
            Instant requestedAt, Instant receiptExpiresAt, List<CandidateCopy> candidates) {
        public CreateCommand { candidates = List.copyOf(candidates); }
    }

    record Result(Status status, ExperimentId experimentId, JobId searchJobId) {
        public enum Status { CREATED, REPLAY, CONFLICT }
    }
}
