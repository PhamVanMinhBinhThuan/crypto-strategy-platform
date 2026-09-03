package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;

import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * Biên ứng dụng chỉ dành cho Worker tin cậy. Message chỉ kích hoạt việc đối soát;
 * mọi bộ đếm và quyết định vòng đời phải được đọc lại từ durable storage.
 */
public interface TrustedSearchCoordinationUseCase {
    CoordinationOutcome reconcileCompletion(CompletionTrigger trigger);

    CoordinationOutcome reconcileRun(ReconciliationTrigger trigger);

    CoordinationOutcome requestStop(StopTrigger trigger);

    record CompletionTrigger(
            String messageId,
            ExperimentId experimentId,
            CandidateId candidateId,
            JobId backtestJobId,
            Instant observedAt,
            String correlationId) {
        public CompletionTrigger {
            messageId = requireText(messageId, "messageId");
            Objects.requireNonNull(experimentId, "experimentId");
            Objects.requireNonNull(candidateId, "candidateId");
            Objects.requireNonNull(backtestJobId, "backtestJobId");
            Objects.requireNonNull(observedAt, "observedAt");
            correlationId = requireText(correlationId, "correlationId");
        }
    }

    record ReconciliationTrigger(ExperimentId experimentId, Instant observedAt, String correlationId) {
        public ReconciliationTrigger {
            Objects.requireNonNull(experimentId, "experimentId");
            Objects.requireNonNull(observedAt, "observedAt");
            correlationId = requireText(correlationId, "correlationId");
        }
    }

    record StopTrigger(ExperimentId experimentId, Instant requestedAt, String correlationId) {
        public StopTrigger {
            Objects.requireNonNull(experimentId, "experimentId");
            Objects.requireNonNull(requestedAt, "requestedAt");
            correlationId = requireText(correlationId, "correlationId");
        }
    }

    record CoordinationOutcome(
            SearchRunId searchRunId,
            int allocatedWork,
            int completedWork,
            int failedWork,
            SearchRunStatus status,
            Decision decision) {
        public CoordinationOutcome {
            Objects.requireNonNull(searchRunId, "searchRunId");
            if (allocatedWork < 0 || completedWork < 0 || failedWork < 0
                    || completedWork + failedWork > allocatedWork) {
                throw new IllegalArgumentException("authoritative counters are inconsistent");
            }
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(decision, "decision");
        }
    }

    enum Decision {
        FILL_AVAILABLE_SLOTS,
        WAIT_FOR_COMPLETIONS,
        COMPLETE,
        STOP,
        FAIL,
        ALREADY_TERMINAL
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
