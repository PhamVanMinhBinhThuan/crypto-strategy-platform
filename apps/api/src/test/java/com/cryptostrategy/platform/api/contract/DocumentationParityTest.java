package com.cryptostrategy.platform.api.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptostrategy.platform.api.backtest.ResultDtos;
import com.cryptostrategy.platform.api.experiment.ReadDtos;
import com.cryptostrategy.platform.api.leaderboard.LeaderboardDtos;
import com.cryptostrategy.platform.api.news.NewsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DocumentationParityTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void openApiContainsReleasedRoutesAndTransportFields() throws Exception {
        String openApi = documentation("docs/api/openapi.yaml");

        assertThat(openApi).contains(
                "  /backtests:",
                "  /backtest-results/{resultId}:",
                "  /jobs/{jobId}:",
                "  /jobs/{jobId}/cancel:",
                "  /experiments/{experimentId}/candidates:",
                "  /experiments/{experimentId}/leaderboard:",
                "  /news-items:",
                "executionPriceRule: { type: string, enum: [NEXT_CANDLE_OPEN] }");

        assertRecordFields(openApi, "Job", "BacktestMetrics", ReadDtos.JobResponse.class);
        assertRecordFields(
                openApi,
                "BacktestResult",
                "GeneratorSelection",
                ResultDtos.BacktestResultResponse.class);
        assertRecordFields(
                openApi,
                "Leaderboard",
                "Sentiment",
                LeaderboardDtos.LeaderboardResponse.class);
        assertRecordFields(openApi, "NewsItem", "NewsItemPage", NewsResponse.Item.class);
    }

    @Test
    void searchOperationsAreDocumentedAsReadyAndCommandsRequireIdempotency() throws Exception {
        String openApi = documentation("docs/api/openapi.yaml");
        String start = pathSection(openApi, "/experiments:", "/experiments/{experimentId}:");
        String stop = pathSection(
                openApi,
                "/experiments/{experimentId}/stop:",
                "/experiments/{experimentId}/reproductions:");
        String reproduce = pathSection(
                openApi,
                "/experiments/{experimentId}/reproductions:",
                "/experiments/{experimentId}/candidates:");
        String cancel = pathSection(openApi, "/jobs/{jobId}/cancel:", "/experiments:");

        assertThat(start).contains("IdempotencyKey", "'202':")
                .doesNotContain("BLOCKED_SEARCH_COORDINATOR");
        assertThat(reproduce).contains("IdempotencyKey", "'202':")
                .doesNotContain("BLOCKED_SEARCH_COORDINATOR");
        assertThat(stop).contains("IdempotencyKey", "'202':");
        assertThat(cancel).contains("IdempotencyKey", "'202':");
    }

    @Test
    void websocketAndErrorDocumentsCoverImplementedProtocol() throws Exception {
        String websocket = documentation("docs/api/websocket-events.md");
        String errors = documentation("docs/api/error-catalog.md");
        String examples = documentation("docs/api/examples.md");

        assertThat(websocket).contains(
                "SUBSCRIBE_CANDLES",
                "SUBSCRIBE_EXPERIMENT",
                "SUBSCRIBE_LEADERBOARD",
                "SUBSCRIPTION_CONFIRMED",
                "EXPERIMENT_PROGRESS_UPDATED",
                "BACKTEST_COMPLETED",
                "LEADERBOARD_UPDATED",
                "4001",
                "4002",
                "4008",
                "progress.events.v1",
                "candidate.evaluated.v1");
        assertThat(websocket).doesNotContain("\ndụng nó sau reconnect.");
        assertThat(errors).contains(
                "UNKNOWN_REQUEST_FIELD",
                "IDEMPOTENCY_KEY_CONFLICT",
                "INVALID_STATE_TRANSITION",
                "DEPENDENCY_UNAVAILABLE",
                "REPRODUCE_EXPERIMENT");
        assertThat(examples)
                .contains(
                        "NEXT_CANDLE_OPEN",
                        "tradingPairId=",
                        "\"newsId\"",
                        "\"evaluationResultId\"",
                        "status\": \"QUEUED")
                .doesNotContain("CANDLE_CLOSE", "\"newsItemId\"");
    }

    @Test
    void jobFixturesCoverEveryDurablePublicLifecycle() throws Exception {
        Set<String> statuses = Set.of(
                fixtureStatus("queued.json"),
                fixtureStatus("running.json"),
                fixtureStatus("retry-scheduled.json"),
                fixtureStatus("cancelled.json"),
                fixtureStatus("failed.json"),
                fixtureStatus("succeeded.json"));

        assertThat(statuses).containsExactlyInAnyOrder(
                "QUEUED", "RUNNING", "RETRY_SCHEDULED", "CANCELLED", "FAILED", "SUCCEEDED");
    }

    private String fixtureStatus(String name) throws IOException {
        try (var stream = getClass().getResourceAsStream("/fixtures/f009/jobs/" + name)) {
            assertThat(stream).as("fixture %s", name).isNotNull();
            JsonNode fixture = json.readTree(stream);
            return fixture.path("status").asText();
        }
    }

    private static void assertRecordFields(
            String openApi,
            String schema,
            String nextSchema,
            Class<?> recordType) {
        String section = schemaSection(openApi, schema, nextSchema);
        Set<String> fields = Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
        fields.forEach(field -> assertThat(
                        Set.of("nextCursor", "hasMore").contains(field) ? openApi : section)
                .as("OpenAPI schema %s field %s", schema, field)
                .contains("        " + field + ":"));
    }

    private static String schemaSection(String document, String schema, String nextSchema) {
        return between(document, "    " + schema + ":", "    " + nextSchema + ":");
    }

    private static String pathSection(String document, String path, String nextPath) {
        return between(document, "  " + path, "  " + nextPath);
    }

    private static String between(String document, String start, String end) {
        int from = document.indexOf(start);
        int to = document.indexOf(end, from + start.length());
        assertThat(from).as("start marker %s", start).isGreaterThanOrEqualTo(0);
        assertThat(to).as("end marker %s", end).isGreaterThan(from);
        return document.substring(from, to);
    }

    private static String documentation(String relativePath) throws IOException {
        Path direct = Path.of(relativePath);
        Path fromModule = Path.of("..", "..", relativePath);
        Path path = Files.exists(direct) ? direct : fromModule;
        return Files.readString(path);
    }
}
