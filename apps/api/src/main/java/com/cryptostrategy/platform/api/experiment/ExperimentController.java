package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.error.DependencyUnavailableException;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.observability.CorrelationContext;
import com.cryptostrategy.platform.api.observability.CorrelationId;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchReproductionUseCase;
import java.time.Instant;
import java.time.Duration;
import java.time.Clock;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController
@RequestMapping("/api/v1/experiments")
public final class ExperimentController {
    static final String STOP_OPERATION = "STOP_EXPERIMENT";

    private final IdempotencyCommandExecutor idempotency;
    private final GetExperimentUseCase experiments;
    private final GetJobUseCase jobs;
    private final ListCandidatesUseCase candidates;
    private final StopExperimentUseCase stopExperiment;
    private final PageRequestMapper pages;
    private final StartSearchExperimentUseCase startSearch;
    private final ExperimentRequestMapper startRequests;
    private final boolean searchStartEnabled;
    private final StartSearchReproductionUseCase reproduceSearch;
    private final boolean searchReproduceEnabled;
    private final Clock clock;

    public ExperimentController(
            IdempotencyCommandExecutor idempotency,
            GetExperimentUseCase experiments,
            GetJobUseCase jobs,
            ListCandidatesUseCase candidates,
            StopExperimentUseCase stopExperiment,
            PageRequestMapper pages) {
        this(idempotency, experiments, jobs, candidates, stopExperiment, pages, null, null, false, null, false,
                Clock.systemUTC());
    }

    ExperimentController(
            IdempotencyCommandExecutor idempotency, GetExperimentUseCase experiments,
            GetJobUseCase jobs, ListCandidatesUseCase candidates, StopExperimentUseCase stopExperiment,
            PageRequestMapper pages, StartSearchExperimentUseCase startSearch,
            ExperimentRequestMapper startRequests, boolean searchStartEnabled) {
        this(idempotency, experiments, jobs, candidates, stopExperiment, pages, startSearch, startRequests,
                searchStartEnabled, null, false, Clock.systemUTC());
    }

    ExperimentController(
            IdempotencyCommandExecutor idempotency, GetExperimentUseCase experiments,
            GetJobUseCase jobs, ListCandidatesUseCase candidates, StopExperimentUseCase stopExperiment,
            PageRequestMapper pages, StartSearchExperimentUseCase startSearch,
            ExperimentRequestMapper startRequests, boolean searchStartEnabled,
            StartSearchReproductionUseCase reproduceSearch, boolean searchReproduceEnabled) {
        this(idempotency, experiments, jobs, candidates, stopExperiment, pages, startSearch, startRequests,
                searchStartEnabled, reproduceSearch, searchReproduceEnabled, Clock.systemUTC());
    }

