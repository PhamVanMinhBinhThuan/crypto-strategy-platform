package com.cryptostrategy.platform.api.strategy;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.strategy.api.model.user.query.UsableStrategyPageRequest;
import com.cryptostrategy.platform.strategy.api.port.in.UserStrategyApplication;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/strategies")
public final class StrategyController {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final UserStrategyApplication strategies;
    private final PageRequestMapper pageRequests;

    public StrategyController(
            UserStrategyApplication strategies, PageRequestMapper pageRequests) {
        this.strategies = strategies;
        this.pageRequests = pageRequests;
    }

    @GetMapping
    public StrategyDtos.StrategyPage list(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        var page = pageRequests.map(limit, cursor, DEFAULT_LIMIT, MAX_LIMIT);
        Optional<String> internalCursor = page.cursor()
                .map(value -> StrategyCursor.decode(StrategyCursor.Stream.SYSTEM, value));
        var catalog = strategies.listUsableStrategies(
                user.userId(),
                new UsableStrategyPageRequest(
                        page.limit(), internalCursor, 1, Optional.empty()));
        String nextCursor = catalog.systemStrategies()
                .nextCursor()
                .map(value -> StrategyCursor.encode(
                        StrategyCursor.Stream.SYSTEM, value))
                .orElse(null);
        return new StrategyDtos.StrategyPage(
                catalog.systemStrategies().items().stream()
                        .map(StrategyDtos.StrategyDescriptorResponse::from)
                        .toList(),
                nextCursor,
                nextCursor != null);
    }
}
