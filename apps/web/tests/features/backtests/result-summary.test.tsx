import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ResultSummary } from "@/src/features/backtests/components/ResultSummary";
import {
  ResultEvidence,
  ResultTechnicalDetails
} from "@/src/features/backtests/components/ResultEvidence";
import { mapBacktestResult } from "@/src/features/backtests/mappers/backtest-result-mapper";
import {
  normalBacktestResult,
  sentimentBacktestResult
} from "@/src/features/backtests/fixtures/backtest-result-fixtures";
describe("result summary", () => {
  it("renders exactly four production metrics plus capital and evidence", () => {
    const result = mapBacktestResult(normalBacktestResult);
    const { container } = render(
      <>
        <ResultSummary result={result} />
        <ResultEvidence result={result} />
        <ResultTechnicalDetails result={result} />
      </>
    );
    expect(
      within(screen.getByLabelText("Backtest performance")).getAllByRole("article")
    ).toHaveLength(4);
    for (const label of [
      "Total return",
      "Win rate",
      "Maximum drawdown",
      "Trades",
      "Initial capital",
      "Final capital",
      "Net profit",
      "Total fees",
      "Manifest fingerprint",
      "Transaction fee"
    ])
      expect(screen.getByText(label)).toBeInTheDocument();
    expect(container).not.toHaveTextContent(/Sharpe|Sortino|Profit Factor/);
  });
  it("presents score, thresholds, model and immutable sentiment evidence", () => {
    render(<ResultEvidence result={mapBacktestResult(sentimentBacktestResult)} />);
    for (const value of [
      "Sentiment evidence",
      "0.42",
      "SELL ≤ -0.25 · BUY ≥ 0.25",
      "multichannel-english@1.0.0",
      "whitespace-en-v1",
      "Sentiment is a research signal, not financial advice or a profit guarantee."
    ])
      expect(screen.getByText(value)).toBeInTheDocument();
  });
});
