package com.cryptostrategy.platform.api.strategy;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyId;
import com.cryptostrategy.platform.strategy.api.model.UserStrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.user.command.ArchiveUserStrategyCommand;
import com.cryptostrategy.platform.strategy.api.model.user.command.CreateNextStrategyVersionCommand;
import com.cryptostrategy.platform.strategy.api.model.user.command.CreateUserStrategyCommand;
import com.cryptostrategy.platform.strategy.api.model.user.command.PublishStrategyVersionCommand;
import com.cryptostrategy.platform.strategy.api.model.user.query.GetUserStrategyQuery;
import com.cryptostrategy.platform.strategy.api.model.user.query.UsableStrategyPageRequest;
import com.cryptostrategy.platform.strategy.api.port.in.UserStrategyApplication;
import java.net.URI;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user-strategies")
public final class UserStrategyController {
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final UserStrategyApplication strategies;
    private final StrategyRequestMapper requests;
    private final PageRequestMapper pageRequests;

    public UserStrategyController(
            UserStrategyApplication strategies,
            StrategyRequestMapper requests,
            PageRequestMapper pageRequests) {
        this.strategies = strategies;
        this.requests = requests;
        this.pageRequests = pageRequests;
    }

    @GetMapping
    public StrategyDtos.UserStrategyPage list(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        var page = pageRequests.map(limit, cursor, DEFAULT_LIMIT, MAX_LIMIT);
        Optional<String> internalCursor = page.cursor()
                .map(value -> StrategyCursor.decode(StrategyCursor.Stream.PRIVATE, value));
        var catalog = strategies.listUsableStrategies(
                user.userId(),
                new UsableStrategyPageRequest(
                        1, Optional.empty(), page.limit(), internalCursor));
        String nextCursor = catalog.privateStrategies()
                .nextCursor()
                .map(value -> StrategyCursor.encode(
                        StrategyCursor.Stream.PRIVATE, value))
                .orElse(null);
        return new StrategyDtos.UserStrategyPage(
                catalog.privateStrategies().items().stream()
                        .map(StrategyDtos.UserStrategySummaryResponse::from)
                        .toList(),
                nextCursor,
                nextCursor != null);
    }

    @PostMapping
    public ResponseEntity<StrategyDtos.UserStrategyResponse> create(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestBody StrategyDtos.CreateUserStrategyRequest request) {
        var kind = requests.kind(request.kind());
        var details = strategies.createUserStrategy(
                user.userId(),
                new CreateUserStrategyCommand(
                        request.name(),
                        request.description(),
                        kind,
                        requests.source(kind, request.source())));
        var response = StrategyDtos.UserStrategyResponse.from(details);
        return ResponseEntity.created(URI.create(
                        "/api/v1/user-strategies/" + response.userStrategyId().value()))
                .body(response);
    }

    @GetMapping("/{userStrategyId}")
    public StrategyDtos.UserStrategyResponse get(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String userStrategyId) {
        return StrategyDtos.UserStrategyResponse.from(strategies.getUserStrategy(
                user.userId(),
                new GetUserStrategyQuery(new UserStrategyId(userStrategyId))));
    }

    @PostMapping("/{userStrategyId}/versions")
    public ResponseEntity<StrategyDtos.UserStrategyVersionResponse> createVersion(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String userStrategyId,
            @RequestBody StrategyDtos.CreateUserStrategyVersionRequest request) {
        UserStrategyId strategyId = new UserStrategyId(userStrategyId);
        var version = strategies.createNextVersion(
                user.userId(),
                new CreateNextStrategyVersionCommand(
                        strategyId,
                        request.expectedLatestVersionNo(),
                        requests.source(
                                strategies.getUserStrategy(
                                                user.userId(),
                                                new GetUserStrategyQuery(strategyId))
                                        .strategy()
                                        .kind(),
                                request.source())));
        return ResponseEntity.created(URI.create(
                        "/api/v1/user-strategies/"
                                + userStrategyId
                                + "/versions/"
                                + version.id().value()))
                .body(StrategyDtos.UserStrategyVersionResponse.from(version));
    }

    @PostMapping("/{userStrategyId}/versions/{versionId}/publish")
    public StrategyDtos.UserStrategyVersionResponse publish(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String userStrategyId,
            @PathVariable String versionId,
            @RequestBody StrategyDtos.PublishUserStrategyVersionRequest request) {
        return StrategyDtos.UserStrategyVersionResponse.from(strategies.publish(
                user.userId(),
                new PublishStrategyVersionCommand(
                        new UserStrategyId(userStrategyId),
                        new UserStrategyVersionId(versionId),
                        request.expectedVersionNo())));
    }

    @PostMapping("/{userStrategyId}/archive")
    public StrategyDtos.UserStrategyResponse archive(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String userStrategyId) {
        return StrategyDtos.UserStrategyResponse.from(strategies.archive(
                user.userId(),
                new ArchiveUserStrategyCommand(new UserStrategyId(userStrategyId))));
    }
}
