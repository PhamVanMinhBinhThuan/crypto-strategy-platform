package com.cryptostrategy.platform.contracts.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LifecycleNotificationPayload(
        @JsonProperty("aggregateType") String aggregateType,
        @JsonProperty("aggregateId") MessageUlid aggregateId,
        @JsonProperty("experimentId") MessageUlid experimentId,
        @JsonProperty("jobId") MessageUlid jobId,
        @JsonProperty("candidateId") MessageUlid candidateId,
        @JsonProperty("lifecycleEventType") String lifecycleEventType
) {
    public LifecycleNotificationPayload {
        Objects.requireNonNull(aggregateType, "aggregateType cannot be null");
        Objects.requireNonNull(aggregateId, "aggregateId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(lifecycleEventType, "lifecycleEventType cannot be null");
    }

    public LifecycleNotificationPayload(
            String aggregateType,
            String aggregateId,
            String experimentId,
            String jobId,
            String candidateId,
            String lifecycleEventType
    ) {
        this(
                aggregateType,
                MessageUlid.of(aggregateId),
                MessageUlid.of(experimentId),
                jobId != null ? MessageUlid.of(jobId) : null,
                candidateId != null ? MessageUlid.of(candidateId) : null,
                lifecycleEventType
        );
    }

    @JsonCreator
    public static LifecycleNotificationPayload of(
            @JsonProperty("aggregateType") String aggregateType,
            @JsonProperty("aggregateId") String aggregateId,
            @JsonProperty("experimentId") String experimentId,
            @JsonProperty("jobId") String jobId,
            @JsonProperty("candidateId") String candidateId,
            @JsonProperty("lifecycleEventType") String lifecycleEventType
    ) {
        return new LifecycleNotificationPayload(
                aggregateType,
                aggregateId,
                experimentId,
                jobId,
                candidateId,
                lifecycleEventType
        );
    }
}
