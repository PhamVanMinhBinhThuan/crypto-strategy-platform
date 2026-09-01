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
    public SearchRequestPayload {
        Objects.requireNonNull(searchJobId, "searchJobId cannot be null");
        Objects.requireNonNull(experimentId, "experimentId cannot be null");
        if (concurrencyHint < 1) concurrencyHint = 1;
        if (topKTarget < 1) topKTarget = 10;
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
            @JsonProperty("concurrencyHint") int concurrencyHint,
            @JsonProperty("topKTarget") int topKTarget
    ) {
        return new SearchRequestPayload(
                searchJobId,
                experimentId,
                concurrencyHint,
                topKTarget
        );
    }
}
