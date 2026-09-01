package com.cryptostrategy.platform.contracts.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BacktestJobPayload(
        @JsonProperty("experimentId") MessageUlid experimentId,
        @JsonProperty("jobId") MessageUlid jobId,
        @JsonProperty("candidateId") MessageUlid candidateId
) {
    public BacktestJobPayload {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(candidateId, "candidateId cannot be null");
    }

    public BacktestJobPayload(String experimentId, String jobId, String candidateId) {
        this(MessageUlid.of(experimentId), MessageUlid.of(jobId), MessageUlid.of(candidateId));
    }

    @JsonCreator
    public static BacktestJobPayload of(
            @JsonProperty("experimentId") String experimentId,
            @JsonProperty("jobId") String jobId,
            @JsonProperty("candidateId") String candidateId
    ) {
        return new BacktestJobPayload(experimentId, jobId, candidateId);
    }
}
