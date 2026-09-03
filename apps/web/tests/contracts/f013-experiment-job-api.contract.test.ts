import { describe, expect, it } from "vitest";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { createExperimentService } from "@/src/features/experiments/service/experiment-service";
import {
  candidatePage,
  runningExperiment,
  runningJob
} from "@/src/features/experiments/fixtures/experiment-job-fixtures";
describe("F-009 Experiment/Job/Candidate adapter", () => {
  it("uses every released owned REST path and opaque cursor", async () => {
    const api = new MockApiClient()
      .respond("/api/v1/experiments/experiment-013", runningExperiment)
      .respond("/api/v1/jobs/job-search-013", runningJob)
      .respond(
        "/api/v1/experiments/experiment-013/candidates?limit=50&cursor=opaque%2Bcursor",
        candidatePage
      )
      .respond(
        "/api/v1/experiments/experiment-013/candidates/candidate-013",
        candidatePage.items[0]
      );
    const service = createExperimentService(api);
    await service.readExperiment("experiment-013");
    await service.readJob("job-search-013");
    await service.readCandidates("experiment-013", "opaque+cursor");
    await service.readCandidate("experiment-013", "candidate-013");
    expect(api.requests.map((x) => x.path)).toEqual([
      "/api/v1/experiments/experiment-013",
      "/api/v1/jobs/job-search-013",
      "/api/v1/experiments/experiment-013/candidates?limit=50&cursor=opaque%2Bcursor",
      "/api/v1/experiments/experiment-013/candidates/candidate-013"
    ]);
  });
  it.each(["EXPERIMENT_NOT_FOUND", "JOB_NOT_FOUND", "FORBIDDEN"])(
    "collapses %s safely",
    async (code) => {
      const api = new MockApiClient().respond("/api/v1/experiments/experiment-013", {
        ok: false,
        error: { code, message: "private", retryable: false }
      });
      await expect(createExperimentService(api).readExperiment("experiment-013")).resolves.toEqual({
        ok: false,
        error: { code: "RESOURCE_NOT_FOUND", message: "Resource inaccessible", retryable: false }
      });
    }
  );
  it.each([
    ["AUTHENTICATION_REQUIRED", false],
    ["RATE_LIMIT_EXCEEDED", true],
    ["DEPENDENCY_UNAVAILABLE", true]
  ])("preserves safe %s", async (code, retryable) => {
    const api = new MockApiClient().respond("/api/v1/jobs/job-search-013", {
      ok: false,
      error: { code, message: "safe", retryable }
    });
    expect(await createExperimentService(api).readJob("job-search-013")).toEqual({
      ok: false,
      error: { code, message: "safe", retryable }
    });
  });
});
