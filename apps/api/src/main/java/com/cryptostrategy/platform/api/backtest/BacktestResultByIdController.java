package com.cryptostrategy.platform.api.backtest;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.port.in.GetBacktestResultUseCase;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import java.util.Objects;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/backtest-results")
public final class BacktestResultByIdController {
    private final GetBacktestResultUseCase results;
    private final GetExperimentUseCase experiments;
    private final ListCandidatesUseCase candidates;

    public BacktestResultByIdController(
            GetBacktestResultUseCase results,
            GetExperimentUseCase experiments,
            ListCandidatesUseCase candidates) {
        this.results = Objects.requireNonNull(results, "results");
        this.experiments = Objects.requireNonNull(experiments, "experiments");
        this.candidates = Objects.requireNonNull(candidates, "candidates");
    }

    @GetMapping("/{resultId}")
    public ResultDtos.BacktestResultResponse getResult(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String resultId) {
        var result = results.getByResultId(new BacktestResultId(resultId))
                .orElseThrow(BacktestResultByIdController::inaccessible);
        if (experiments.getExperiment(user.userId(), result.experimentId()).isEmpty()) {
            throw inaccessible();
        }
        var manifest = experiments.getManifest(user.userId(), result.experimentId())
                .orElseThrow(BacktestResultByIdController::inaccessible);
        var candidate = candidates.getCandidate(
                        user.userId(), result.experimentId(), result.candidateId())
                .orElseThrow(BacktestResultByIdController::inaccessible);
        return ResultDtos.BacktestResultResponse.from(null, result, manifest, candidate);
    }

    private static ResourceInaccessibleException inaccessible() {
        return new ResourceInaccessibleException("Backtest result is inaccessible");
    }
}
