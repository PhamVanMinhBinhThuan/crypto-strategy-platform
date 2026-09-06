import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TradeHistory } from "@/src/features/backtests/components/TradeHistory";
import { mapBacktestResult } from "@/src/features/backtests/mappers/backtest-result-mapper";
import {
  extremeDecimalBacktestResult,
  manyTradeBacktestResult,
  zeroTradeBacktestResult
} from "@/src/features/backtests/fixtures/backtest-result-fixtures";
describe("trade history", () => {
  it("renders the user-facing columns in authoritative order with local scrolling", () => {
    render(<TradeHistory trades={mapBacktestResult(manyTradeBacktestResult).trades} />);
    const region = screen.getByRole("region", { name: "Scrollable trade history" });
    expect(region).toHaveClass("table-scroll");
    expect(within(region).getAllByRole("row")).toHaveLength(7);
    expect(screen.getByText(/recorded entry, exit, fees/i)).toBeInTheDocument();
    expect(
      within(region)
        .getAllByRole("columnheader")
        .map((x) => x.textContent)
    ).toEqual([
      "#",
      "Side",
      "Entry",
      "Exit",
      "Quantity",
      "Fees",
      "Realized P/L",
      "Cash after trade",
      "Exit reason",
      "Actions"
    ]);
  });
  it("announces a valid zero-trade outcome", () => {
    render(<TradeHistory trades={mapBacktestResult(zeroTradeBacktestResult).trades} />);
    expect(screen.getByText(/generated no signals/i)).toBeInTheDocument();
  });
  it("discloses full decimal values", () => {
    render(<TradeHistory trades={mapBacktestResult(extremeDecimalBacktestResult).trades} />);
    expect(screen.getByTitle("65000.123456789")).toHaveTextContent("65,000.12345679");
    fireEvent.click(screen.getAllByRole("button", { name: /View details for trade/ })[0]);
    expect(screen.getByText("65000.123456789")).toBeInTheDocument();
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
