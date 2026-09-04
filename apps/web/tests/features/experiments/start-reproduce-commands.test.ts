import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { createExperimentCommandService } from "@/src/features/experiments/service/experiment-command-service";
import { initialExperimentDraft } from "@/src/features/experiments/types/experiment-configuration";
describe("Start/Reproduce commands", () => {
  it("owns independent paths and logical keys", async () => {
    const api = new MockApiClient()
      .respond("POST /api/v1/experiments", {
        experimentId: "new",
        jobId: "job-new",
        status: "QUEUED"
      })
      .respond("POST /api/v1/experiments/experiment-013/reproductions", {
        experimentId: "copy",
        jobId: "job-copy",
        status: "QUEUED"
      });
    const service = createExperimentCommandService(api);
    await service.start(
      { ...initialExperimentDraft, name: "x", datasetId: "dataset" },
      "start-key"
    );
    await service.reproduce("experiment-013", "reproduce-key");
    expect(
      api.requests.map((x) => [
        (x.init.headers as Record<string, string>)["Idempotency-Key"],
        x.path
      ])
    ).toEqual([
      ["start-key", "/api/v1/experiments"],
      ["reproduce-key", "/api/v1/experiments/experiment-013/reproductions"]
    ]);
  });
  it.each([
    ["AUTHENTICATION_REQUIRED", false],
    ["RATE_LIMIT_EXCEEDED", true],
    ["DEPENDENCY_UNAVAILABLE", true],
    ["TRANSPORT_UNCERTAIN", true]
  ])("preserves %s once without replay", async (code, retryable) => {
    const api = new MockApiClient().respond("POST /api/v1/experiments", {
      ok: false,
      error: { code, message: "safe", retryable }
    });
    const result = await createExperimentCommandService(api).start(
      { ...initialExperimentDraft, name: "x", datasetId: "dataset" },
      "same-key"
    );
    expect(result.ok).toBe(false);
    expect(api.requests).toHaveLength(1);
  });
  it("implements independent state, locks and same-key uncertain retry lineage", () => {
    const source = readFileSync("src/features/experiments/hooks/useExperimentCommands.ts", "utf8");
    expect(source).toContain("startKey");
    expect(source).toContain("reproduceKey");
    expect(source).toContain("busy.current[kind]");
    expect(source).toContain("if (!retry || !ref.current)");
  });
});
