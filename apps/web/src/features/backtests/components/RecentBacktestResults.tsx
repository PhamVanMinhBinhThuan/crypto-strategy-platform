"use client";

import Link from "next/link";
import { useCallback } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { useCursorPagination } from "@/src/foundation/ui/useCursorPagination";
import { CandidatePagination, CandidateTableShell } from "@/src/features/experiments/components/CandidateTableParts";
import { formatDateTime, formatPercent, formatScore, strategyName } from "@/src/features/experiments/components/candidate-presentation";
import { createBacktestResultService } from "../service/backtest-result-service";
import type { BacktestResultHistoryItem } from "../types/backtest-result";

export function RecentBacktestResults({ api }: { api: ApiClient }) {
  const loadPage = useCallback(
    (cursor?: string) => createBacktestResultService(api).readRecent(cursor),
    [api]
  );
  const state = useCursorPagination<BacktestResultHistoryItem>(loadPage);
  const items = state.page?.items ?? [];
  const total = state.page?.totalCount ?? 0;
  const start = items.length ? (state.pageNumber - 1) * 10 + 1 : 0;
  const end = items.length ? start + items.length - 1 : 0;

  return (
    <section className="panel history-panel" aria-labelledby="recent-backtests-heading">
      <div className="history-heading">
        <div><p className="eyebrow">Result history</p><h2 id="recent-backtests-heading">Recent backtest results</h2></div>
        <Link className="button secondary" href="/search">Find an experiment</Link>
      </div>
      {state.loading && !state.page && <p role="status">Loading recent backtest results…</p>}
      {state.error && <div className="history-error" role="alert"><p>{state.error}</p><button className="button secondary" type="button" onClick={() => void state.retry()}>Retry</button></div>}
      {!state.loading && !state.error && !items.length && <div className="empty-state"><h3>No backtest results yet</h3><p>Successful experiment candidates will appear here.</p></div>}
      {items.length > 0 && <>
        <CandidateTableShell label="Recent backtest results">
          <table className="history-table backtest-history-table">
            <thead><tr><th>Experiment</th><th>Candidate</th><th>Score</th><th>Return</th><th>Trades</th><th>Completed</th><th>Actions</th></tr></thead>
            <tbody>{items.map((item) => <ResultRow key={item.backtestResultId} item={item} />)}</tbody>
          </table>
        </CandidateTableShell>
        <CandidatePagination label="Recent backtest results" start={start} end={end} total={total}
          canPrevious={state.canPrevious} canNext={Boolean(state.page?.hasMore)} disabled={state.loading}
          onPrevious={state.previous} onNext={state.next} />
      </>}
    </section>
  );
}

function ResultRow({ item }: { item: BacktestResultHistoryItem }) {
  const href = `/backtests?resultId=${encodeURIComponent(item.backtestResultId)}`;
  const returnValue = Number(item.metrics.totalReturn);
  return <tr>
    <td><strong>{item.experimentName}</strong><small className="mono">{item.experimentId}</small></td>
    <td><Link className="history-primary-link" href={href}>{strategyName(item.definition)}</Link><small>Candidate #{item.generationIndex + 1}</small></td>
    <td className="numeric">{item.score === null ? "—" : formatScore(item.score)}</td>
    <td className={`numeric ${returnValue > 0 ? "metric-positive" : returnValue < 0 ? "metric-negative" : ""}`}>{formatPercent(item.metrics.totalReturn, true)}</td>
    <td className="numeric">{item.metrics.numberOfTrades}</td>
    <td><time dateTime={item.completedAt}>{formatDateTime(item.completedAt)}</time></td>
    <td><Link className="button secondary history-action" href={href}>Open</Link></td>
  </tr>;
}
