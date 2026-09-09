import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StrategyParameters } from "@/src/features/strategy/components/StrategyParameters";
import type { StrategyDescriptor, UserStrategy } from "@/src/features/strategy/model/strategy";

const descriptor = {
  strategyId: "ma-crossover",
  strategyVersionId: "system-ma-v1",
  version: "1.0.0",
  contractVersion: "strategy-contract-v1",
  displayName: "Moving Average Crossover",
  description: "",
  category: "TREND",
  supportedSignals: ["BUY", "SELL", "HOLD"],
  requiredLookback: 25,
  parameters: [
    {
      name: "fastPeriod",
      type: "INTEGER",
      required: true,
      defaultValue: "5",
      minimum: "2",
      maximum: "100",
      allowedValues: [],
      description: "Number of candles used by the fast moving average."
    },
    {
      name: "slowPeriod",
      type: "INTEGER",
      required: true,
      defaultValue: "25",
      minimum: "3",
      maximum: "200",
      allowedValues: [],
      description: "Number of candles used by the slow moving average."
    }
  ],
  constraints: [],
  descriptorFingerprint: "descriptor-fingerprint"
} as StrategyDescriptor;

const owned = {
  userStrategyId: "owned-1",
  kind: "SINGLE",
  name: "My MA",
  description: "",
  status: "ACTIVE",
  archivedAt: null,
  createdAt: "2026-09-08T00:00:00Z",
  updatedAt: "2026-09-08T00:00:00Z",
  latestVersion: {
    userStrategyVersionId: "owned-version-1",
    userStrategyId: "owned-1",
    versionNo: 2,
    kind: "SINGLE",
    source: {
      type: "SINGLE",
      strategy: {
        strategyId: "ma-crossover",
        strategyVersionId: "system-ma-v1",
        version: "1.0.0",
        parameters: { fastPeriod: "8", slowPeriod: "34" }
      }
    },
    status: "PUBLISHED",
    fingerprint: "owned-fingerprint",
    publishedAt: "2026-09-08T00:00:00Z",
    createdAt: "2026-09-08T00:00:00Z"
  }
} as UserStrategy;

describe("Configured strategy parameters", () => {
  it("shows the values persisted in the latest personal strategy version", () => {
    render(<StrategyParameters owned={owned} systemStrategies={[descriptor]} />);

    const panel = screen.getByRole("region", { name: "Configured parameters" });
    expect(panel).toHaveTextContent("Moving Average Crossover");
    expect(panel).toHaveTextContent("fastPeriodINTEGER8");
    expect(panel).toHaveTextContent("slowPeriodINTEGER34");
    expect(panel).toHaveTextContent("Version 2");
  });
});
