package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.parameter.CrossParameterConstraint;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/** One exact strategy version and the finite parameter domains Search may choose from. */
public record SearchStrategyPoolEntry(
        StrategyReference strategy,
        SortedMap<String, SearchParameterDomain> parameterDomains,
        List<CrossParameterConstraint> constraints)
        implements Comparable<SearchStrategyPoolEntry> {

    public SearchStrategyPoolEntry(StrategyReference strategy, Map<String, SearchParameterDomain> domains) {
        this(strategy, canonical(domains), List.of());
    }

    public SearchStrategyPoolEntry(StrategyReference strategy, Map<String, SearchParameterDomain> domains,
            List<CrossParameterConstraint> constraints) {
        this(strategy, canonical(domains), constraints);
    }

    public SearchStrategyPoolEntry {
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(parameterDomains, "parameterDomains");
        Objects.requireNonNull(constraints, "constraints");
        parameterDomains = Collections.unmodifiableSortedMap(new TreeMap<>(parameterDomains));
        constraints = constraints.stream().sorted(Comparator
                .comparing(CrossParameterConstraint::lowerParameter)
                .thenComparing(CrossParameterConstraint::upperParameter)).toList();
        if (constraints.size() > 1) {
            throw new IllegalArgumentException("F-015 supports one ordered parameter constraint per Strategy");
        }
        SortedMap<String, SearchParameterDomain> validatedDomains = parameterDomains;
        constraints.forEach(constraint -> {
            SearchParameterDomain lower = validatedDomains.get(constraint.lowerParameter());
            SearchParameterDomain upper = validatedDomains.get(constraint.upperParameter());
            if (lower == null || upper == null || !numeric(lower.type()) || !numeric(upper.type())) {
                throw new IllegalArgumentException("Ordered constraint must reference numeric parameter domains");
            }
        });
        if (count(parameterDomains, constraints).signum() == 0) {
            throw new IllegalArgumentException("Strategy parameter constraints produce an empty domain");
        }
    }

    public BigInteger combinationCount() {
        return count(parameterDomains, constraints);
    }

    /** Resolves one valid assignment without materializing the entry's Cartesian product. */
    public StrategyParameterSet parametersAt(BigInteger ordinal) {
        if (ordinal.signum() < 0 || ordinal.compareTo(combinationCount()) >= 0) {
            throw new IllegalArgumentException("Strategy parameter ordinal is outside the domain");
        }
        TreeMap<String, StrategyParameterValue> values = new TreeMap<>();
        if (constraints.isEmpty()) {
            decodeIndependent(parameterDomains, ordinal, values);
            return StrategyParameterSet.of(values);
        }
        CrossParameterConstraint constraint = constraints.getFirst();
        for (var domain : parameterDomains.entrySet()) {
            if (domain.getKey().equals(constraint.lowerParameter())
                    || domain.getKey().equals(constraint.upperParameter())) continue;
            BigInteger optionCount = BigInteger.valueOf(domain.getValue().options().size());
            BigInteger[] qr = ordinal.divideAndRemainder(optionCount);
            ordinal = qr[0];
            values.put(domain.getKey(), domain.getValue().options().get(qr[1].intValueExact()));
        }
        for (StrategyParameterValue left : parameterDomains.get(constraint.lowerParameter()).options()) {
            for (StrategyParameterValue right : parameterDomains.get(constraint.upperParameter()).options()) {
                if (number(left).compareTo(number(right)) >= 0) continue;
                if (ordinal.signum() == 0) {
                    values.put(constraint.lowerParameter(), left);
                    values.put(constraint.upperParameter(), right);
                    return StrategyParameterSet.of(values);
                }
                ordinal = ordinal.subtract(BigInteger.ONE);
            }
        }
        throw new IllegalStateException("Valid parameter ordinal could not be decoded");
    }

    @Override
    public int compareTo(SearchStrategyPoolEntry other) {
        return strategy.compareTo(other.strategy);
    }

    private static BigInteger count(SortedMap<String, SearchParameterDomain> domains,
            List<CrossParameterConstraint> constraints) {
        if (constraints.isEmpty()) {
            return domains.values().stream()
                    .map(domain -> BigInteger.valueOf(domain.options().size()))
                    .reduce(BigInteger.ONE, BigInteger::multiply);
        }
        CrossParameterConstraint constraint = constraints.getFirst();
        BigInteger validPairs = BigInteger.ZERO;
        for (StrategyParameterValue left : domains.get(constraint.lowerParameter()).options()) {
            for (StrategyParameterValue right : domains.get(constraint.upperParameter()).options()) {
                if (number(left).compareTo(number(right)) < 0) validPairs = validPairs.add(BigInteger.ONE);
            }
        }
        BigInteger total = validPairs;
        for (var entry : domains.entrySet()) {
            if (!entry.getKey().equals(constraint.lowerParameter())
                    && !entry.getKey().equals(constraint.upperParameter())) {
                total = total.multiply(BigInteger.valueOf(entry.getValue().options().size()));
            }
        }
        return total;
    }

    private static void decodeIndependent(SortedMap<String, SearchParameterDomain> domains,
            BigInteger ordinal, TreeMap<String, StrategyParameterValue> values) {
        for (var domain : domains.entrySet()) {
            BigInteger optionCount = BigInteger.valueOf(domain.getValue().options().size());
            BigInteger[] qr = ordinal.divideAndRemainder(optionCount);
            ordinal = qr[0];
            values.put(domain.getKey(), domain.getValue().options().get(qr[1].intValueExact()));
        }
    }

    private static boolean numeric(ParameterType type) {
        return type == ParameterType.INTEGER || type == ParameterType.DECIMAL;
    }

    private static BigDecimal number(StrategyParameterValue value) {
        return switch (value) {
            case StrategyParameterValue.IntegerValue item -> BigDecimal.valueOf(item.value());
            case StrategyParameterValue.DecimalValue item -> item.value();
            default -> throw new IllegalArgumentException("Ordered constraint value must be numeric");
        };
    }

    private static SortedMap<String, SearchParameterDomain> canonical(
            Map<String, SearchParameterDomain> source) {
        Objects.requireNonNull(source, "parameterDomains");
        TreeMap<String, SearchParameterDomain> result = new TreeMap<>();
        source.forEach((name, domain) -> {
            if (name == null || !name.matches("^[a-z][A-Za-z0-9]*$")) {
                throw new IllegalArgumentException("Invalid Search parameter name: " + name);
            }
            result.put(name, Objects.requireNonNull(domain, "parameter domain"));
        });
        return result;
    }
}
