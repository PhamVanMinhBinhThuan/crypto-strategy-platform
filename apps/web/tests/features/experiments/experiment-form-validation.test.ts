import { describe, expect, it } from "vitest";
import {
  initialExperimentDraft,
  validateExperimentDraft
} from "@/src/features/experiments/types/experiment-configuration";
describe("Experiment configuration validation", () => {
  it("requires identity/discovery fields, integer seed and a positive finite stop bound", () => {
    const errors = validateExperimentDraft({
      ...initialExperimentDraft,
      name: "",
      datasetId: "",
      generatorId: "",
      seed: "1.5",
      strategyId: "",
      maximumCandidates: "0",
      maximumDurationSeconds: "NaN"
    });
    expect(errors).toMatchObject({
      name: expect.any(String),
      datasetId: expect.any(String),
      generatorId: expect.any(String),
      seed: expect.any(String),
      strategyId: expect.any(String),
      stop: expect.any(String)
    });
  });
  it.each([
    [0, false],
    [1, true],
    [10, true],
    [25, true],
    [50, true],
    [100, true],
    [101, false]
  ] as const)("validates Top-K %s", (topK, valid) => {
    const errors = validateExperimentDraft({
      ...initialExperimentDraft,
      name: "x",
      datasetId: "dataset",
      topK
    });
    expect(!errors.topK).toBe(valid);
  });
  it("validates finite ordered parameter ranges", () => {
    const errors = validateExperimentDraft({
      ...initialExperimentDraft,
      name: "x",
      datasetId: "dataset",
      parameters: { fastPeriod: { minimum: "50", maximum: "2" } }
    });
    expect(errors["parameter-fastPeriod"]).toBeDefined();
  });
});
