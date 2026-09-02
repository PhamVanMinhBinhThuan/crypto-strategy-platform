package com.cryptostrategy.platform.api.backtest;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.backtesting.api.port.in.GetBacktestResultUseCase;
import com.cryptostrategy.platform.experiment.api.backtest.BacktestId;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.port.in.GetStandaloneBacktestUseCase;
import java.util.Objects;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/backtests")
public final class BacktestResultController {
    private final GetStandaloneBacktestUseCase backtests;
    private final GetBacktestResultUseCase results;

    public BacktestResultController(
            @Qualifier("getStandaloneBacktestUseCase") GetStandaloneBacktestUseCase backtests,
            GetBacktestResultUseCase results) {
        this.backtests = Objects.requireNonNull(backtests, "backtests");
        this.results = Objects.requireNonNull(results, "results");
    }

    @GetMapping("/{id}/result")
    public ResultDtos.BacktestResultResponse getResult(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String id) {
        BacktestId backtestId = new BacktestId(id);
        var backtest = backtests.getStandaloneBacktest(user.userId(), backtestId)
                .orElseThrow(BacktestResultController::inaccessible);
        var result = results.getByJobId(backtest.jobId())
                .orElseThrow(BacktestResultController::inaccessible);
        if (!result.experimentId().equals(backtest.experimentId())
                || !result.candidateId().equals(backtest.candidateId())) {
            throw inaccessible();
        }
        return ResultDtos.BacktestResultResponse.from(backtestId, result);
    }

    private static ResourceInaccessibleException inaccessible() {
        return new ResourceInaccessibleException("Backtest result is inaccessible");
    }
}
