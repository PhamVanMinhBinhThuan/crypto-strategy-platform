package com.cryptostrategy.platform.search.api.model;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

public record SearchSpace(SortedMap<String, SearchParameterDomain> parameters) {
    private static final Pattern PARAMETER_NAME = Pattern.compile("^[a-z][A-Za-z0-9]*$");

    public SearchSpace(Map<String, SearchParameterDomain> parameters) {
        this(canonicalize(parameters));
    }

    public SearchSpace {
        Objects.requireNonNull(parameters, "parameters");
        parameters = Collections.unmodifiableSortedMap(new TreeMap<>(parameters));
    }

    public BigInteger combinationCount() {
        return parameters.values().stream()
                .map(domain -> BigInteger.valueOf(domain.options().size()))
                .reduce(BigInteger.ONE, BigInteger::multiply);
    }

    private static SortedMap<String, SearchParameterDomain> canonicalize(Map<String, SearchParameterDomain> source) {
        Objects.requireNonNull(source, "parameters");
        TreeMap<String, SearchParameterDomain> canonical = new TreeMap<>();
        source.forEach((name, domain) -> {
            Objects.requireNonNull(name, "parameter name");
            Objects.requireNonNull(domain, "parameter domain");
            if (!PARAMETER_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Invalid search parameter name: " + name);
            }
            canonical.put(name, domain);
        });
        return canonical;
    }
}
