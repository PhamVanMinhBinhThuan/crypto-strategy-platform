package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.api.idempotency.IdempotencyCommandExecutor;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import com.cryptostrategy.platform.experiment.api.port.in.CancelJobUseCase;
import com.cryptostrategy.platform.experiment.api.port.in.GetJobUseCase;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public final class JobController {
    static final String CANCEL_OPERATION = "CANCEL_JOB";

    private final IdempotencyCommandExecutor idempotency;
    private final GetJobUseCase jobs;
    private final CancelJobUseCase cancelJob;

    public JobController(
            IdempotencyCommandExecutor idempotency,
            GetJobUseCase jobs,
            CancelJobUseCase cancelJob) {
        this.idempotency = Objects.requireNonNull(idempotency, "idempotency");
        this.jobs = Objects.requireNonNull(jobs, "jobs");
        this.cancelJob = Objects.requireNonNull(cancelJob, "cancelJob");
    }

    @GetMapping("/{id}")
    public ReadDtos.JobResponse getJob(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @PathVariable String id) {
        return jobs.getJob(user.userId(), new JobId(id))
                .map(ReadDtos.JobResponse::from)
                .orElseThrow(JobController::inaccessible);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReadDtos.JobResponse> cancelJob(
            @AuthenticationPrincipal AuthenticatedUserContext user,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable String id) {
        JobId jobId = new JobId(id);
        var response = idempotency.execute(
                user.userId(),
                CANCEL_OPERATION,
                idempotencyKey,
                Map.of("jobId", id),
                ReadDtos.JobResponse.class,
                (key, requestHash) -> {
                    cancelJob.cancelJob(user.userId(), jobId);
                    return jobs.getJob(user.userId(), jobId)
                            .map(ReadDtos.JobResponse::from)
                            .orElseThrow(JobController::inaccessible);
                });
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/jobs/" + id))
                .body(response);
    }

    private static ResourceInaccessibleException inaccessible() {
        return new ResourceInaccessibleException("Job is inaccessible");
    }
}
