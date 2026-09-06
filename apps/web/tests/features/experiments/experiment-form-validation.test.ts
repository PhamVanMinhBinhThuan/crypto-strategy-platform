import { describe, expect, it } from "vitest";
import {
  draftPayload,
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
      strategyPool: expect.any(String),
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
      strategyPool: [
        {
          key: "system:ma-crossover:1.0.0",
          displayName: "Moving Average Crossover",
          strategyId: "ma-crossover",
          strategyVersion: "1.0.0",
          parameters: { fastPeriod: { kind: "RANGE", minimum: "50", maximum: "2" } }
        }
      ]
    });
    expect(errors["parameter-system:ma-crossover:1.0.0-fastPeriod"]).toBeDefined();
  });

  it("preserves an exact decimal range discriminator and step in the v2 payload", () => {
    const draft = {
      ...initialExperimentDraft,
      name: "decimal search",
      datasetId: "dataset",
      strategyPool: [
        {
          key: "system:rsi:1.0.0",
          displayName: "RSI",
          strategyId: "rsi",
          strategyVersion: "1.0.0",
          parameters: {
            buyThreshold: {
              kind: "RANGE" as const,
              valueType: "DECIMAL" as const,
              minimum: "20.5",
              maximum: "30.5",
              step: "0.5"
            }
          }
        }
      ]
    };

    expect(validateExperimentDraft(draft)).not.toHaveProperty(
      "parameter-system:rsi:1.0.0-buyThreshold"
    );
    expect(draftPayload(draft).searchSpace.strategyPool[0].parameterDomains).toEqual({
      buyThreshold: { kind: "DECIMAL_RANGE", min: 20.5, max: 30.5, step: 0.5 }
    });
  });

  it("validates simulated capital and converts fee/slippage percentages to exact rates", () => {
    const invalid = validateExperimentDraft({
      ...initialExperimentDraft,
      initialCapital: "0",
      feePercent: "100",
      slippagePercent: "-0.1"
    });
    expect(invalid).toMatchObject({
      initialCapital: expect.any(String),
      feePercent: expect.any(String),
      slippagePercent: expect.any(String)
    });

    const payload = draftPayload({
      ...initialExperimentDraft,
      initialCapital: "25000.50",
      feePercent: "0.1",
      slippagePercent: "0.05"
    });
    expect(payload.backtestConfiguration).toEqual({
      initialCapital: "25000.50",
      feeRate: "0.001",
      slippageRate: "0.0005"
    });
  });
});
