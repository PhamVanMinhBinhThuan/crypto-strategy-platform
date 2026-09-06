"use client";
import { useMemo } from "react";
import { useClients } from "@/src/foundation/composition/client-provider";
import { parseBacktestLookup } from "../types/backtest-result";
import { useBacktestResult } from "../hooks/useBacktestResult";
import { ResultSummary } from "./ResultSummary";
import { ResultEvidence, ResultTechnicalDetails } from "./ResultEvidence";
import { TradeHistory } from "./TradeHistory";
import { quoteCurrency } from "./backtest-presentation";
import Link from "next/link";
import { RecentBacktestResults } from "./RecentBacktestResults";
import { safeExperimentReturnUrl } from "@/src/foundation/navigation/resource-history";
export function BacktestResultsView({
  resultId,
  backtestId,
  returnTo
}: {
  resultId?: string;
  backtestId?: string;
  returnTo?: string;
}) {
  const { api } = useClients();
  const lookup = useMemo(
    () => parseBacktestLookup({ resultId, backtestId }),
    [resultId, backtestId]
  );
  const { state, retry, canRetry } = useBacktestResult(api, lookup);
  if (lookup.kind === "none")
    return (
      <main className="feature-page">
        <header className="feature-header"><div><p className="eyebrow">Backtest evidence</p><h1>Backtest Results</h1><p className="muted">Open a completed result and inspect its immutable evidence.</p></div></header>
        <RecentBacktestResults api={api} />
      </main>
    );
  if (state.status === "idle" || state.status === "loading" || state.status === "refreshing" || state.status === "empty-identifier")
    return (
      <main className="feature-page" aria-busy="true">
        <p role="status">Loading backtest result…</p>
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
          <p><Link className="button secondary" href="/backtests">All backtest results</Link></p>
        </section>
      </main>
    );
  return (
    <main className="feature-page backtest-result-page">
      <nav className="page-actions backtest-navigation" aria-label="Backtest result navigation">
        <Link className="backtest-return-link" href={safeExperimentReturnUrl(
          returnTo,
          state.snapshot.provenance.experimentId,
          state.snapshot.provenance.candidateId
        )}>&larr; Back to experiment</Link>
        <Link className="button secondary" href="/backtests">All backtest results</Link>
      </nav>
      <ResultSummary result={state.snapshot} />
      <ResultEvidence result={state.snapshot} />
      <TradeHistory
        key={state.snapshot.backtestResultId}
        trades={state.snapshot.trades}
        quoteCurrency={quoteCurrency(state.snapshot.provenance.dataset?.tradingPair)}
      />
      <ResultTechnicalDetails result={state.snapshot} />
    </main>
  );
}
