"use client";

import Link from "next/link";
import { useCallback } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { useCursorPagination } from "@/src/foundation/ui/useCursorPagination";
import { createExperimentService } from "../service/experiment-service";
import type { ExperimentHistoryItem } from "../types/experiment";
import { CandidatePagination, CandidateTableShell, StatusBadge } from "./CandidateTableParts";
import { formatDateTime } from "./candidate-presentation";

export function RecentExperiments({ api }: { api: ApiClient }) {
  const loadPage = useCallback(
    (cursor?: string) => createExperimentService(api).readRecentExperiments(cursor),
    [api]
  );
  const state = useCursorPagination<ExperimentHistoryItem>(loadPage);
  const items = state.page?.items ?? [];
  const total = state.page?.totalCount ?? 0;
  const start = items.length ? (state.pageNumber - 1) * 10 + 1 : 0;
  const end = items.length ? start + items.length - 1 : 0;

  return (
    <section className="panel history-panel" aria-labelledby="recent-experiments-heading">
      <div className="history-heading">
        <div>
          <p className="eyebrow">Experiment history</p>
          <h2 id="recent-experiments-heading">Recent experiments</h2>
        </div>
        <Link className="button" href="/search?mode=new">
          New experiment
        </Link>
      </div>
      {state.loading && !state.page && <p role="status">Loading recent experiments…</p>}
      {state.error && (
        <div className="history-error" role="alert">
          <p>{state.error}</p>
          <button className="button secondary" type="button" onClick={() => void state.retry()}>
            Retry
          </button>
        </div>
      )}
      {!state.loading && !state.error && !items.length && (
        <div className="empty-state">
          <h3>No experiments yet</h3>
          <p>Create your first experiment to start searching and comparing strategies.</p>
        </div>
      )}
      {items.length > 0 && (
        <>
          <CandidateTableShell label="Recent experiments">
            <table className="history-table">
              <thead>
                <tr>
                  <th>Experiment</th>
                  <th>Dataset</th>
                  <th>Status</th>
                  <th>Progress</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <ExperimentRow key={item.experimentId} item={item} />
                ))}
              </tbody>
            </table>
          </CandidateTableShell>
          <CandidatePagination
            label="Recent experiments"
            start={start}
            end={end}
            total={total}
            canPrevious={state.canPrevious}
            canNext={Boolean(state.page?.hasMore)}
            disabled={state.loading}
            onPrevious={state.previous}
            onNext={state.next}
          />
        </>
      )}
    </section>
  );
}

function ExperimentRow({ item }: { item: ExperimentHistoryItem }) {
  const href = `/search/${encodeURIComponent(item.experimentId)}?view=results`;
  return (
    <tr>
      <td>
        <Link className="history-primary-link" href={href}>
          {item.name}
        </Link>
        <small className="mono">{item.experimentId}</small>
      </td>
      <td>
        <strong>
          {item.dataset.provider} · {item.dataset.pair} · {item.dataset.timeframe}
        </strong>
        <small>{new Intl.NumberFormat("en-US").format(item.dataset.candleCount)} candles</small>
      </td>
      <td>
        <StatusBadge value={item.status} />
      </td>
      <td className="numeric">
        <strong>
          {item.progress.processed}/{item.progress.total}
        </strong>
        <small>
          {item.progress.succeeded} succeeded · {item.progress.failed} failed
        </small>
      </td>
      <td>
        <time dateTime={item.createdAt}>{formatDateTime(item.createdAt)}</time>
      </td>
      <td>
        <Link className="button secondary history-action" href={href}>
          Open
        </Link>
      </td>
    </tr>
  );
}
