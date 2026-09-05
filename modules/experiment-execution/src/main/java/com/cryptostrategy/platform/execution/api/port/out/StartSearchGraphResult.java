package com.cryptostrategy.platform.execution.api.port.out;

import com.cryptostrategy.platform.experiment.api.ExperimentId;
import com.cryptostrategy.platform.experiment.api.job.JobId;
import java.util.Objects;

public record StartSearchGraphResult(Status status, ExperimentId experimentId, JobId searchJobId,
        com.cryptostrategy.platform.search.api.model.SearchRunId searchRunId,
        String configurationFingerprint, int configurationVersion) {
    public enum Status { CREATED, REPLAY, CONFLICT }

    public StartSearchGraphResult {
        Objects.requireNonNull(status, "status");
        if (status != Status.CONFLICT) {
            Objects.requireNonNull(experimentId, "experimentId");
            Objects.requireNonNull(searchJobId, "searchJobId");
        }
    }

    public StartSearchGraphResult(Status status, ExperimentId experimentId, JobId searchJobId) {
        this(status, experimentId, searchJobId, null, null, 1);
    }

    public StartSearchGraphResult asReplay() {
        return status == Status.CONFLICT ? this : new StartSearchGraphResult(Status.REPLAY,
                experimentId, searchJobId, searchRunId, configurationFingerprint, configurationVersion);
    }

    public static StartSearchGraphResult conflict() {
        return new StartSearchGraphResult(Status.CONFLICT, null, null, null, null, 1);
    }
}
