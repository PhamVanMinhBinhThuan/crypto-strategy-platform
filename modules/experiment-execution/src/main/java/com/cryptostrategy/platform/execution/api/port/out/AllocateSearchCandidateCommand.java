package com.cryptostrategy.platform.execution.api.port.out;

import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.search.api.model.CoordinationDecision;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import com.cryptostrategy.platform.search.api.port.out.SearchRunClaim;
import java.util.Objects;
import java.util.UUID;

public record AllocateSearchCandidateCommand(
        UUID ownerUserId,
        SearchRunClaim claim,
        SearchRun replacementRun,
        CandidateDefinition candidate,
        Job backtestJob,
        CoordinationDecision decision,
        OutboxEvent outboxEvent) {
    public AllocateSearchCandidateCommand {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        Objects.requireNonNull(claim, "claim");
        Objects.requireNonNull(replacementRun, "replacementRun");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(backtestJob, "backtestJob");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(outboxEvent, "outboxEvent");
    }
}
