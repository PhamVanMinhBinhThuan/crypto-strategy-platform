package com.cryptostrategy.platform.backtesting.internal;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultSummary;
import com.cryptostrategy.platform.backtesting.api.port.in.GetBacktestResultUseCase;
import com.cryptostrategy.platform.backtesting.api.port.out.BacktestResultReader;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class BacktestResultQueryService implements GetBacktestResultUseCase {
    private final BacktestResultReader results;

    public BacktestResultQueryService(BacktestResultReader results) {
        this.results = Objects.requireNonNull(results, "results");
    }

    @Override
    public Optional<BacktestResult> getByResultId(BacktestResultId resultId) {
        return results.findById(Objects.requireNonNull(resultId, "resultId"));
    }

    @Override
    public Optional<BacktestResult> getByJobId(JobId jobId) {
        return results.findByJobId(Objects.requireNonNull(jobId, "jobId"));
    }

    @Override
    public List<BacktestResultSummary> listRecent(
            UUID ownerUserId, Instant beforeCompletedAt, String beforeResultId, int limit) {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        if ((beforeCompletedAt == null) != (beforeResultId == null)) {
            throw new IllegalArgumentException("Backtest result cursor boundary is incomplete");
        }
        if (limit < 1 || limit > 101) {
            throw new IllegalArgumentException("Backtest result page limit is invalid");
        }
        return results.listRecent(ownerUserId, beforeCompletedAt, beforeResultId, limit);
    }

    @Override
    public long count(UUID ownerUserId) {
        return results.count(Objects.requireNonNull(ownerUserId, "ownerUserId"));
    }
}
