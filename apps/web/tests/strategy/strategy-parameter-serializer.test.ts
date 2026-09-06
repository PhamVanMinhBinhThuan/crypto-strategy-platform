import { describe, expect, it } from "vitest";
import type { StrategyDescriptor } from "@/src/features/strategy/model/strategy";
import { serializeStrategyParameters } from "@/src/features/strategy/state/strategy-parameter-serializer";

const descriptor = {
  parameters: [
    { name: "period", type: "INTEGER", defaultValue: "20" },
    { name: "enabled", type: "BOOLEAN", defaultValue: "false" },
    { name: "threshold", type: "DECIMAL", defaultValue: "2.5" },
    { name: "mode", type: "ENUM", defaultValue: "MEAN_REVERSION" }
  ]
} as StrategyDescriptor;

describe("Strategy parameter serializer", () => {
  it("restores API canonical strings to their request contract types", () => {
    expect(
      serializeStrategyParameters(descriptor, {
        period: "21",
        enabled: "true",
        threshold: "3.25",
        mode: "BREAKOUT"
      })
    ).toEqual({
      period: 21,
      enabled: true,
      threshold: "3.25",
      mode: "BREAKOUT"
    });
  });
});
