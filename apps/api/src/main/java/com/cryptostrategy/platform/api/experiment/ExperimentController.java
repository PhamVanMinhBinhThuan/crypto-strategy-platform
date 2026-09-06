package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.error.DependencyUnavailableException;
import com.cryptostrategy.platform.api.error.RequestFieldValidationException;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.observability.CorrelationContext;
import com.cryptostrategy.platform.api.observability.CorrelationId;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.api.transport.HistoryCursor;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchExperimentUseCase;
import com.cryptostrategy.platform.execution.api.port.in.StartSearchReproductionUseCase;
import com.cryptostrategy.platform.execution.api.port.in.GetSearchProgressUseCase;
import com.cryptostrategy.platform.leaderboard.api.port.in.GetLeaderboardUseCase;
import com.cryptostrategy.platform.api.leaderboard.LeaderboardDtos;
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
    private final GetSearchProgressUseCase searchProgress;
    private final GetLeaderboardUseCase leaderboards;

    public ExperimentController(
            IdempotencyCommandExecutor idempotency,
            GetExperimentUseCase experiments,
            GetJobUseCase jobs,
            ListCandidatesUseCase candidates,
            StopExperimentUseCase stopExperiment,
            PageRequestMapper pages) {
        this(idempotency, experiments, jobs, candidates, stopExperiment, pages, null, null, false, null, false,
                Clock.systemUTC(), ignored -> java.util.Optional.empty(), emptyLeaderboards());
    }

    ExperimentController(
            IdempotencyCommandExecutor idempotency, GetExperimentUseCase experiments,
            GetJobUseCase jobs, ListCandidatesUseCase candidates, StopExperimentUseCase stopExperiment,
            PageRequestMapper pages, StartSearchExperimentUseCase startSearch,
            ExperimentRequestMapper startRequests, boolean searchStartEnabled) {
        this(idempotency, experiments, jobs, candidates, stopExperiment, pages, startSearch, startRequests,
                searchStartEnabled, null, false, Clock.systemUTC(), ignored -> java.util.Optional.empty(),
                emptyLeaderboards());
    }

    ExperimentController(
            IdempotencyCommandExecutor idempotency, GetExperimentUseCase experiments,
            GetJobUseCase jobs, ListCandidatesUseCase candidates, StopExperimentUseCase stopExperiment,
            PageRequestMapper pages, StartSearchExperimentUseCase startSearch,
            ExperimentRequestMapper startRequests, boolean searchStartEnabled,
            StartSearchReproductionUseCase reproduceSearch, boolean searchReproduceEnabled) {
        this(idempotency, experiments, jobs, candidates, stopExperiment, pages, startSearch, startRequests,
                searchStartEnabled, reproduceSearch, searchReproduceEnabled, Clock.systemUTC(),
                ignored -> java.util.Optional.empty(), emptyLeaderboards());
    }

    ExperimentController(
            IdempotencyCommandExecutor idempotency, GetExperimentUseCase experiments,
            GetJobUseCase jobs, ListCandidatesUseCase candidates, StopExperimentUseCase stopExperiment,
            PageRequestMapper pages, StartSearchExperimentUseCase startSearch,
            ExperimentRequestMapper startRequests, boolean searchStartEnabled,
            StartSearchReproductionUseCase reproduceSearch, boolean searchReproduceEnabled,
            Clock clock) {
        this(idempotency, experiments, jobs, candidates, stopExperiment, pages, startSearch,
                startRequests, searchStartEnabled, reproduceSearch, searchReproduceEnabled, clock,
                ignored -> java.util.Optional.empty(), emptyLeaderboards());
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
            @Qualifier("searchApiClock") Clock clock,
            GetSearchProgressUseCase searchProgress,
            GetLeaderboardUseCase leaderboards) {
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
        this.searchProgress = Objects.requireNonNull(searchProgress, "searchProgress");
        this.leaderboards = Objects.requireNonNull(leaderboards, "leaderboards");
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
                (key, requestHash) -> {
                    try {
                        return startSearch.start(startRequests.map(
                                user.userId(), key, requestHash, correlationId(), request));
                    } catch (RequestFieldValidationException failure) {
                        throw failure;
                    } catch (IllegalArgumentException failure) {
                        String message = failure.getMessage();
                        String prefix = "searchSpace.strategyPool: ";
                        if (message != null && message.startsWith(prefix)) {
                            throw new RequestFieldValidationException(
                                    "searchSpace.strategyPool", message.substring(prefix.length()));
                        }
                        throw failure;
                    }
                });
        var response = new CommandDtos.ExperimentAcceptedResponse(
                accepted.experimentId(), accepted.searchJobId(), accepted.searchRunId(),
                accepted.status(), accepted.configurationVersion(),
                accepted.configurationFingerprint(),
                "/search/" + accepted.experimentId().value());
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
                experiment, manifest, jobs.listJobs(user.userId(), experimentId),
                searchProgress.findByExperimentId(experimentId).orElse(null));
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
                            stopped, manifest, jobs.listJobs(user.userId(), experimentId),
                            searchProgress.findByExperimentId(experimentId).orElse(null));
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
    public LeaderboardDtos.CandidateDetailResponse getCandidate(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String id,
            @PathVariable String candidateId) {
        ExperimentId experimentId = new ExperimentId(id);
        var candidate = candidates.getCandidate(user.userId(), experimentId,
                        new CandidateId(candidateId))
                .orElseThrow(ExperimentController::inaccessible);
        var manifest = experiments.getManifest(user.userId(), experimentId)
                .orElseThrow(ExperimentController::inaccessible);
        var pipeline = leaderboards.getCandidatePipelineEntry(
                experimentId, new CandidateId(candidateId)).orElse(null);
        if (pipeline == null) {
            var legacyEvidence = leaderboards.getCandidate(
                    experimentId, new CandidateId(candidateId));
            if (legacyEvidence.isPresent()) {
                return LeaderboardDtos.CandidateDetailResponse.from(
                        legacyEvidence.get(), manifest.datasetProvenance());
            }
        }
        return LeaderboardDtos.CandidateDetailResponse.from(
                candidate, manifest.datasetProvenance(), pipeline);
    }

    @GetMapping
    public ReadDtos.ExperimentHistoryPage listExperiments(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        var page = pages.map(limit, cursor, 10, 100);
        HistoryCursor after = page.cursor()
                .map(value -> HistoryCursor.decode(value, "experiments"))
                .orElse(null);
        var ordered = experiments.listRecent(
                user.userId(),
                after == null ? null : after.timestamp(),
                after == null ? null : after.id(),
                page.limit() + 1);
        boolean hasMore = ordered.size() > page.limit();
        var selected = hasMore ? ordered.subList(0, page.limit()) : ordered;
        String nextCursor = hasMore && !selected.isEmpty()
                ? new HistoryCursor("experiments", selected.getLast().createdAt(),
                        selected.getLast().experimentId().value()).encode()
                : null;
        return new ReadDtos.ExperimentHistoryPage(
                selected.stream().map(ReadDtos.ExperimentHistoryItem::from).toList(),
                nextCursor, hasMore, experiments.count(user.userId()));
    }

    @GetMapping("/{id}/candidate-pipeline")
    public ReadDtos.CandidatePipelinePage candidatePipeline(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String id,
            @RequestParam(defaultValue = "ALL") String view,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String cursor) {
        ExperimentId experimentId = new ExperimentId(id);
        if (experiments.getExperiment(user.userId(), experimentId).isEmpty()) throw inaccessible();
        String normalizedView = view.toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("RESULTS", "FAILED", "ALL").contains(normalizedView)) {
            throw new IllegalArgumentException("view must be RESULTS, FAILED or ALL");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        var all = leaderboards.getCandidatePipeline(experimentId);
        int resultCount = (int) all.stream()
                .filter(item -> "SUCCEEDED".equals(item.evaluation().status())).count();
        int failedCount = (int) all.stream().filter(item -> item.failureStage() != null).count();
        java.util.Comparator<com.cryptostrategy.platform.leaderboard.api.model.CandidatePipelineEntry>
                generationOrder = java.util.Comparator
                        .comparingInt(com.cryptostrategy.platform.leaderboard.api.model.CandidatePipelineEntry::generationIndex)
                        .thenComparing(item -> item.candidateId().value());
        java.util.Comparator<com.cryptostrategy.platform.leaderboard.api.model.CandidatePipelineEntry>
                resultOrder = java.util.Comparator
                        .comparing((com.cryptostrategy.platform.leaderboard.api.model.CandidatePipelineEntry item) ->
                                item.evaluation().score(), java.util.Comparator.reverseOrder())
                        .thenComparing(item -> item.evaluation().evaluatedAt())
                        .thenComparing(item -> item.candidateId().value());
        var filtered = all.stream().filter(item -> switch (normalizedView) {
                    case "RESULTS" -> "SUCCEEDED".equals(item.evaluation().status());
                    case "FAILED" -> item.failureStage() != null;
                    default -> true;
                })
                .sorted("RESULTS".equals(normalizedView) ? resultOrder : generationOrder)
                .toList();
        int start = 0;
        if (cursor != null && !cursor.isBlank()) {
            CandidatePipelineCursor decoded = CandidatePipelineCursor.decode(cursor);
            if (!normalizedView.equals(decoded.view())) {
                throw new IllegalArgumentException("Cursor does not belong to this pipeline view");
            }
            start = java.util.stream.IntStream.range(0, filtered.size())
                    .filter(index -> filtered.get(index).candidateId().value().equals(decoded.candidateId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Cursor is stale")) + 1;
        }
        int end = Math.min(filtered.size(), start + limit);
        var selected = filtered.subList(start, end);
        boolean hasMore = end < filtered.size();
        String nextCursor = hasMore && !selected.isEmpty()
                ? new CandidatePipelineCursor(normalizedView,
                        selected.getLast().candidateId().value()).encode()
                : null;
        return new ReadDtos.CandidatePipelinePage(
                selected.stream().map(ReadDtos.CandidatePipelineResponse::from).toList(),
                nextCursor, hasMore, resultCount, failedCount, all.size());
    }

    private static DependencyUnavailableException searchCoordinatorUnavailable() {
        return new DependencyUnavailableException("Search Coordinator");
    }

    private static String correlationId() {
        String current = CorrelationContext.current();
        return current == null ? CorrelationId.resolve(null) : current;
    }

    private static GetLeaderboardUseCase emptyLeaderboards() {
        return experimentId -> java.util.Optional.empty();
    }

    private static ResourceInaccessibleException inaccessible() {
        return new ResourceInaccessibleException("Experiment resource is inaccessible");
    }
}
