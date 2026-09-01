package com.cryptostrategy.platform.news.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record SentimentResultId(String value) implements UlidIdentifier {
    public SentimentResultId { value = Ulids.requireValid(value); }
    public static SentimentResultId generate() { return new SentimentResultId(Ulids.generate()); }
    @Override public String toString() { return value; }
}
