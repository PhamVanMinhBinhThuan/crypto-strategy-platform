package com.cryptostrategy.platform.persistence.internal.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SearchDefinitionJsonMapperTest {
    private final SearchDefinitionJsonMapper mapper = new SearchDefinitionJsonMapper();

    @Test
    void readsLegacyV1WithoutInventingACompositeDiscriminator() {
        Map<String, Object> decoded = mapper.readSearchConfig(
                "{\"maximumCandidates\":10,\"searchSpace\":{\"period\":[7,14]}}");

        assertThat(decoded).containsEntry("maximumCandidates", 10);
        assertThat(decoded).doesNotContainKey("contractVersion");
    }

    @Test
    void encodesAndReadsV2WithoutLosingItsCompositeSchema() {
        Map<String, Object> candidate = Map.of(
                "schemaVersion", 2,
                "components", List.of(Map.of("strategyId", "rsi", "parameters", Map.of("period", 14))),
                "combinationPolicy", Map.of("policyId", "majority-vote", "version", "1.0.0"));

        String encoded = mapper.writeCandidateDefinition(candidate);

        assertThat(mapper.readCandidateDefinition(encoded)).isEqualTo(candidate);
        assertThat(encoded).contains("\"schemaVersion\":2", "\"components\"");
    }

    @Test
    void rejectsUnknownOrIncompleteVersionedDefinitions() {
        assertThatThrownBy(() -> mapper.readSearchConfig(
                "{\"contractVersion\":\"search-config-v2\",\"searchSpace\":{}}"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> mapper.readCandidateDefinition("{\"schemaVersion\":3}"))
                .isInstanceOf(IllegalStateException.class);
    }
}
