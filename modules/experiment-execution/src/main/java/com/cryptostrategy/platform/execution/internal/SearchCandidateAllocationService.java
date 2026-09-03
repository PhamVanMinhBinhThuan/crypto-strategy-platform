package com.cryptostrategy.platform.execution.internal;

import com.cryptostrategy.platform.domain.api.identity.Ulids;
import com.cryptostrategy.platform.execution.api.port.in.SearchCandidateAllocationUseCase;
import com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationCommand;
import com.cryptostrategy.platform.execution.api.port.in.SearchCoordinationResult;
import com.cryptostrategy.platform.execution.api.port.out.AllocateSearchCandidateCommand;
import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationContextGateway;
import com.cryptostrategy.platform.execution.api.port.out.SearchAllocationResult;
import com.cryptostrategy.platform.execution.api.port.out.SearchExperimentTransactionGateway;
import com.cryptostrategy.platform.experiment.api.CandidateDefinition;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.search.api.model.CoordinationDecision;
import com.cryptostrategy.platform.search.api.model.CoordinationDecisionId;
import com.cryptostrategy.platform.search.api.model.CoordinationDecisionType;
import com.cryptostrategy.platform.search.api.model.GenerationOutcome;
import com.cryptostrategy.platform.search.api.model.GenerationRequest;
import com.cryptostrategy.platform.search.api.port.in.SearchGenerationUseCase;
import com.cryptostrategy.platform.search.api.port.out.SearchRunClaim;
import com.cryptostrategy.platform.search.api.port.out.SearchRunStore;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Fill bounded window bằng proposal thuần, sau đó revalidate qua composite transaction fence. */
public final class SearchCandidateAllocationService implements SearchCandidateAllocationUseCase {
    private static final int DRAW_BUDGET = 256;
    private final SearchRunStore runs;
    private final SearchGenerationUseCase generation;
    private final SearchAllocationContextGateway contexts;
    private final SearchExperimentTransactionGateway transactions;
    private final Clock clock;
    private final ObjectMapper json;

    public SearchCandidateAllocationService(
            SearchRunStore runs,
            SearchGenerationUseCase generation,
            SearchAllocationContextGateway contexts,
            SearchExperimentTransactionGateway transactions,
            Clock clock,
            ObjectMapper json) {
        this.runs = Objects.requireNonNull(runs, "runs");
        this.generation = Objects.requireNonNull(generation, "generation");
        this.contexts = Objects.requireNonNull(contexts, "contexts");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.json = Objects.requireNonNull(json, "json");
    }

