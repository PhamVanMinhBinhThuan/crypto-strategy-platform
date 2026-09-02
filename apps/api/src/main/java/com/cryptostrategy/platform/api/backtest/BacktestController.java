package com.cryptostrategy.platform.api.backtest;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.experiment.CommandDtos;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.observability.CorrelationContext;
import com.cryptostrategy.platform.api.observability.CorrelationId;
import com.cryptostrategy.platform.experiment.api.port.in.StartStandaloneBacktestUseCase;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/backtests")
public final class BacktestController {
    private final StartStandaloneBacktestUseCase backtests;
    private final BacktestRequestMapper requests;
    private final IdempotencyCommandExecutor idempotency;

    public BacktestController(
            StartStandaloneBacktestUseCase backtests,
            BacktestRequestMapper requests,
            IdempotencyCommandExecutor idempotency) {
        this.backtests = backtests;
        this.requests = requests;
        this.idempotency = idempotency;
    }

    @PostMapping
    public ResponseEntity<CommandDtos.BacktestAcceptedResponse> start(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CommandDtos.StartBacktestRequest request) {
        String correlationId = CorrelationContext.current();
        if (correlationId == null) {
            correlationId = CorrelationId.resolve(null);
        }
        String resolvedCorrelationId = correlationId;
        var acceptance = idempotency.execute(
                user.userId(),
                StartStandaloneBacktestUseCase.OPERATION,
                idempotencyKey,
                request,
                (key, hash) -> backtests.startStandaloneBacktest(
                        user.userId(),
                        requests.map(request, key, hash, resolvedCorrelationId)));
        var response = CommandDtos.BacktestAcceptedResponse.from(acceptance);
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/jobs/" + response.jobId().value()))
                .body(response);
    }
}
