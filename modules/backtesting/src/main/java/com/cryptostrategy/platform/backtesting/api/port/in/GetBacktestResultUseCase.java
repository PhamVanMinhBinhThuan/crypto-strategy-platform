package com.cryptostrategy.platform.backtesting.api.port.in;

import com.cryptostrategy.platform.backtesting.api.model.BacktestResult;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultSummary;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Reads the immutable result associated with a successfully completed Backtest Job. */
public interface GetBacktestResultUseCase {
    Optional<BacktestResult> getByResultId(BacktestResultId resultId);

    Optional<BacktestResult> getByJobId(JobId jobId);

    default List<BacktestResultSummary> listRecent(
            UUID ownerUserId, Instant beforeCompletedAt, String beforeResultId, int limit) {
        return List.of();
    }

    default long count(UUID ownerUserId) { return 0; }
}