    @Override
    public SearchCoordinationResult fillAvailableSlots(SearchCoordinationCommand command) {
        Objects.requireNonNull(command, "command");
        var context = contexts.load(command.experimentId().value(), command.searchJobId().value())
                .orElseThrow(() -> new IllegalArgumentException("Search allocation context is inaccessible"));
        int allocated = context.acceptedCandidateFingerprints().size();
        int active = allocated - context.completedWork() - context.failedWork();
        int targetWindow = Math.min(command.concurrencyHint(), command.topKTarget());

        while (active < targetWindow) {
            SearchRunClaim claim = runs.findBySearchJobId(new com.cryptostrategy.platform.search.api.model.SearchJobId(command.searchJobId().value()))
                    .flatMap(run -> runs.claim(run.searchRunId()))
                    .orElseThrow(() -> new IllegalArgumentException("Search Run is inaccessible"));
            var run = claim.snapshot();
            if (run.status().isTerminal() || run.status().name().equals("STOPPING")
                    || run.nextGenerationIndex() >= run.stopConditions().maximumCandidates()) {
                return result(run, allocated, active, context);
            }
            GenerationOutcome outcome = generation.generate(run.generatorId(), run.generatorVersion(),
                    new GenerationRequest(context.searchSpace(), run.seed(), Optional.of(run.generatorState()),
                            Math.toIntExact(run.nextGenerationIndex()),
                            context.acceptedCandidateFingerprints(), DRAW_BUDGET));
            if (!(outcome instanceof GenerationOutcome.Generated generated)) {
                return result(run, allocated, active, context);
            }

            Instant now = clock.instant();
            CandidateId candidateId = new CandidateId(Ulids.generate());
            JobId backtestJobId = new JobId(Ulids.generate());
            var candidate = new CandidateDefinition(candidateId, new ExperimentId(run.experimentId().value()),
                    generated.candidate().generationIndex(), parameterMap(generated),
                    Map.of("contractVersion", generated.nextState().contractVersion(),
                            "canonicalState", generated.nextState().canonicalState(),
                            "fingerprint", generated.nextState().fingerprint()),
                    generated.candidate().fingerprint(), now);
            Job job = Job.createBacktestJob(backtestJobId, new ExperimentId(run.experimentId().value()),
                    candidateId, command.correlationId(), now);
            var replacement = run.advance(generated.nextState(), run.nextGenerationIndex() + 1, now);
            var decision = new CoordinationDecision(new CoordinationDecisionId(Ulids.generate()),
                    run.searchRunId(), run.nextGenerationIndex(), CoordinationDecisionType.ALLOCATED,
                    new com.cryptostrategy.platform.search.api.model.SearchCandidateId(candidateId.value()),
                    new com.cryptostrategy.platform.search.api.model.SearchJobId(backtestJobId.value()), generated.candidate().fingerprint(),
                    run.generatorState().fingerprint(), generated.nextState().fingerprint(),
                    "CANDIDATE_ALLOCATED", now);
            String messageId = Ulids.generate();
            var outbox = new OutboxEvent(Ulids.generate(), messageId, "JOB", backtestJobId.value(),
                    "BACKTEST_JOB", "1", backtestPayload(messageId, command, candidateId, backtestJobId, now),
                    Map.of("correlationId", command.correlationId()), now);
            SearchAllocationResult committed = transactions.allocate(new AllocateSearchCandidateCommand(
                    context.ownerUserId(), claim, replacement, candidate, job, decision, outbox));
            if (committed.status() == SearchAllocationResult.Status.STALE_FENCE) {
                context = contexts.load(command.experimentId().value(), command.searchJobId().value()).orElseThrow();
                allocated = context.acceptedCandidateFingerprints().size();
                active = allocated - context.completedWork() - context.failedWork();
                continue;
            }
            allocated++;
            active++;
            var fingerprints = new java.util.HashSet<>(context.acceptedCandidateFingerprints());
            fingerprints.add(generated.candidate().fingerprint());
            context = new SearchAllocationContextGateway.Context(context.ownerUserId(), context.searchSpace(),
                    fingerprints, context.completedWork(), context.failedWork());
        }
        var run = runs.findBySearchJobId(new com.cryptostrategy.platform.search.api.model.SearchJobId(command.searchJobId().value())).orElseThrow();
        return result(run, allocated, active, context);
    }

    private static Map<String, Object> parameterMap(GenerationOutcome.Generated generated) {
        Map<String, Object> values = new LinkedHashMap<>();
        generated.candidate().parameters().values().forEach((name, value) -> values.put(name, raw(value)));
        return Map.copyOf(values);
    }

    private static Object raw(StrategyParameterValue value) {
        return switch (value) {
            case StrategyParameterValue.IntegerValue item -> item.value();
            case StrategyParameterValue.DecimalValue item -> item.value();
            case StrategyParameterValue.BooleanValue item -> item.value();
            case StrategyParameterValue.TextValue item -> item.value();
            case StrategyParameterValue.EnumValue item -> item.value();
        };
    }

    private String backtestPayload(String messageId, SearchCoordinationCommand command,
            CandidateId candidateId, JobId jobId, Instant now) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("messageId", messageId);
        envelope.put("messageVersion", 1);
        envelope.put("messageType", "BACKTEST_JOB");
        envelope.put("occurredAt", now.toString());
        envelope.put("correlationId", command.correlationId());
        envelope.put("payload", Map.of("experimentId", command.experimentId().value(),
                "jobId", jobId.value(), "candidateId", candidateId.value()));
        try {
            return json.writeValueAsString(envelope);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Failed to serialize Backtest Job event", failure);
        }
    }

    private static SearchCoordinationResult result(com.cryptostrategy.platform.search.api.model.SearchRun run,
            int allocated, int active, SearchAllocationContextGateway.Context context) {
        return new SearchCoordinationResult(run.searchRunId(), allocated, Math.max(0, active),
                context.completedWork(), context.failedWork(), run.status());
    }
}
