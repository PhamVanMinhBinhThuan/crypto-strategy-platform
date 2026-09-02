package com.cryptostrategy.platform.api.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class F009ContractFixtureTest {
    private static final String ROOT = "contracts/f009/";
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void marketAndDatasetFixturesKeepUtcAndExactDecimalStrings() throws Exception {
        JsonNode market = fixture("market-page.json");
        JsonNode candle = market.path("items").get(0);
        assertThat(candle.path("openTime").asText()).endsWith("Z");
        assertThat(candle.path("closeTime").asText()).endsWith("Z");
        for (String field : List.of("open", "high", "low", "close", "volume")) {
            assertThat(candle.path(field).isTextual()).as(field).isTrue();
        }

        JsonNode dataset = fixture("dataset-snapshot.json");
        assertThat(dataset.path("datasetId").isTextual()).isTrue();
        assertThat(dataset.path("startTime").asText()).endsWith("Z");
        assertThat(dataset.path("endTime").asText()).endsWith("Z");
    }

    @Test
    void strategyFixturesKeepTypedIdentitiesAsScalarsAndCanonicalParameters()
            throws Exception {
        JsonNode catalog = fixture("strategy-catalog.json");
        JsonNode descriptor = catalog.path("items").get(0);
        assertThat(descriptor.path("strategyVersionId").isTextual()).isTrue();
        assertThat(descriptor.path("parameters").get(2).path("defaultValue").asText())
                .isEqualTo("0.100000000001");

        JsonNode privateStrategy = fixture("private-strategy.json");
        assertThat(privateStrategy.path("userStrategyId").isTextual()).isTrue();
        JsonNode latest = privateStrategy.path("latestVersion");
        assertThat(latest.path("userStrategyVersionId").isTextual()).isTrue();
        assertThat(latest.path("source")
                        .path("strategy")
                        .path("parameters")
                        .path("threshold")
                        .asText())
                .isEqualTo("0.100000000001");
    }

    private JsonNode fixture(String name) throws IOException {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(ROOT + name)) {
            assertThat(input).as("fixture %s", name).isNotNull();
            return json.readTree(input);
        }
    }
}
