import { describe, expect, it } from "vitest";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { createExperimentCommandService } from "@/src/features/experiments/service/experiment-command-service";
import { initialExperimentDraft } from "@/src/features/experiments/types/experiment-configuration";
import { strategyDescriptorPage } from "@/src/features/experiments/fixtures/experiment-configuration-fixtures";
describe("F-009 Strategy/Create/Reproduce contracts", () => {
  it("uses released Strategy discovery", async () => {
    const api = new MockApiClient().respond("/api/v1/strategies", strategyDescriptorPage);
    const result = await createExperimentCommandService(api).strategies();
    expect(api.requests[0]?.path).toBe("/api/v1/strategies");
    expect(result.ok).toBe(true);
  });
  it.each([["/api/v1/experiments"], ["/api/v1/experiments/experiment-013/reproductions"]] as const)(
    "accepts the released Search Coordinator response on %s",
    async (path) => {
      const api = new MockApiClient().respond(`POST ${path}`, {
        experimentId: "01JNEWEXPERIMENT00000000001",
        jobId: "01JNEWSEARCHJOB000000000001",
        status: "QUEUED"
      });
      const service = createExperimentCommandService(api);
      const result = path.endsWith("reproductions")
        ? await service.reproduce("experiment-013", "key")
        : await service.start(
            { ...initialExperimentDraft, name: "x", datasetId: "dataset" },
            "key"
          );
      expect(result).toMatchObject({
        ok: true,
        data: { experimentId: "01JNEWEXPERIMENT00000000001", status: "QUEUED" }
      });
      expect(api.requests).toHaveLength(1);
      expect(api.requests[0]?.init.method).toBe("POST");
      expect((api.requests[0]?.init.headers as Record<string, string>)["Idempotency-Key"]).toBe(
        "key"
      );
    }
  );
});
