import { describe, expect, it } from "vitest";
import {
  candidatePage,
  completionNotification,
  experimentFixtureErrors,
  experimentStates,
  jobStates
} from "@/src/features/experiments/fixtures/experiment-job-fixtures";
describe("finite Experiment fixtures", () => {
  it("covers all lifecycle, Job, Candidate and completion snapshots", () => {
    expect(experimentStates).toHaveLength(7);
    expect(jobStates).toHaveLength(7);
    expect(candidatePage.items[0]?.candidateId).toBe("candidate-013");
    expect(completionNotification.eventType).toBe("BACKTEST_COMPLETED");
  });
  it("covers 401, 404, 429 and F-010 503", () =>
    expect(Object.keys(experimentFixtureErrors)).toEqual([
      "authentication",
      "inaccessible",
      "rateLimited",
      "dependency"
    ]));
  it("contains no timers, random generation or worker simulation", async () => {
    const source = await import("node:fs").then((fs) =>
      fs.readFileSync("src/features/experiments/fixtures/experiment-job-fixtures.ts", "utf8")
    );
    expect(source).not.toMatch(/setTimeout|setInterval|Math\.random|Worker/);
  });
});