    @Autowired
    public ExperimentController(
            IdempotencyCommandExecutor idempotency,
            GetExperimentUseCase experiments,
            GetJobUseCase jobs,
            ListCandidatesUseCase candidates,
            StopExperimentUseCase stopExperiment,
            PageRequestMapper pages,
            StartSearchExperimentUseCase startSearch,
            ExperimentRequestMapper startRequests,
            @Value("${platform.features.search-start-enabled:true}") boolean searchStartEnabled,
            StartSearchReproductionUseCase reproduceSearch,
            @Value("${platform.features.search-reproduce-enabled:true}") boolean searchReproduceEnabled,
            @Qualifier("searchApiClock") Clock clock) {
        this.idempotency = Objects.requireNonNull(idempotency, "idempotency");
        this.experiments = Objects.requireNonNull(experiments, "experiments");
        this.jobs = Objects.requireNonNull(jobs, "jobs");
        this.candidates = Objects.requireNonNull(candidates, "candidates");
        this.stopExperiment = Objects.requireNonNull(stopExperiment, "stopExperiment");
        this.pages = Objects.requireNonNull(pages, "pages");
        this.startSearch = startSearch;
        this.startRequests = startRequests;
        this.searchStartEnabled = searchStartEnabled;
        this.reproduceSearch = reproduceSearch;
        this.searchReproduceEnabled = searchReproduceEnabled;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @PostMapping
    public ResponseEntity<CommandDtos.ExperimentAcceptedResponse> startExperiment(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CommandDtos.StartExperimentRequest request) {
        if (!searchStartEnabled) {
            throw searchCoordinatorUnavailable();
        }
        var accepted = idempotency.execute(
                user.userId(),
                "START_SEARCH",
                idempotencyKey,
                request,
                (key, requestHash) -> startSearch.start(startRequests.map(
                        user.userId(), key, requestHash, correlationId(), request)));
        var response = new CommandDtos.ExperimentAcceptedResponse(
                accepted.experimentId(), accepted.searchJobId(), accepted.status());
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/experiments/" + accepted.experimentId().value()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ReadDtos.ExperimentResponse getExperiment(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String id) {
        ExperimentId experimentId = new ExperimentId(id);
        var experiment = experiments.getExperiment(user.userId(), experimentId)
                .orElseThrow(ExperimentController::inaccessible);
        var manifest = experiments.getManifest(user.userId(), experimentId)
                .orElseThrow(ExperimentController::inaccessible);
        return ReadDtos.ExperimentResponse.from(
                experiment, manifest, jobs.listJobs(user.userId(), experimentId));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<ReadDtos.ExperimentResponse> stopExperiment(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable String id) {
        ExperimentId experimentId = new ExperimentId(id);
        var response = idempotency.execute(
                user.userId(),
                STOP_OPERATION,
                idempotencyKey,
                Map.of("experimentId", id),
                ReadDtos.ExperimentResponse.class,
                (key, requestHash) -> {
                    var stopped = stopExperiment.stopExperiment(user.userId(), experimentId);
                    var manifest = experiments.getManifest(user.userId(), experimentId)
                            .orElseThrow(ExperimentController::inaccessible);
                    return ReadDtos.ExperimentResponse.from(
                            stopped, manifest, jobs.listJobs(user.userId(), experimentId));
                });
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/experiments/" + id))
                .body(response);
    }

    @PostMapping("/{id}/reproductions")
    public ResponseEntity<CommandDtos.ExperimentAcceptedResponse> reproduceExperiment(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable String id,
            @RequestBody(required = false) CommandDtos.ReproduceExperimentRequest request) {
        if (!searchReproduceEnabled) throw searchCoordinatorUnavailable();
        CommandDtos.ReproduceExperimentRequest body = request == null
                ? new CommandDtos.ReproduceExperimentRequest("Reproduction of " + id) : request;
        String reproductionName = body.name() == null || body.name().isBlank()
                ? "Reproduction of " + id
                : body.name();
        Instant now = clock.instant();
        String correlationId = correlationId();
        var accepted = idempotency.execute(user.userId(), "REPRODUCE_SEARCH", idempotencyKey,
                Map.of("sourceExperimentId", id, "name", reproductionName),
                (key, hash) -> reproduceSearch.start(new StartSearchReproductionUseCase.Command(
                        user.userId(), new ExperimentId(id), reproductionName, key, hash,
                        correlationId, now, now.plus(Duration.ofHours(24)))));
        var response = new CommandDtos.ExperimentAcceptedResponse(
                accepted.experimentId(), accepted.searchJobId(), accepted.status());
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/experiments/" + accepted.experimentId().value()))
                .body(response);
    }

    @GetMapping("/{id}/candidates")
    public ReadDtos.CandidatePage listCandidates(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String id,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        ExperimentId experimentId = new ExperimentId(id);
        if (experiments.getExperiment(user.userId(), experimentId).isEmpty()) {
            throw inaccessible();
        }
        var page = pages.map(limit, cursor, 50, 100);
        CandidateCursor after = page.cursor().map(CandidateCursor::decode).orElse(null);
        var ordered = candidates.listCandidates(
                user.userId(),
                experimentId,
                after == null ? -1 : after.generationIndex(),
                after == null ? "" : after.candidateId().value(),
                page.limit() + 1);
        boolean hasMore = ordered.size() > page.limit();
        var selected = hasMore ? ordered.subList(0, page.limit()) : ordered;
        String nextCursor = hasMore
                ? CandidateCursor.from(selected.getLast()).encode()
                : null;
        return new ReadDtos.CandidatePage(
                selected.stream().map(ReadDtos.CandidateResponse::from).toList(),
                nextCursor,
                hasMore);
    }

    @GetMapping("/{id}/candidates/{candidateId}")
    public ReadDtos.CandidateResponse getCandidate(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String id,
            @PathVariable String candidateId) {
        return candidates.getCandidate(
                        user.userId(), new ExperimentId(id), new CandidateId(candidateId))
                .map(ReadDtos.CandidateResponse::from)
                .orElseThrow(ExperimentController::inaccessible);
    }

    private static DependencyUnavailableException searchCoordinatorUnavailable() {
        return new DependencyUnavailableException("Search Coordinator");
    }

    private static String correlationId() {
        String current = CorrelationContext.current();
        return current == null ? CorrelationId.resolve(null) : current;
    }

    private static ResourceInaccessibleException inaccessible() {
        return new ResourceInaccessibleException("Experiment resource is inaccessible");
    }
}
