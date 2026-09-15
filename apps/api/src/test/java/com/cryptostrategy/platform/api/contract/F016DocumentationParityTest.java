package com.cryptostrategy.platform.api.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class F016DocumentationParityTest {
    @Test
    void documentsSentimentCatalogParametersAndOptionalResultProvenance() throws Exception {
        String openApi = document("docs/api/openapi.yaml");
        String resultContract = document("specs/016-sentiment-strategy/contracts/rest-api-contract.md");

        assertThat(openApi).contains(
                "SentimentProvenance:",
                "SentimentDecisionEvidence:",
                "sentimentProvenance:",
                "snapshotFingerprint:",
                "preprocessingVersion:",
                "evidenceFingerprint:",
                "searchRangeHint:");
        assertThat(resultContract).contains(
                "sentiment-polarity", "lookbackHours", "minimumArticles",
                "buyThreshold", "sellThreshold", "SENTIMENT_SNAPSHOT_UNAVAILABLE");
    }

    private static String document(String relativePath) throws Exception {
        Path direct = Path.of(relativePath);
        return Files.readString(Files.exists(direct) ? direct : Path.of("..", "..", relativePath));
    }
}
