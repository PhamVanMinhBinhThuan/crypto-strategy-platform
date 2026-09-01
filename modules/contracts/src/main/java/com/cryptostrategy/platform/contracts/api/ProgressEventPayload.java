package com.cryptostrategy.platform.contracts.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProgressEventPayload(
        @JsonProperty("experimentId") MessageUlid experimentId,
        @JsonProperty("jobId") MessageUlid jobId,
        @JsonProperty("completedWork") int completedWork,
        @JsonProperty("failedWork") int failedWork,
        @JsonProperty("totalWork") int totalWork,
        @JsonProperty("bestScore") BigDecimal bestScore,
        @JsonProperty("leaderboardRevisionId") MessageUlid leaderboardRevisionId,
        @JsonProperty("eventType") String eventType
) {
    public ProgressEventPayload {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        if (completedWork < 0) throw new IllegalArgumentException("completedWork must be >= 0");
        if (failedWork < 0) throw new IllegalArgumentException("failedWork must be >= 0");
        if (totalWork < 0) throw new IllegalArgumentException("totalWork must be >= 0");
    }

    public ProgressEventPayload(
            String experimentId,
            String jobId,
            int completedWork,
            int failedWork,
            int totalWork,
            BigDecimal bestScore,
            String leaderboardRevisionId,
            String eventType
    ) {
        this(
                MessageUlid.of(experimentId),
                MessageUlid.of(jobId),
                completedWork,
                failedWork,
                totalWork,
                bestScore,
                leaderboardRevisionId != null ? MessageUlid.of(leaderboardRevisionId) : null,
                eventType
        );
    }

    @JsonCreator
    public static ProgressEventPayload of(
            @JsonProperty("experimentId") String experimentId,
            @JsonProperty("jobId") String jobId,
            @JsonProperty("completedWork") int completedWork,
            @JsonProperty("failedWork") int failedWork,
            @JsonProperty("totalWork") int totalWork,
            @JsonProperty("bestScore") BigDecimal bestScore,
            @JsonProperty("leaderboardRevisionId") String leaderboardRevisionId,
            @JsonProperty("eventType") String eventType
    ) {
        return new ProgressEventPayload(
                experimentId,
                jobId,
                completedWork,
                failedWork,
                totalWork,
                bestScore,
                leaderboardRevisionId,
                eventType
        );
    }
}
