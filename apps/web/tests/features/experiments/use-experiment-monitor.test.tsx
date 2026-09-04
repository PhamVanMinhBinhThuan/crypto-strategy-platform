import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { createExperimentService } from "@/src/features/experiments/service/experiment-service";
import {
  runningExperiment,
  runningJob
} from "@/src/features/experiments/fixtures/experiment-job-fixtures";
describe("Experiment monitor ownership", () => {
  it("reads Experiment and Job into separate authoritative results", async () => {
    const api = new MockApiClient()
      .respond("/api/v1/experiments/experiment-013", runningExperiment)
      .respond("/api/v1/jobs/job-search-013", runningJob);
    const service = createExperimentService(api);
    const [experiment, job] = await Promise.all([
      service.readExperiment("experiment-013"),
      service.readJob("job-search-013")
    ]);
    expect(experiment.ok && experiment.data.status).toBe("RUNNING");
    expect(job.ok && job.data.completedWork).toBe(42);
  });
  it("preserves snapshots while refresh errors are represented separately", () => {
    const source = readFileSync("src/features/experiments/hooks/useExperimentMonitor.ts", "utf8");
    expect(source).toContain("setExperiment(exp.data)");
    expect(source).not.toMatch(/setExperiment\(undefined\)|setJobs\(\[\]\).*error/);
  });
  it("uses realtime payloads only as refresh hints", () => {
    const source = readFileSync("src/features/experiments/hooks/useExperimentRealtime.ts", "utf8");
    expect(source).toContain("onExperimentRefresh()");
    expect(source).not.toMatch(/setExperiment|setJobs|completedWork\s*:/);
  });
});
