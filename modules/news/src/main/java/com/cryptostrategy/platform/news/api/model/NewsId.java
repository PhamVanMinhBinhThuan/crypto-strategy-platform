package com.cryptostrategy.platform.news.api.model;

import com.cryptostrategy.platform.domain.api.identity.UlidIdentifier;
import com.cryptostrategy.platform.domain.api.identity.Ulids;

public record NewsId(String value) implements UlidIdentifier {
    public NewsId { value = Ulids.requireValid(value); }
    public static NewsId generate() { return new NewsId(Ulids.generate()); }
    @Override public String toString() { return value; }
}
