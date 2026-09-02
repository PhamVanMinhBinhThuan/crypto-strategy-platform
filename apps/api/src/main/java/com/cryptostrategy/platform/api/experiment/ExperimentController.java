package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.error.DependencyUnavailableException;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.api.transport.PageRequestMapper;
import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.port.in.GetExperimentUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.ListCandidatesUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.StopExperimentUseCase;
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

    public ExperimentController(
            IdempotencyCommandExecutor idempotency,
            GetExperimentUseCase experiments,
            GetJobUseCase jobs,
            ListCandidatesUseCase candidates,
            StopExperimentUseCase stopExperiment,
            PageRequestMapper pages) {
        this.idempotency = Objects.requireNonNull(idempotency, "idempotency");
        this.experiments = Objects.requireNonNull(experiments, "experiments");
        this.jobs = Objects.requireNonNull(jobs, "jobs");
        this.candidates = Objects.requireNonNull(candidates, "candidates");
        this.stopExperiment = Objects.requireNonNull(stopExperiment, "stopExperiment");
        this.pages = Objects.requireNonNull(pages, "pages");
    }

    @PostMapping
    public ResponseEntity<CommandDtos.ExperimentAcceptedResponse> startExperiment(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CommandDtos.StartExperimentRequest request) {
        throw searchCoordinatorUnavailable();
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
        throw searchCoordinatorUnavailable();
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

    private static ResourceInaccessibleException inaccessible() {
        return new ResourceInaccessibleException("Experiment resource is inaccessible");
    }
}
