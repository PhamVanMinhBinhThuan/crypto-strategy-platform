package com.cryptostrategy.platform.contracts.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchRequestPayload(
        @JsonProperty("searchJobId") MessageUlid searchJobId,
        @JsonProperty("experimentId") MessageUlid experimentId,
        @JsonProperty("concurrencyHint") int concurrencyHint,
        @JsonProperty("topKTarget") int topKTarget
) {
    public static final int DEFAULT_CONCURRENCY_HINT = 1;
    public static final int MAX_CONCURRENCY_HINT = 64;
    public static final int DEFAULT_TOP_K_TARGET = 10;
    public static final int MAX_TOP_K_TARGET = 1_000;

    public SearchRequestPayload {
        Objects.requireNonNull(searchJobId, "searchJobId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        if (concurrencyHint < 1 || concurrencyHint > MAX_CONCURRENCY_HINT) {
            throw new IllegalArgumentException("concurrencyHint must be between 1 and " + MAX_CONCURRENCY_HINT);
        }
        if (topKTarget < 1 || topKTarget > MAX_TOP_K_TARGET) {
            throw new IllegalArgumentException("topKTarget must be between 1 and " + MAX_TOP_K_TARGET);
        }
    }

    public SearchRequestPayload(
            String searchJobId,
            String experimentId,
            int concurrencyHint,
            int topKTarget
    ) {
        this(
                MessageUlid.of(searchJobId),
                MessageUlid.of(experimentId),
                concurrencyHint,
                topKTarget
        );
    }

    @JsonCreator
    public static SearchRequestPayload of(
            @JsonProperty("searchJobId") String searchJobId,
            @JsonProperty("experimentId") String experimentId,
            @JsonProperty("concurrencyHint") Integer concurrencyHint,
            @JsonProperty("topKTarget") Integer topKTarget
    ) {
        return new SearchRequestPayload(
                searchJobId,
                experimentId,
                concurrencyHint != null ? concurrencyHint : DEFAULT_CONCURRENCY_HINT,
                topKTarget != null ? topKTarget : DEFAULT_TOP_K_TARGET
        );
    }
}
