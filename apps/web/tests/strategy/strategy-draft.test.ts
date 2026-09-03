import { describe, expect, it } from "vitest";
import type { StrategySourceDraft } from "@/src/features/strategy/model/strategy-draft";
describe("Strategy drafts", () => {
  it("discriminates SINGLE and COMPOSITE", () => {
    const single: StrategySourceDraft = {
      type: "SINGLE",
      strategy: { strategyId: "ma", version: "1", parameters: {} }
    };
    const composite: StrategySourceDraft = {
      type: "COMPOSITE",
      policyId: "weighted",
      policyVersion: "1",
      policyParameters: {},
      components: [single.strategy, single.strategy]
    };
    expect(single.type).toBe("SINGLE");
    expect(composite.components).toHaveLength(2);
  });
});
