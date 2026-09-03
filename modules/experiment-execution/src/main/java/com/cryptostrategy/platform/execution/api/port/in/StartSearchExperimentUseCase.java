package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Public owner boundary để khởi tạo atomic Experiment/Search graph. */
public interface StartSearchExperimentUseCase {
    Acceptance start(StartCommand command);

    record StartCommand(
            UUID ownerUserId,
            String idempotencyKey,
            String canonicalRequestHash,
            Instant receiptExpiresAt,
            Experiment experiment,
            ExperimentManifest manifest,
            Job searchJob,
            SearchRun searchRun,
            OutboxEvent searchRequest) {
        public StartCommand {
            Objects.requireNonNull(ownerUserId, "ownerUserId");
            idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
            canonicalRequestHash = requireText(canonicalRequestHash, "canonicalRequestHash");
            Objects.requireNonNull(receiptExpiresAt, "receiptExpiresAt");
            Objects.requireNonNull(experiment, "experiment");
            Objects.requireNonNull(manifest, "manifest");
            Objects.requireNonNull(searchJob, "searchJob");
            Objects.requireNonNull(searchRun, "searchRun");
            Objects.requireNonNull(searchRequest, "searchRequest");
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }
    }

    record Acceptance(ExperimentId experimentId, JobId searchJobId, String status, boolean replay) {
        public Acceptance {
            Objects.requireNonNull(experimentId, "experimentId");
            Objects.requireNonNull(searchJobId, "searchJobId");
            status = Objects.requireNonNull(status, "status");
            if (!"QUEUED".equals(status)) {
                throw new IllegalArgumentException("Start acceptance status must be QUEUED");
            }
        }
    }
}
