package com.cryptostrategy.platform.api.leaderboard;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import java.util.Objects;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/experiments")
public final class LeaderboardController {
    private final GetExperimentUseCase experiments;
    private final GetLeaderboardUseCase leaderboards;
    private final PageRequestMapper pages;

    public LeaderboardController(
            GetExperimentUseCase experiments,
            GetLeaderboardUseCase leaderboards,
            PageRequestMapper pages) {
        this.experiments = Objects.requireNonNull(experiments, "experiments");
        this.leaderboards = Objects.requireNonNull(leaderboards, "leaderboards");
        this.pages = Objects.requireNonNull(pages, "pages");
    }

    @GetMapping("/{id}/leaderboard")
    public LeaderboardDtos.LeaderboardResponse getLeaderboard(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String id,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        ExperimentId experimentId = new ExperimentId(id);
        if (experiments.getExperiment(user.userId(), experimentId).isEmpty()) {
            throw inaccessible();
        }
        var snapshot = leaderboards.getLatest(experimentId)
                .orElseThrow(LeaderboardController::inaccessible);
        var page = pages.map(limit, cursor, Math.min(snapshot.topK(), 50), 100);
        int afterRank = page.cursor()
                .map(value -> LeaderboardCursor.decode(value, snapshot.revisionId()).rank())
                .orElse(0);
        var remaining = snapshot.entries().stream()
                .filter(entry -> entry.rank() > afterRank)
                .limit((long) page.limit() + 1)
                .toList();
        boolean hasMore = remaining.size() > page.limit();
        var selected = hasMore ? remaining.subList(0, page.limit()) : remaining;
        String nextCursor = hasMore
                ? new LeaderboardCursor(
                        snapshot.revisionId(), selected.getLast().rank()).encode()
                : null;
        return new LeaderboardDtos.LeaderboardResponse(
                snapshot.experimentId(),
                snapshot.revisionId(),
                snapshot.revisionNumber(),
                snapshot.topK(),
                snapshot.rankingVersion().value(),
                snapshot.fingerprint(),
                snapshot.createdAt(),
                selected.stream().map(LeaderboardDtos.EntryResponse::from).toList(),
                nextCursor,
                hasMore);
    }

    private static ResourceInaccessibleException inaccessible() {
        return new ResourceInaccessibleException("Leaderboard is inaccessible");
    }
}
