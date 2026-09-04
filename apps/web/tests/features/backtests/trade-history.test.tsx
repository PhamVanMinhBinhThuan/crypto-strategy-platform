import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TradeHistory } from "@/src/features/backtests/components/TradeHistory";
import { mapBacktestResult } from "@/src/features/backtests/mappers/backtest-result-mapper";
import {
  extremeDecimalBacktestResult,
  manyTradeBacktestResult,
  zeroTradeBacktestResult
} from "@/src/features/backtests/fixtures/backtest-result-fixtures";
describe("trade history", () => {
  it("renders every released column in authoritative order with local scrolling", () => {
    render(<TradeHistory trades={mapBacktestResult(manyTradeBacktestResult).trades} />);
    const region = screen.getByRole("region", { name: /trade history/i });
    expect(region).toHaveClass("table-scroll");
    expect(within(region).getAllByRole("row")).toHaveLength(7);
    expect(screen.getByText(/authoritative execution evidence/i)).toBeInTheDocument();
    expect(
      within(region)
        .getAllByRole("columnheader")
        .map((x) => x.textContent)
    ).toEqual([
      "#",
      "Side",
      "Entry time",
      "Entry price",
      "Exit time",
      "Exit price",
      "Quantity",
      "Entry fee",
      "Exit fee",
      "Total fee",
      "Realized P/L",
      "Post-trade cash",
      "Exit reason"
    ]);
  });
  it("announces a valid zero-trade outcome", () => {
    render(<TradeHistory trades={mapBacktestResult(zeroTradeBacktestResult).trades} />);
    expect(screen.getByText(/generated no signals/i)).toBeInTheDocument();
  });
  it("discloses full decimal values", () => {
    render(<TradeHistory trades={mapBacktestResult(extremeDecimalBacktestResult).trades} />);
    expect(screen.getByTitle("65000.123456789")).toHaveTextContent("65000.123456789");
  });
  it("rejects contradictory trade count and execution ordering", () => {
    expect(() =>
      mapBacktestResult({
        ...manyTradeBacktestResult,
        metrics: { ...manyTradeBacktestResult.metrics, numberOfTrades: 5 }
      })
    ).toThrow();
    expect(() =>
      mapBacktestResult({
        ...zeroTradeBacktestResult,
        metrics: { ...zeroTradeBacktestResult.metrics, numberOfTrades: 1 },
        trades: [
          {
            ...manyTradeBacktestResult.trades[0],
            entryTime: "2026-08-03T00:00:00Z",
            exitTime: "2026-08-02T00:00:00Z"
          }
        ]
      })
    ).toThrow();
  });
});
