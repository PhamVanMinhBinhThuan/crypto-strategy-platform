package com.cryptostrategy.platform.api.backtest;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.transport.HistoryCursor;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.backtesting.api.model.BacktestResultId;
import com.cryptostrategy.platform.backtesting.api.port.in.GetBacktestResultUseCase;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import java.util.Objects;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/backtest-results")
public final class BacktestResultByIdController {
    private final GetBacktestResultUseCase results;
    private final GetExperimentUseCase experiments;
    private final ListCandidatesUseCase candidates;
    private final PageRequestMapper pages;

    public BacktestResultByIdController(
            GetBacktestResultUseCase results,
            GetExperimentUseCase experiments,
            ListCandidatesUseCase candidates) {
        this(results, experiments, candidates, new PageRequestMapper());
    }

    @Autowired
    public BacktestResultByIdController(
            GetBacktestResultUseCase results,
            GetExperimentUseCase experiments,
            ListCandidatesUseCase candidates,
            PageRequestMapper pages) {
        this.results = Objects.requireNonNull(results, "results");
        this.experiments = Objects.requireNonNull(experiments, "experiments");
        this.candidates = Objects.requireNonNull(candidates, "candidates");
        this.pages = Objects.requireNonNull(pages, "pages");
    }

    @GetMapping
    public ResultDtos.BacktestResultHistoryPage listResults(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        var page = pages.map(limit, cursor, 10, 100);
        HistoryCursor after = page.cursor()
                .map(value -> HistoryCursor.decode(value, "backtest-results"))
                .orElse(null);
        var ordered = results.listRecent(
                user.userId(),
                after == null ? null : after.timestamp(),
                after == null ? null : after.resourceKey(),
                page.limit() + 1);
        boolean hasMore = ordered.size() > page.limit();
        var selected = hasMore ? ordered.subList(0, page.limit()) : ordered;
        String nextCursor = hasMore && !selected.isEmpty()
                ? new HistoryCursor("backtest-results", selected.getLast().completedAt(),
                        selected.getLast().backtestResultId().value()).encode()
                : null;
        return new ResultDtos.BacktestResultHistoryPage(
                selected.stream().map(ResultDtos.BacktestResultHistoryItem::from).toList(),
                nextCursor, hasMore, results.count(user.userId()));
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
