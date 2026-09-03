import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CandleChart } from "@/src/features/market/components/CandleChart";
import { candlePageFixture } from "../fixtures/f012/public-contract";
describe("Candle chart", () => {
  it("provides accessible chart and exact OHLCV summary", () => {
    render(<CandleChart candles={candlePageFixture.items} label="BTC/USDT 1h" />);
    expect(screen.getByRole("img", { name: /BTC\/USDT 1h/ })).toBeInTheDocument();
    expect(screen.getByText(/O 100000.00/)).toBeInTheDocument();
  });
  it("renders an empty state", () => {
    render(<CandleChart candles={[]} label="empty" />);
    expect(screen.getByRole("status")).toHaveTextContent("Chưa có dữ liệu");
  });
});
