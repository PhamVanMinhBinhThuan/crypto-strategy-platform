import { describe, expect, it } from "vitest";
import { validateStrategyParameters } from "@/src/features/strategy/state/strategy-parameter-validator";
import type { StrategyDescriptor } from "@/src/features/strategy/model/strategy";
describe("Strategy cross rules", () => {
  it("requires lower to be less than upper", () => {
    const descriptor = {
      parameters: [
        {
          name: "fast",
          type: "INTEGER",
          required: true,
          minimum: null,
          maximum: null,
          defaultValue: null,
          allowedValues: [],
          description: ""
        },
        {
          name: "slow",
          type: "INTEGER",
          required: true,
          minimum: null,
          maximum: null,
          defaultValue: null,
          allowedValues: [],
          description: ""
        }
      ],
      constraints: [{ lowerParameter: "fast", upperParameter: "slow" }]
    } as unknown as StrategyDescriptor;
    expect(validateStrategyParameters(descriptor, { fast: "20", slow: "10" }).slow).toMatch(/fast/);
  });
});
