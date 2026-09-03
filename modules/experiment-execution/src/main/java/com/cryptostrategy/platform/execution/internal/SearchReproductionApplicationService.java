package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.search.api.model.SearchRunId;
import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchReproductionUseCase;
import com.cryptostrategy.platform.execution.api.port.out.SearchReproductionGateway;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.IdempotencyConflictException;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.util.Objects;
import com.cryptostrategy.platform.search.api.model.ReproductionVerificationId;

/** Validate owner/terminal/evidence trước khi yêu cầu một atomic immutable-source copy. */
public final class SearchReproductionApplicationService implements StartSearchReproductionUseCase {
    private final SearchReproductionGateway gateway;

    public SearchReproductionApplicationService(SearchReproductionGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    @Override
    public Acceptance start(Command command) {
        var source = gateway.loadSource(command.ownerUserId(), command.sourceExperimentId())
                .orElseThrow(() -> new ResourceInaccessibleException("Experiment resource is inaccessible"));
        if (!"COMPLETED".equals(source.status()) && !"STOPPED".equals(source.status())) {
            throw new IllegalStateException("Source Experiment must be terminal");
        }
        if (!source.evidenceComplete()) {
            throw new IllegalStateException("Source Experiment lacks reproduction evidence");
        }
        ExperimentId target = new ExperimentId(Ulids.generate());
        JobId searchJob = new JobId(Ulids.generate());
        var copies = source.orderedCandidateIds().stream().map(sourceCandidate ->
                new SearchReproductionGateway.CandidateCopy(new CandidateId(sourceCandidate), new CandidateId(Ulids.generate()),
                        new JobId(Ulids.generate()), Ulids.generate(), Ulids.generate())).toList();
        var result = gateway.create(new SearchReproductionGateway.CreateCommand(
                command.ownerUserId(), command.sourceExperimentId(), target, searchJob,
                new SearchRunId(Ulids.generate()), new ReproductionVerificationId(Ulids.generate()), command.name(), command.idempotencyKey(),
                command.canonicalRequestHash(), command.correlationId(), command.requestedAt(),
                command.receiptExpiresAt(), copies));
        if (result.status() == SearchReproductionGateway.Result.Status.CONFLICT) {
            throw new IdempotencyConflictException("Idempotency key conflicts with an earlier reproduction");
        }
        return new Acceptance(result.experimentId(), result.searchJobId(), "QUEUED",
                result.status() == SearchReproductionGateway.Result.Status.REPLAY);
    }
}
