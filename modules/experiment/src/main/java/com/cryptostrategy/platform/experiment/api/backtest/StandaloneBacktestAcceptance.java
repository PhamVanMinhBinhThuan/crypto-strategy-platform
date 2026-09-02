package com.cryptostrategy.platform.experiment.api.backtest;

import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.job.JobStatus;
import java.util.Objects;

/** Original durable outcome returned for both first acceptance and safe replay. */
public record StandaloneBacktestAcceptance(
        StandaloneBacktest backtest,
        JobId jobId,
        JobStatus acceptedStatus,
        boolean replayed) {
    public StandaloneBacktestAcceptance {
        Objects.requireNonNull(backtest, "backtest cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(acceptedStatus, "acceptedStatus cannot be null");
        if (!backtest.jobId().equals(jobId)) {
            throw new IllegalArgumentException("Backtest acceptance Job identity is inconsistent");
        }
        if (acceptedStatus != JobStatus.QUEUED) {
            throw new IllegalArgumentException("A newly accepted Backtest Job must be QUEUED");
        }
    }
}
