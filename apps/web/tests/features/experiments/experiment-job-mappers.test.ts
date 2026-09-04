import { describe, expect, it } from "vitest";
import {
  candidatePage,
  experimentStates,
  jobStates,
  runningExperiment,
  runningJob
} from "@/src/features/experiments/fixtures/experiment-job-fixtures";
import {
  mapCandidatePage,
  mapExperiment,
  mapJob
} from "@/src/features/experiments/mappers/experiment-job-mappers";
describe("Experiment/Job/Candidate mappers", () => {
  it("accepts every released Experiment lifecycle and safe failure", () => {
    expect(experimentStates.map(mapExperiment).map((x) => x.status)).toEqual([
      "CREATED",
      "QUEUED",
      "RUNNING",
      "STOP_REQUESTED",
      "STOPPED",
      "COMPLETED",
      "FAILED"
    ]);
    expect(mapExperiment(experimentStates[6]).failure?.code).toBe("JOB_EXECUTION_TIMEOUT");
  });
  it("keeps Jobs separate with exact counts, scores, retry UTC and states", () => {
    expect(jobStates.map(mapJob).map((x) => x.status)).toEqual([
      "QUEUED",
      "RUNNING",
      "RETRY_SCHEDULED",
      "SUCCEEDED",
      "FAILED",
      "CANCEL_REQUESTED",
      "CANCELLED"
    ]);
    expect(mapJob(runningJob).bestScore).toBe("0.873400000000000001");
    expect(mapJob(runningJob).completedWork).toBe(42);
    expect(mapJob(jobStates[2]).nextRetryAt).toMatch(/Z$/);
  });
  it("preserves Candidate metadata and opaque cursor fields", () => {
    const value = mapCandidatePage(candidatePage);
    expect(value.items[0]).toMatchObject({
      generationIndex: 42,
      fingerprint: "sha256:candidate013",
      definition: { strategyId: "ma-crossover" }
    });
    expect(value).toMatchObject({ nextCursor: null, hasMore: false });
  });
  it("rejects invalid UTC and counts", () => {
    expect(() => mapExperiment({ ...runningExperiment, createdAt: "local" })).toThrow();
    expect(() => mapJob({ ...runningJob, totalWork: 0 })).toThrow();
  });
});
