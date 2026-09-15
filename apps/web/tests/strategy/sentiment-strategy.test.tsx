import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { StrategyCatalog } from "@/src/features/strategy/components/StrategyCatalog";
import { StrategyDetail } from "@/src/features/strategy/components/StrategyDetail";
import { StrategyParameters } from "@/src/features/strategy/components/StrategyParameters";
import type { StrategyDescriptor } from "@/src/features/strategy/model/strategy";
import {
  searchDomains,
  supportedForSearch
} from "@/src/features/experiments/components/ExperimentConfigurationForm";

const sentiment = {
  strategyId: "sentiment-polarity",
  strategyVersionId: "sentiment-polarity-v1",
  version: "1.0.0",
  contractVersion: "strategy-contract-v1",
  displayName: "Sentiment Polarity",
  description: "Aggregates frozen News sentiment into a research signal.",
  category: "SENTIMENT",
  supportedSignals: ["BUY", "SELL", "HOLD"],
  requiredLookback: 1,
  parameters: [
    {
      name: "lookbackHours",
      type: "INTEGER",
      required: true,
      defaultValue: "24",
      minimum: "1",
      maximum: "168",
      allowedValues: [],
      description: "News window ending at the evaluation candle.",
      searchRangeHint: { minimum: "6", maximum: "48", step: "6" }
    },
    {
      name: "minimumArticles",
      type: "INTEGER",
      required: true,
      defaultValue: "2",
      minimum: "1",
      maximum: "100",
      allowedValues: [],
      description: "Minimum eligible analyzed articles.",
      searchRangeHint: { minimum: "1", maximum: "5", step: "1" }
    },
    {
      name: "buyThreshold",
      type: "DECIMAL",
      required: true,
      defaultValue: "0.25",
      minimum: "-1",
      maximum: "1",
      allowedValues: [],
      description: "Score at or above this value emits BUY.",
      searchRangeHint: { minimum: "0.1", maximum: "0.5", step: "0.1" }
    },
    {
      name: "sellThreshold",
      type: "DECIMAL",
      required: true,
      defaultValue: "-0.25",
      minimum: "-1",
      maximum: "1",
      allowedValues: [],
      description: "Score at or below this value emits SELL.",
      searchRangeHint: { minimum: "-0.5", maximum: "-0.1", step: "0.1" }
    }
  ],
  constraints: [{ lowerParameter: "sellThreshold", upperParameter: "buyThreshold" }],
  descriptorFingerprint: "strategy-descriptor-v1:sentiment-polarity:1.0.0"
} as StrategyDescriptor;

describe("Sentiment system strategy", () => {
  it("renders as the fifth catalog plugin with its released identity", () => {
    const technical = Array.from({ length: 4 }, (_, index) => ({
      ...sentiment,
      strategyId: `technical-${index}`,
      strategyVersionId: `technical-${index}-v1`,
      displayName: `Technical ${index}`,
      category: "TECHNICAL"
    }));
    render(
      <StrategyCatalog
        system={[...technical, sentiment]}
        owned={[]}
        loadingSystem={false}
        loadingOwned={false}
        onSelectSystem={vi.fn()}
        onSelectOwned={vi.fn()}
      />
    );

    expect(screen.getByText("5")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Sentiment Polarity/ })).toHaveTextContent(
      "SENTIMENT · v1.0.0"
    );
  });

  it("presents the four parameters and the research-only disclaimer", () => {
    render(
      <>
        <StrategyDetail descriptor={sentiment} />
        <StrategyParameters descriptor={sentiment} systemStrategies={[sentiment]} />
      </>
    );

    expect(
      screen.getByRole("complementary", { name: "Sentiment strategy notice" })
    ).toHaveTextContent("not financial advice");
    const parameters = screen.getByRole("region", { name: "Parameter definitions" });
    for (const name of ["lookbackHours", "minimumArticles", "buyThreshold", "sellThreshold"])
      expect(parameters).toHaveTextContent(name);
  });

  it("creates finite domains that work for both single and composite Search pools", () => {
    expect(supportedForSearch(sentiment)).toBe(true);
    expect(searchDomains(sentiment)).toEqual({
      lookbackHours: {
        kind: "RANGE",
        valueType: "INTEGER",
        minimum: "6",
        maximum: "48",
        step: "6"
      },
      minimumArticles: {
        kind: "RANGE",
        valueType: "INTEGER",
        minimum: "1",
        maximum: "5",
        step: "1"
      },
      buyThreshold: {
        kind: "RANGE",
        valueType: "DECIMAL",
        minimum: "0.1",
        maximum: "0.5",
        step: "0.1"
      },
      sellThreshold: {
        kind: "RANGE",
        valueType: "DECIMAL",
        minimum: "-0.5",
        maximum: "-0.1",
        step: "0.1"
      }
    });
  });
});
