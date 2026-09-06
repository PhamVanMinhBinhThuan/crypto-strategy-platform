package com.cryptostrategy.platform.search.api.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Canonical finite strategy-pool space for F-015 composite candidates. */
public record CompositeSearchSpace(
        List<SearchStrategyPoolEntry> strategyPool,
        int minimumComponents,
        int maximumComponents,
        SearchCombinationPolicy combinationPolicy,
        List<String> constraints) {
    public static final int SCHEMA_VERSION = 2;
    private static final int MAX_POOL_SIZE = 20;

    public CompositeSearchSpace {
        Objects.requireNonNull(strategyPool, "strategyPool");
        Objects.requireNonNull(combinationPolicy, "combinationPolicy");
        Objects.requireNonNull(constraints, "constraints");
        if (strategyPool.isEmpty() || strategyPool.size() > MAX_POOL_SIZE) {
            throw new IllegalArgumentException("Strategy pool size must be between 1 and 20");
        }
        ArrayList<SearchStrategyPoolEntry> canonicalPool = new ArrayList<>(strategyPool);
        canonicalPool.sort(SearchStrategyPoolEntry::compareTo);
        if (new HashSet<>(canonicalPool.stream().map(SearchStrategyPoolEntry::strategy).toList()).size()
                != canonicalPool.size()) {
            throw new IllegalArgumentException("Strategy pool contains duplicate strategy versions");
        }
        if (minimumComponents < 1 || maximumComponents < minimumComponents
                || maximumComponents > canonicalPool.size()) {
            throw new IllegalArgumentException("Invalid component-count bounds");
        }
        strategyPool = Collections.unmodifiableList(canonicalPool);
        constraints = constraints.stream().map(value -> {
            Objects.requireNonNull(value, "constraint");
            if (value.isBlank()) throw new IllegalArgumentException("Constraint must not be blank");
            return value;
        }).sorted().distinct().toList();
    }

    public BigInteger combinationCount() {
        BigInteger total = BigInteger.ZERO;
        for (int size = minimumComponents; size <= maximumComponents; size++) {
            total = total.add(countSubsets(0, size, BigInteger.ONE));
        }
        return total;
    }

    private BigInteger countSubsets(int start, int remaining, BigInteger product) {
        if (remaining == 0) return product;
        BigInteger total = BigInteger.ZERO;
        for (int index = start; index <= strategyPool.size() - remaining; index++) {
            total = total.add(countSubsets(index + 1, remaining - 1,
                    product.multiply(strategyPool.get(index).combinationCount())));
        }
        return total;
    }
}
