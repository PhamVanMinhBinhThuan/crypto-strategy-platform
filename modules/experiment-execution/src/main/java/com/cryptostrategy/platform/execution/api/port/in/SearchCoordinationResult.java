package com.cryptostrategy.platform.execution.api.port.in;

import com.cryptostrategy.platform.search.api.model.SearchRunId;

import com.cryptostrategy.platform.search.api.model.SearchRunStatus;
import java.util.Objects;

public record SearchCoordinationResult(
        SearchRunId searchRunId,
        int allocatedWork,
        int activeWork,
        int completedWork,
        int failedWork,
        SearchRunStatus status) {
    public SearchCoordinationResult {
        Objects.requireNonNull(searchRunId, "searchRunId");
        Objects.requireNonNull(status, "status");
        if (allocatedWork < 0 || activeWork < 0 || completedWork < 0 || failedWork < 0) {
            throw new IllegalArgumentException("progress counters must be non-negative");
        }
    }
}
