package com.cryptostrategy.platform.contracts.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeadLetterPayload(
        @JsonProperty("experimentId") MessageUlid experimentId,
        @JsonProperty("jobId") MessageUlid jobId,
        @JsonProperty("candidateId") MessageUlid candidateId,
        @JsonProperty("messageId") MessageUlid messageId,
        @JsonProperty("failureClassification") String failureClassification,
        @JsonProperty("failureCode") String failureCode,
        @JsonProperty("safeDiagnosticReference") String safeDiagnosticReference,
        @JsonProperty("attemptCount") int attemptCount,
        @JsonProperty("failedAt") Instant failedAt
) {
    public DeadLetterPayload {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(messageId, "messageId cannot be null");
        Objects.requireNonNull(failureClassification, "failureClassification cannot be null");
        Objects.requireNonNull(failureCode, "failureCode cannot be null");
        if (attemptCount < 1) attemptCount = 1;
        if (failedAt == null) failedAt = Instant.now();
    }

    public DeadLetterPayload(
            String experimentId,
            String jobId,
            String candidateId,
            String messageId,
            String failureClassification,
            String failureCode,
            String safeDiagnosticReference,
            int attemptCount,
            Instant failedAt
    ) {
        this(
                MessageUlid.of(experimentId),
                MessageUlid.of(jobId),
                candidateId != null ? MessageUlid.of(candidateId) : null,
                MessageUlid.of(messageId),
                failureClassification,
                failureCode,
                safeDiagnosticReference,
                attemptCount,
                failedAt
        );
    }

    @JsonCreator
    public static DeadLetterPayload of(
            @JsonProperty("experimentId") String experimentId,
            @JsonProperty("jobId") String jobId,
            @JsonProperty("candidateId") String candidateId,
            @JsonProperty("messageId") String messageId,
            @JsonProperty("failureClassification") String failureClassification,
            @JsonProperty("failureCode") String failureCode,
            @JsonProperty("safeDiagnosticReference") String safeDiagnosticReference,
            @JsonProperty("attemptCount") int attemptCount,
            @JsonProperty("failedAt") Instant failedAt
    ) {
        return new DeadLetterPayload(
                experimentId,
                jobId,
                candidateId,
                messageId,
                failureClassification,
                failureCode,
                safeDiagnosticReference,
                attemptCount,
                failedAt
        );
    }
}
