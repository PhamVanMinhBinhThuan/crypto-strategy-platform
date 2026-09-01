package com.cryptostrategy.platform.contracts.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CandidateEvaluatedPayload(
        @JsonProperty("experimentId") MessageUlid experimentId,
        @JsonProperty("jobId") MessageUlid jobId,
        @JsonProperty("candidateId") MessageUlid candidateId,
        @JsonProperty("backtestResultId") MessageUlid backtestResultId,
        @JsonProperty("evaluationResultId") MessageUlid evaluationResultId,
        @JsonProperty("overallScore") BigDecimal overallScore
) {
    public CandidateEvaluatedPayload {
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(candidateId, "candidateId cannot be null");
        Objects.requireNonNull(backtestResultId, "backtestResultId cannot be null");
        Objects.requireNonNull(evaluationResultId, "evaluationResultId cannot be null");
        Objects.requireNonNull(overallScore, "overallScore cannot be null");
    }

    public CandidateEvaluatedPayload(
            String experimentId,
            String jobId,
            String candidateId,
            String backtestResultId,
            String evaluationResultId,
            BigDecimal overallScore
    ) {
        this(
                MessageUlid.of(experimentId),
                MessageUlid.of(jobId),
                MessageUlid.of(candidateId),
                MessageUlid.of(backtestResultId),
                MessageUlid.of(evaluationResultId),
                overallScore
        );
    }

    @JsonCreator
    public static CandidateEvaluatedPayload of(
            @JsonProperty("experimentId") String experimentId,
            @JsonProperty("jobId") String jobId,
            @JsonProperty("candidateId") String candidateId,
            @JsonProperty("backtestResultId") String backtestResultId,
            @JsonProperty("evaluationResultId") String evaluationResultId,
            @JsonProperty("overallScore") BigDecimal overallScore
    ) {
        return new CandidateEvaluatedPayload(
                experimentId,
                jobId,
                candidateId,
                backtestResultId,
                evaluationResultId,
                overallScore
        );
    }
}
