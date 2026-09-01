package com.cryptostrategy.platform.news.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record SentimentRequestId(String value) implements UlidIdentifier {
    public SentimentRequestId { value = Ulids.requireValid(value); }
    public static SentimentRequestId generate() { return new SentimentRequestId(Ulids.generate()); }
    @Override public String toString() { return value; }
}
