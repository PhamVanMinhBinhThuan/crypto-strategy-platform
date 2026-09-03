import { describe, expect, it } from "vitest";
import {
  mapStrategyDescriptors,
  validateDescriptorParameters
} from "@/src/features/experiments/mappers/strategy-descriptor-mapper";
import { strategyDescriptorPage } from "@/src/features/experiments/fixtures/experiment-configuration-fixtures";
describe("Strategy descriptor mapping", () => {
  it("preserves released identity/version/schema/range/options", () => {
    const page = mapStrategyDescriptors(strategyDescriptorPage);
    expect(page.items[0]).toMatchObject({
      strategyId: "ma-crossover",
      version: "1.0.0",
      parameters: [
        { name: "fastPeriod", minimum: "2", maximum: "50" },
        { name: "slowPeriod" },
        { name: "priceSource", allowedValues: ["OPEN", "CLOSE"] }
      ]
    });
    expect(page).not.toHaveProperty("datasets");
    expect(page).not.toHaveProperty("generators");
  });
  it("validates descriptor ranges, options and cross constraints", () => {
    const strategy = mapStrategyDescriptors(strategyDescriptorPage).items[0]!;
    expect(
      validateDescriptorParameters(
        { fastPeriod: "70", slowPeriod: "40", priceSource: "HIGH" },
        strategy
      )
    ).toEqual(
      expect.objectContaining({ fastPeriod: expect.any(String), priceSource: expect.any(String) })
    );
    expect(
      validateDescriptorParameters(
        { fastPeriod: "12", slowPeriod: "64", priceSource: "CLOSE" },
        strategy
      )
    ).toEqual({});
  });
});
