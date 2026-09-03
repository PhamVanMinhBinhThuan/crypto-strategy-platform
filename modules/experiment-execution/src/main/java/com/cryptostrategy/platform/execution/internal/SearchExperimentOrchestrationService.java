package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase;
import com.cryptostrategy.platform.execution.api.port.out.SearchExperimentTransactionGateway;
import com.cryptostrategy.platform.execution.api.port.out.StartSearchGraphCommand;
import com.cryptostrategy.platform.execution.api.port.out.StartSearchGraphResult;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import java.util.Objects;

/** Điều phối Start qua owner graph công khai và một transaction gateway duy nhất. */
public final class SearchExperimentOrchestrationService implements StartSearchExperimentUseCase {
    private static final String OPERATION = "START_SEARCH";
    private final SearchExperimentTransactionGateway transactions;

    public SearchExperimentOrchestrationService(SearchExperimentTransactionGateway transactions) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
    }

    @Override
    public Acceptance start(StartCommand command) {
        Objects.requireNonNull(command, "command");
        validateOwnerGraph(command);
        StartSearchGraphResult result = transactions.start(new StartSearchGraphCommand(
                command.ownerUserId(),
                OPERATION,
                command.idempotencyKey(),
                command.canonicalRequestHash(),
                command.receiptExpiresAt(),
                command.experiment(),
                command.manifest(),
                command.searchJob(),
                command.searchRun(),
                command.searchRequest()));
        if (result.status() == StartSearchGraphResult.Status.CONFLICT) {
            throw new IdempotencyConflictException(
                    "Idempotency key was already used with a different request payload");
        }
        return new Acceptance(
                result.experimentId(),
                result.searchJobId(),
                "QUEUED",
                result.status() == StartSearchGraphResult.Status.REPLAY);
    }

    private static void validateOwnerGraph(StartCommand command) {
        var experimentId = command.experiment().experimentId();
        if (!command.ownerUserId().equals(command.experiment().ownerUserId())
                || !experimentId.equals(command.manifest().experimentId())
                || !experimentId.value().equals(command.searchRun().experimentRef())
                || !experimentId.equals(command.searchJob().experimentId())
                || !command.searchJob().jobId().value().equals(command.searchRun().searchJobRef())
                || !command.searchJob().jobId().value().equals(command.searchRequest().aggregateId())) {
            throw new IllegalArgumentException("Start Search owner graph is inconsistent");
        }
    }
}
