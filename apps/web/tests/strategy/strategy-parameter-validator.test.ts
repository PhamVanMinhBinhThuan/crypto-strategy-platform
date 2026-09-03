import { describe, expect, it } from "vitest";
import { validateStrategyParameters } from "@/src/features/strategy/state/strategy-parameter-validator";
import type { StrategyDescriptor } from "@/src/features/strategy/model/strategy";
const descriptor = {
  strategyId: "ma",
  strategyVersionId: "v1",
  version: "1",
  contractVersion: "1",
  displayName: "MA",
  description: "",
  category: "TREND",
  supportedSignals: ["BUY"],
  requiredLookback: 2,
  descriptorFingerprint: "fp",
  constraints: [],
  parameters: [
    {
      name: "period",
      type: "INTEGER",
      required: true,
      defaultValue: "5",
      minimum: "2",
      maximum: "100",
      allowedValues: [],
      description: ""
    },
    {
      name: "threshold",
      type: "DECIMAL",
      required: true,
      defaultValue: "0.1",
      minimum: "0.000000000001",
      maximum: "1",
      allowedValues: [],
      description: ""
    },
    {
      name: "enabled",
      type: "BOOLEAN",
      required: true,
      defaultValue: "true",
      minimum: null,
      maximum: null,
      allowedValues: [],
      description: ""
    },
    {
      name: "mode",
      type: "ENUM",
      required: true,
      defaultValue: "fast",
      minimum: null,
      maximum: null,
      allowedValues: ["fast", "slow"],
      description: ""
    }
  ]
} as StrategyDescriptor;
describe("Strategy parameter validator", () => {
  it("supports all typed constraints without decimal rounding", () => {
    expect(
      validateStrategyParameters(descriptor, {
        period: "2.5",
        threshold: "0.0000000000009",
        enabled: "yes",
        mode: "other"
      })
    ).toEqual({
      period: "Phải là số nguyên.",
      threshold: "Tối thiểu 0.000000000001.",
      enabled: "Phải là true hoặc false.",
      mode: "Giá trị không được hỗ trợ."
    });
  });
  it("accepts valid exact values", () =>
    expect(
      validateStrategyParameters(descriptor, {
        period: "20",
        threshold: "0.100000000001",
        enabled: "true",
        mode: "fast"
      })
    ).toEqual({}));
});
