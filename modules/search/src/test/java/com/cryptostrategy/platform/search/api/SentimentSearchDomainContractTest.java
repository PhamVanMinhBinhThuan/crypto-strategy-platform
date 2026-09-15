package com.cryptostrategy.platform.search.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.search.api.model.SearchParameterDomain;
import com.cryptostrategy.platform.search.api.model.SearchStrategyPoolEntry;
import com.cryptostrategy.platform.strategy.api.model.SemanticVersion;
import com.cryptostrategy.platform.strategy.api.model.StrategyPluginId;
import com.cryptostrategy.platform.strategy.api.model.StrategyReference;
import com.cryptostrategy.platform.strategy.api.model.StrategyVersionId;
import com.cryptostrategy.platform.strategy.api.model.parameter.CrossParameterConstraint;
import com.cryptostrategy.platform.strategy.api.model.parameter.ParameterType;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterSet;
import com.cryptostrategy.platform.strategy.api.model.parameter.StrategyParameterValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class SentimentSearchDomainContractTest {
    @Test
    void sentiment_catalog_entry_exposes_four_finite_typed_domains() {
        SearchStrategyPoolEntry entry = sentimentEntry();

        assertThat(entry.strategy().pluginId().value()).isEqualTo("sentiment-polarity");
        assertThat(entry.parameterDomains()).containsOnlyKeys(
                "lookbackHours", "minimumArticles", "buyThreshold", "sellThreshold");
        assertThat(entry.parameterDomains().get("lookbackHours").type())
                .isEqualTo(ParameterType.INTEGER);
        assertThat(entry.parameterDomains().get("minimumArticles").type())
                .isEqualTo(ParameterType.INTEGER);
        assertThat(entry.parameterDomains().get("buyThreshold").type())
                .isEqualTo(ParameterType.DECIMAL);
        assertThat(entry.parameterDomains().get("sellThreshold").type())
                .isEqualTo(ParameterType.DECIMAL);
        assertThat(entry.constraints()).containsExactly(
                new CrossParameterConstraint("sellThreshold", "buyThreshold"));
        assertThat(entry.combinationCount()).isEqualTo(BigInteger.valueOf(9_720));
    }

    @Test
    void every_sentiment_candidate_stays_inside_the_released_parameter_contract() {
        SearchStrategyPoolEntry entry = sentimentEntry();

        for (int ordinal = 0; ordinal < entry.combinationCount().intValueExact(); ordinal++) {
            StrategyParameterSet candidate = entry.parametersAt(BigInteger.valueOf(ordinal));
            long lookback = integer(candidate, "lookbackHours");
            long minimumArticles = integer(candidate, "minimumArticles");
            BigDecimal buyThreshold = decimal(candidate, "buyThreshold");
            BigDecimal sellThreshold = decimal(candidate, "sellThreshold");

            boolean valid = lookback >= 6 && lookback <= 72 && lookback % 6 == 0
                    && minimumArticles >= 1 && minimumArticles <= 10
                    && buyThreshold.compareTo(BigDecimal.ZERO) > 0
                    && buyThreshold.compareTo(new BigDecimal("0.5")) <= 0
                    && sellThreshold.compareTo(BigDecimal.ZERO) < 0
                    && sellThreshold.compareTo(new BigDecimal("-0.5")) >= 0
                    && sellThreshold.compareTo(buyThreshold) < 0;
            if (!valid) throw new AssertionError("Invalid sentiment candidate ordinal " + ordinal);
        }
    }

    @Test
    void candidate_contains_exactly_the_parameters_declared_by_the_catalog_entry() {
        SearchStrategyPoolEntry entry = sentimentEntry();

        StrategyParameterSet candidate = entry.parametersAt(BigInteger.ZERO);

        assertThat(candidate.values().keySet())
                .isEqualTo(entry.parameterDomains().keySet())
                .isEqualTo(Set.of("lookbackHours", "minimumArticles",
                        "buyThreshold", "sellThreshold"));
    }

    private static SearchStrategyPoolEntry sentimentEntry() {
        return new SearchStrategyPoolEntry(
                new StrategyReference(
                        new StrategyVersionId("01J00000000000000000000040"),
                        new StrategyPluginId("sentiment-polarity"),
                        new SemanticVersion(1, 0, 0)),
                Map.of(
                        "lookbackHours", integers(6, 12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72),
                        "minimumArticles", integers(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                        "buyThreshold", decimals("0.10", "0.15", "0.20", "0.25", "0.30",
                                "0.35", "0.40", "0.45", "0.50"),
                        "sellThreshold", decimals("-0.50", "-0.45", "-0.40", "-0.35",
                                "-0.30", "-0.25", "-0.20", "-0.15", "-0.10")),
                List.of(new CrossParameterConstraint("sellThreshold", "buyThreshold")));
    }

    private static SearchParameterDomain integers(long... values) {
        return new SearchParameterDomain(ParameterType.INTEGER,
                LongStream.of(values)
                        .mapToObj(StrategyParameterValue.IntegerValue::new)
                        .map(StrategyParameterValue.class::cast)
                        .toList());
    }

    private static SearchParameterDomain decimals(String... values) {
        return new SearchParameterDomain(ParameterType.DECIMAL,
                Arrays.stream(values)
                        .map(BigDecimal::new)
                        .map(StrategyParameterValue.DecimalValue::new)
                        .map(StrategyParameterValue.class::cast)
                        .toList());
    }

    private static long integer(StrategyParameterSet candidate, String name) {
        return ((StrategyParameterValue.IntegerValue) candidate.require(name)).value();
    }

    private static BigDecimal decimal(StrategyParameterSet candidate, String name) {
        return ((StrategyParameterValue.DecimalValue) candidate.require(name)).value();
    }
}
