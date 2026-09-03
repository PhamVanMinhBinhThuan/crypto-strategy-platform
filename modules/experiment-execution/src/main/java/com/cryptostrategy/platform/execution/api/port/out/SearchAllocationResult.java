package com.cryptostrategy.platform.execution.api.port.out;

import com.cryptostrategy.platform.experiment.api.CandidateId;
import com.cryptostrategy.platform.experiment.api.job.JobId;

public record SearchAllocationResult(Status status, CandidateId candidateId, JobId backtestJobId, long runVersion) {
    public enum Status { ALLOCATED, STALE_FENCE, WINDOW_FULL, STOPPED, EXHAUSTED }

    public static SearchAllocationResult allocated(CandidateId candidateId, JobId jobId, long version) {
        return new SearchAllocationResult(Status.ALLOCATED, candidateId, jobId, version);
    }

    public static SearchAllocationResult stale(long version) {
        return new SearchAllocationResult(Status.STALE_FENCE, null, null, version);
    }
}
