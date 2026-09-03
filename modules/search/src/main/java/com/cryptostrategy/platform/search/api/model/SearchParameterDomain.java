package com.cryptostrategy.platform.search.api.model;

import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record SearchParameterDomain(ParameterType type, List<StrategyParameterValue> options) {
    public SearchParameterDomain {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(options, "options");
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Search parameter domain must not be empty");
        }

        List<StrategyParameterValue> canonical = new ArrayList<>(options.size());
        Set<String> seen = new HashSet<>();
        for (StrategyParameterValue option : options) {
            Objects.requireNonNull(option, "option");
            if (option.type() != type) {
                throw new IllegalArgumentException("Search parameter option type mismatch");
            }
            String key = type.name() + ':' + option.canonicalText();
            if (!seen.add(key)) {
                throw new IllegalArgumentException("Search parameter domain contains duplicate canonical option");
            }
            canonical.add(option);
        }
        canonical.sort(optionComparator(type));
        options = Collections.unmodifiableList(canonical);
    }

    private static Comparator<StrategyParameterValue> optionComparator(ParameterType type) {
        return switch (type) {
            case INTEGER -> Comparator.comparingLong(value -> ((StrategyParameterValue.IntegerValue) value).value());
            case DECIMAL -> (left, right) -> ((StrategyParameterValue.DecimalValue) left).value()
                    .compareTo(((StrategyParameterValue.DecimalValue) right).value());
            case BOOLEAN -> Comparator.comparing(value -> ((StrategyParameterValue.BooleanValue) value).value());
            case TEXT -> Comparator.comparing(value -> ((StrategyParameterValue.TextValue) value).value());
            case ENUM -> Comparator.comparing(value -> ((StrategyParameterValue.EnumValue) value).value());
        };
    }
}
