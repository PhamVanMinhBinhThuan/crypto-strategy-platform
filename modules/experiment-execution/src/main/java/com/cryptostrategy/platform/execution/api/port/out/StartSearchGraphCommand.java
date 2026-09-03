package com.cryptostrategy.platform.execution.api.port.out;

import com.cryptostrategy.platform.experiment.api.Experiment;
import com.cryptostrategy.platform.experiment.api.ExperimentManifest;
import com.cryptostrategy.platform.experiment.api.job.Job;
import com.cryptostrategy.platform.experiment.api.outbox.OutboxEvent;
import com.cryptostrategy.platform.search.api.model.SearchRun;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StartSearchGraphCommand(
        UUID ownerUserId,
        String operation,
        String idempotencyKey,
        String requestHash,
        Instant receiptExpiresAt,
        Experiment experiment,
        ExperimentManifest manifest,
        Job searchJob,
        SearchRun searchRun,
        OutboxEvent searchRequest) {
    public StartSearchGraphCommand {
        Objects.requireNonNull(ownerUserId, "ownerUserId");
        operation = requireText(operation, "operation");
        idempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        requestHash = requireText(requestHash, "requestHash");
        Objects.requireNonNull(receiptExpiresAt, "receiptExpiresAt");
        Objects.requireNonNull(experiment, "experiment");
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(searchJob, "searchJob");
        Objects.requireNonNull(searchRun, "searchRun");
        Objects.requireNonNull(searchRequest, "searchRequest");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
