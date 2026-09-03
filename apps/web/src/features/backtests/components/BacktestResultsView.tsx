"use client";
import { useMemo } from "react";
import { useClients } from "@/src/foundation/composition/client-provider";
import { parseBacktestLookup } from "../types/backtest-result";
import { useBacktestResult } from "../hooks/useBacktestResult";
import { ResultSummary } from "./ResultSummary";
import { ResultEvidence } from "./ResultEvidence";
import { TradeHistory } from "./TradeHistory";
export function BacktestResultsView({
  resultId,
  backtestId
}: {
  resultId?: string;
  backtestId?: string;
}) {
  const { api } = useClients();
  const lookup = useMemo(
    () => parseBacktestLookup({ resultId, backtestId }),
    [resultId, backtestId]
  );
  const { state, retry, canRetry } = useBacktestResult(api, lookup);
  if (state.status === "idle" || state.status === "loading" || state.status === "refreshing")
    return (
      <main className="feature-page" aria-busy="true">
        <p role="status">Loading backtest result…</p>
      </main>
    );
  if (state.status === "empty-identifier")
    return (
      <main className="feature-page">
        <h1>Backtest Results</h1>
        <section className="panel empty-state">
          <h2>Select a result</h2>
          <p>
            Choose an evaluated candidate from the Leaderboard or supply a valid backtest
            identifier.
          </p>
        </section>
      </main>
    );
  if (state.status !== "success")
    return (
      <main className="feature-page">
        <h1>Backtest Results</h1>
        <section className="panel error-state" role="alert">
          <h2>
            {state.status === "inaccessible"
              ? "Resource inaccessible"
              : state.status === "dependency-blocked"
                ? "Result lookup unavailable"
                : "Unable to load result"}
          </h2>
          <p>{state.error["message"]}</p>
          {state.error.retryable && (
            <button className="button" onClick={() => void retry()} disabled={!canRetry}>
              Retry{state.error.retryAfterSeconds ? ` in ${state.error.retryAfterSeconds}s` : ""}
            </button>
          )}
        </section>
      </main>
    );
  return (
    <main className="feature-page">
      <ResultSummary result={state.snapshot} />
      <ResultEvidence result={state.snapshot} />
      <TradeHistory trades={state.snapshot.trades} />
    </main>
  );
}
