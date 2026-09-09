import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  BacktestStrategyChart,
  buildBacktestChartModel
} from "@/src/features/backtests/components/BacktestStrategyChart";
import { normalBacktestResult } from "@/src/features/backtests/fixtures/backtest-result-fixtures";
import { mapBacktestResult } from "@/src/features/backtests/mappers/backtest-result-mapper";
import type { Candle } from "@/src/features/market/model/candle";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";

const candleTime = (hour: number, end = false) =>
  new Date(Date.parse("2026-08-01T00:00:00Z") + hour * 3_600_000 + (end ? 3_599_000 : 0))
    .toISOString()
    .replace(".000Z", "Z");

const candle = (hour: number, close = 100 + hour): Candle => ({
  pair: "BTC/USDT",
  timeframe: "1h",
  openTime: candleTime(hour),
  closeTime: candleTime(hour, true),
  open: String(close - 1),
  high: String(close + 2),
  low: String(close - 2),
  close: String(close),
  volume: "10",
  closed: true
});

describe("Backtest strategy chart", () => {
  it("derives MA, Bollinger, support/resistance and RSI overlays from parameters", () => {
    const candles = Array.from({ length: 24 }, (_, index) => candle(index));
    const model = buildBacktestChartModel(candles, [
      { strategyId: "ma-crossover", parameters: { fastPeriod: 3, slowPeriod: 7 } },
      { strategyId: "bollinger-bands", parameters: { period: 5, standardDeviation: 2 } },
      { strategyId: "support-resistance", parameters: { lookback: 4 } },
      {
        strategyId: "rsi-threshold",
        parameters: { period: 6, buyThreshold: 25, sellThreshold: 75 }
      }
    ]);

    expect(model.priceSeries.map((item) => item.label)).toEqual(
      expect.arrayContaining([
        "MA Crossover · MA 3",
        "Bollinger Bands · Bollinger upper",
        "Support Resistance · Support zone"
      ])
    );
    expect(model.rsiSeries[0]?.label).toBe("RSI Threshold · RSI 6");
    expect(model.rsiThresholds.map((item) => item.value)).toEqual([25, 75]);
  });

  it("loads real candle data and presents executed BUY/SELL markers", async () => {
    const start = "2026-08-01T00:00:00Z";
    const end = "2026-08-02T00:00:00Z";
    const candles = Array.from({ length: 80 }, (_, index) => candle(index, 65_000 + index * 10));
    const path =
      "/api/v1/candles?pair=BTC%2FUSDT&timeframe=1h&startTime=" +
      encodeURIComponent(start) +
      "&endTime=" +
      encodeURIComponent(end) +
      "&limit=200";
    const api = new MockApiClient().respond(path, {
      items: candles,
      nextCursor: null,
      hasMore: false
    });
    const result = mapBacktestResult({
      ...normalBacktestResult,
      metrics: { ...normalBacktestResult.metrics, numberOfTrades: 1 },
      trades: [
        {
          ...normalBacktestResult.trades[0],
          entryTime: candleTime(62),
          entryPrice: "65620",
          exitTime: candleTime(78),
          exitPrice: "65780"
        }
      ],
      provenance: {
        ...normalBacktestResult.provenance,
        dataset: {
          datasetVersionId: "dataset-chart",
          version: "candle-v1",
          checksum: "sha256:dataset-chart",
          provider: "BINANCE",
          tradingPair: "BTC/USDT",
          timeframe: "1h",
          normalizationVersion: "binance-v1",
          rangeStart: start,
          rangeEnd: end,
          candleCount: candles.length
        },
        candidate: {
          candidateId: "candidate-chart",
          generationIndex: 0,
          definition: { fastPeriod: 3, slowPeriod: 7 },
          fingerprint: "sha256:candidate-chart",
          createdAt: start
        },
        strategy: {
          kind: "SINGLE",
          singleStrategy: {
            strategyVersionId: "strategy-chart",
            pluginId: "moving-average-crossover",
            implementationVersion: "1.0.0"
          },
          parameters: {
            fastPeriod: { type: "INTEGER", value: "5" },
            slowPeriod: { type: "INTEGER", value: "20" }
          },
          compositePolicyId: null,
          compositePolicyVersion: null,
          components: [],
          sourceUserStrategyVersionId: null,
          fingerprint: "sha256:strategy-chart"
        }
      }
    });

    const { container } = render(<BacktestStrategyChart api={api} result={result} />);

    await waitFor(() =>
      expect(screen.getByRole("img", { name: "BTC/USDT backtest execution" })).toBeInTheDocument()
    );
    expect(screen.getByText("BUY · executed entry")).toBeInTheDocument();
    expect(screen.getByText("SELL · executed exit")).toBeInTheDocument();
    expect(screen.getByText("MA 3")).toBeInTheDocument();
    expect(container.querySelectorAll(".marker-buy")).toHaveLength(1);
    expect(container.querySelectorAll(".marker-sell")).toHaveLength(1);
    expect(api.requests.map((request) => request.path)).toContain(path);

    expect(screen.getByText("Nến 21–80 / 80")).toBeInTheDocument();
    expect(screen.getByText("C 65,790")).toBeInTheDocument();
    const firstCandleHitbox = container.querySelector(".backtest-chart-hitbox");
    expect(firstCandleHitbox).not.toBeNull();
    fireEvent.mouseEnter(firstCandleHitbox!);
    expect(screen.getByText("C 65,200")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "← Cũ hơn" }));
    expect(screen.getByText("Nến 1–20 / 80")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "← Cũ hơn" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Mới hơn →" })).toBeEnabled();
  });
});
