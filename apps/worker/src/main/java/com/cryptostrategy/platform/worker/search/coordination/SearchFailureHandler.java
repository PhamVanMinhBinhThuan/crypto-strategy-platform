package com.cryptostrategy.platform.worker.search.coordination;

import com.cryptostrategy.platform.worker.config.WorkerProperties;
import com.cryptostrategy.platform.worker.infra.redis.DeadLetterPublisher;
import com.cryptostrategy.platform.worker.infra.redis.LifecycleNotificationPublisher;
import java.util.Objects;

/** Chính sách retry hữu hạn và publication lỗi chỉ dùng mã/diagnostic reference an toàn. */
public final class SearchFailureHandler {
    private final int maxAttempts;
    private final DeadLetterPublisher deadLetters;
    private final LifecycleNotificationPublisher lifecycle;
    private final SearchObservability observability;

    public SearchFailureHandler(
            WorkerProperties properties,
            DeadLetterPublisher deadLetters,
            LifecycleNotificationPublisher lifecycle,
            SearchObservability observability) {
        this.maxAttempts = Objects.requireNonNull(properties, "properties").retry().maxAttempts();
        this.deadLetters = Objects.requireNonNull(deadLetters, "deadLetters");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.observability = Objects.requireNonNull(observability, "observability");
    }

    /** @return true khi caller còn được retry; false khi đã chuyển sang terminal dead-letter. */
    public boolean handle(Failure failure) {
        Objects.requireNonNull(failure, "failure");
        String code = SearchObservability.safeCode(failure.failureCode());
        observability.coordinationFailed(failure.experimentId(), failure.correlationId(), code);
        if (failure.attemptCount() < maxAttempts) return true;
        String reference = "search-failure:" + SearchObservability.safeId(failure.messageId());
        deadLetters.publishDeadLetter(failure.experimentId(), failure.jobId(), failure.candidateId(),
                failure.messageId(), "SEARCH_COORDINATION", code, reference, failure.attemptCount());
        lifecycle.publishLifecycleNotification("EXPERIMENT", failure.experimentId(), failure.experimentId(),
                failure.jobId(), failure.candidateId(), "SEARCH_FAILED", failure.correlationId());
        return false;
    }

    public record Failure(String experimentId, String jobId, String candidateId, String messageId,
                          String correlationId, String failureCode, int attemptCount) {
        public Failure {
            if (attemptCount < 1) throw new IllegalArgumentException("attemptCount must be positive");
        }
    }
}
