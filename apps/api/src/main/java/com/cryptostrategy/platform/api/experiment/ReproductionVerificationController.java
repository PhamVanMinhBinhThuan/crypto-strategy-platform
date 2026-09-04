package com.cryptostrategy.platform.api.experiment;

import com.cryptostrategy.platform.api.auth.AuthenticatedUserContext;
import com.cryptostrategy.platform.execution.api.ReproductionVerificationId;
import com.cryptostrategy.platform.execution.api.port.in.GetSearchReproductionVerificationUseCase;
import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.error.ResourceInaccessibleException;
import com.cryptostrategy.platform.api.transport.TypedUlidSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/experiments")
public final class ReproductionVerificationController {
    private final GetSearchReproductionVerificationUseCase verifications;

    public ReproductionVerificationController(GetSearchReproductionVerificationUseCase verifications) {
        this.verifications = Objects.requireNonNull(verifications, "verifications");
    }

    @GetMapping("/{id}/reproduction-verification")
    public Response get(@AuthenticationPrincipal AuthenticatedUserContext user, @PathVariable String id) {
        return verifications.get(user.userId(), new ExperimentId(id))
                .map(Response::from)
                .orElseThrow(() -> new ResourceInaccessibleException(
                        "Reproduction verification is inaccessible"));
    }

    public record Response(
            @JsonSerialize(using = TypedUlidSerializer.class) ReproductionVerificationId verificationId,
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId sourceExperimentId,
            @JsonSerialize(using = TypedUlidSerializer.class) ExperimentId reproductionExperimentId,
            String status,
            Boolean tradesMatched,
            Boolean metricsMatched,
            Boolean fingerprintsMatched,
            String sourceEvidenceFingerprint,
            String reproductionEvidenceFingerprint,
            Map<String, Object> differences,
            Failure failure,
            Instant startedAt,
            Instant finishedAt,
            Instant updatedAt) {
        static Response from(GetSearchReproductionVerificationUseCase.Snapshot value) {
            Failure failure = value.failureCode() == null
                    ? null
                    : new Failure(value.failureCode(), value.failureMessage());
            return new Response(value.verificationId(), value.sourceExperimentId(),
                    value.reproductionExperimentId(), value.status(), value.tradesMatched(),
                    value.metricsMatched(), value.fingerprintsMatched(),
                    value.sourceEvidenceFingerprint(), value.reproductionEvidenceFingerprint(),
                    value.differences(), failure, value.startedAt(), value.finishedAt(), value.updatedAt());
        }
    }

    public record Failure(String code, String message) {}
}
