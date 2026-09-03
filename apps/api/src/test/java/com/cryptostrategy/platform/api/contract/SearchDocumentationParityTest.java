package com.cryptostrategy.platform.api.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SearchDocumentationParityTest {
    @Test
    void f010PublicAndEventContractsMatchTheActivatedImplementation() throws Exception {
        String openApi = document("docs/api/openapi.yaml");
        String errors = document("docs/api/error-catalog.md");
        String events = document("docs/api/websocket-events.md");
        String flow = document("docs/architecture/data-flows.md");

        assertThat(openApi).contains("operationId: startExperiment", "operationId: reproduceExperiment",
                "'202':", "ExperimentAccepted").doesNotContain("x-readiness: BLOCKED_SEARCH_COORDINATOR");
        assertThat(errors).contains("START_EXPERIMENT", "REPRODUCE_EXPERIMENT",
                "IDEMPOTENCY_KEY_CONFLICT", "verification chạy bất đồng bộ");
        assertThat(events).contains("search.requests.v1", "search-coordinators",
                "messageType", "messageVersion");
        assertThat(flow).contains("PENDING/RUNNING", "MATCHED", "MISMATCHED", "FAILED");
    }

    private static String document(String relative) throws Exception {
        Path direct = Path.of(relative);
        Path path = Files.exists(direct) ? direct : Path.of("..", "..", relative);
        return Files.readString(path);
    }
}
