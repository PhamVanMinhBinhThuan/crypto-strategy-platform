"use client";

import Link from "next/link";
import { useEffect, useRef } from "react";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { useCandidatePipeline } from "../hooks/useCandidatePipeline";
import type { CandidatePipelineItem, CandidatePipelineView } from "../types/experiment";
import {
  failurePresentation,
  formatDateTime,
  formatPercent,
  formatScore
} from "./candidate-presentation";
import {
  CandidateCell,
  CandidatePagination,
  CandidateTableShell,
  StatusBadge,
  ViewDetailsAction
} from "./CandidateTableParts";

const labels: Record<CandidatePipelineView, string> = {
  RESULTS: "Results",
  FAILED: "Failed candidates",
  ALL: "Candidate pipeline"
};

export function CandidatePipelineTabs({
  api,
  experimentId,
  view,
  refreshVersion,
  selectedCandidateId,
  onSelectedCandidateAvailable
}: {
  api: ApiClient;
  experimentId: string;
  view: CandidatePipelineView;
  refreshVersion: number;
  selectedCandidateId?: string;
  onSelectedCandidateAvailable?: (candidate: CandidatePipelineItem | undefined) => void;
}) {
  const state = useCandidatePipeline(api, experimentId, view);
  const initialRefresh = useRef(refreshVersion);
  const scrollPositions = useRef<Partial<Record<CandidatePipelineView, number>>>({});
  const notifyUpdate = state.notifyUpdate;
  useEffect(() => {
    if (refreshVersion !== initialRefresh.current) {
      initialRefresh.current = refreshVersion;
      notifyUpdate();
    }
  }, [refreshVersion, notifyUpdate]);

  useEffect(() => {
    const positions = scrollPositions.current;
    const savedPosition = positions[view];
    const frame =
      savedPosition === undefined
        ? undefined
        : globalThis.requestAnimationFrame(() => globalThis.scrollTo(0, savedPosition));
    return () => {
      if (frame !== undefined) globalThis.cancelAnimationFrame(frame);
      positions[view] = globalThis.scrollY;
    };
  }, [view]);

  useEffect(() => {
    if (!selectedCandidateId || !onSelectedCandidateAvailable) return;
    onSelectedCandidateAvailable(
      state.page?.items.find((candidate) => candidate.candidateId === selectedCandidateId)
    );
  }, [onSelectedCandidateAvailable, selectedCandidateId, state.page]);

  const counts = state.counts;
  const total =
    view === "RESULTS"
      ? counts.resultCount
      : view === "FAILED"
        ? counts.failedCount
        : counts.totalCount;
  const itemCount = state.page?.items.length ?? 0;
  const start = itemCount === 0 ? 0 : (state.pageNumber - 1) * 10 + 1;
  const end = itemCount === 0 ? 0 : start + itemCount - 1;

  return (
    <section className="panel pipeline-panel">
      <nav className="monitor-tabs" aria-label="Experiment result views">
        {(Object.keys(labels) as CandidatePipelineView[]).map((item) => (
          <Link
            key={item}
            href={`/search/${encodeURIComponent(experimentId)}?view=${item.toLowerCase()}`}
            className={view === item ? "active" : ""}
            aria-current={view === item ? "page" : undefined}
            scroll={false}
          >
            {labels[item]}
            {state.countsLoaded && (
              <span>
                {item === "RESULTS"
                  ? counts.resultCount
                  : item === "FAILED"
                    ? counts.failedCount
                    : counts.totalCount}
              </span>
            )}
          </Link>
        ))}
      </nav>

      {state.stale && (
        <div className="update-notice" role="status" aria-live="polite">
          <span>New results available.</span>
          <button type="button" className="button secondary" onClick={state.refresh}>
            Refresh this page
          </button>
        </div>
      )}
      {state.loading && !state.page && <CandidateTableSkeleton />}
      {state.error && (
        <div className="candidate-table-error" role="alert">
          <p>{state.error}</p>
          <button type="button" className="button secondary" onClick={state.refresh}>
            Retry
          </button>
        </div>
      )}
      {state.page && state.page.items.length === 0 && (
        <p className="empty-copy">{emptyMessage(view)}</p>
      )}
      {state.page && state.page.items.length > 0 && (
        <CandidateTableShell label={`${labels[view]} table`}>
          {view === "RESULTS" ? (
            <ResultsTable items={state.page.items} experimentId={experimentId} view={view} />
          ) : view === "FAILED" ? (
            <FailuresTable items={state.page.items} experimentId={experimentId} view={view} />
          ) : (
            <PipelineTable items={state.page.items} experimentId={experimentId} view={view} />
          )}
        </CandidateTableShell>
      )}

      <CandidatePagination
        label={labels[view]}
        start={start}
        end={end}
        total={total}
        canPrevious={state.canPrevious}
        canNext={!!state.page?.hasMore}
        disabled={state.loading || state.stale}
        onPrevious={state.previous}
        onNext={state.next}
      />
    </section>
  );
}

function ResultsTable({
  items,
  experimentId,
  view
}: {
  items: readonly CandidatePipelineItem[];
  experimentId: string;
  view: CandidatePipelineView;
}) {
  return (
    <table className="candidate-table candidate-results-table">
      <thead>
        <tr>
          <th>Ranking</th>
          <th>Candidate</th>
          <th className="numeric">Score</th>
          <th className="numeric">Return</th>
          <th className="numeric">Win rate</th>
          <th className="numeric">Drawdown</th>
          <th className="numeric">Trades</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => {
          const returnValue = Number(item.evaluation.totalReturn);
          return (
            <tr key={item.candidateId}>
              <td>
                <RankingValue item={item} />
              </td>
              <td>
                <CandidateCell item={item} />
              </td>
              <td className="numeric" title={item.evaluation.score ?? undefined}>
                {formatScore(item.evaluation.score)}
              </td>
              <td
                className={`numeric metric-return ${returnValue > 0 ? "positive" : returnValue < 0 ? "negative" : ""}`}
              >
                {formatPercent(item.evaluation.totalReturn, true)}
              </td>
              <td className="numeric">{formatPercent(item.evaluation.winRate)}</td>
              <td className="numeric">{formatPercent(item.evaluation.maximumDrawdown)}</td>
              <td className="numeric">{item.evaluation.numberOfTrades ?? "—"}</td>
              <td>
                <ViewDetailsAction
                  experimentId={experimentId}
                  candidateId={item.candidateId}
                  backtestResultId={item.backtest.backtestResultId}
                  failed={item.failureStage !== null}
                  candidateNumber={item.generationIndex + 1}
                  view={view}
                />
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

function RankingValue({ item }: { item: CandidatePipelineItem }) {
  if (item.ranking.rank) {
    return (
      <span className={`rank-badge rank-${Math.min(item.ranking.rank, 4)}`}>
        #{item.ranking.rank}
      </span>
    );
  }
  if (item.ranking.status === "INELIGIBLE") {
    const reason = item.evaluation.eligibilityReason;
    return (
      <div className="ranking-value">
        <StatusBadge value="INELIGIBLE" />
        {reason?.actualTrades != null && reason.requiredTrades != null && (
          <small>
            {reason.actualTrades}/{reason.requiredTrades} trades
          </small>
        )}
      </div>
    );
  }
  return <StatusBadge value={item.ranking.status} />;
}

function FailuresTable({
  items,
  experimentId,
  view
}: {
  items: readonly CandidatePipelineItem[];
  experimentId: string;
  view: CandidatePipelineView;
}) {
  return (
    <table className="candidate-table candidate-failures-table">
      <thead>
        <tr>
          <th>Candidate</th>
          <th>Failed stage</th>
          <th>Reason</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => {
          const failure = failurePresentation(item);
          return (
            <tr key={item.candidateId}>
              <td>
                <CandidateCell item={item} />
              </td>
              <td>
                <StatusBadge value={item.failureStage ?? "UNKNOWN"} />
              </td>
              <td>
                <div className="failure-reason" title={failure.title}>
                  <strong>{failure.title}</strong>
                  {item.backtest.retryable && (
                    <small>
                      Retry scheduled
                      {item.backtest.attemptNo ? ` · Attempt ${item.backtest.attemptNo}` : ""}
                      {item.backtest.nextRetryAt
                        ? ` · ${formatDateTime(item.backtest.nextRetryAt)}`
                        : ""}
                    </small>
                  )}
                </div>
              </td>
              <td>
                <ViewDetailsAction
                  experimentId={experimentId}
                  candidateId={item.candidateId}
                  backtestResultId={item.backtest.backtestResultId}
                  failed={item.failureStage !== null}
                  candidateNumber={item.generationIndex + 1}
                  view={view}
                />
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

function PipelineTable({
  items,
  experimentId,
  view
}: {
  items: readonly CandidatePipelineItem[];
  experimentId: string;
  view: CandidatePipelineView;
}) {
  return (
    <table className="candidate-table candidate-pipeline-table">
      <thead>
        <tr>
          <th>Candidate</th>
          <th>Backtest</th>
          <th>Evaluation</th>
          <th>Ranking</th>
          <th>Outcome</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr key={item.candidateId}>
            <td>
              <CandidateCell item={item} />
            </td>
            <td>
              <StatusBadge value={item.backtest.status} />
            </td>
            <td>
              <StatusBadge value={item.evaluation.status} />
            </td>
            <td>
              {item.ranking.rank ? (
                <strong className="numeric">Rank #{item.ranking.rank}</strong>
              ) : (
                <StatusBadge value={item.ranking.status} />
              )}
            </td>
            <td>
              {item.evaluation.score !== null ? (
                <span className="numeric">Score {formatScore(item.evaluation.score)}</span>
              ) : item.backtest.retryable ? (
                `Retry scheduled${item.backtest.attemptNo ? ` · Attempt ${item.backtest.attemptNo}` : ""}`
              ) : item.backtest.failure ? (
                failurePresentation(item).title
              ) : (
                "—"
              )}
            </td>
            <td>
              <ViewDetailsAction
                experimentId={experimentId}
                candidateId={item.candidateId}
                backtestResultId={item.backtest.backtestResultId}
                failed={item.failureStage !== null}
                candidateNumber={item.generationIndex + 1}
                view={view}
              />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function CandidateTableSkeleton() {
  return (
    <div className="candidate-table-skeleton" role="status" aria-label="Loading candidate table">
      {Array.from({ length: 5 }, (_, index) => (
        <span key={index} />
      ))}
    </div>
  );
}

const emptyMessage = (view: CandidatePipelineView) => {
  if (view === "RESULTS") return "No evaluated candidates yet.";
  if (view === "FAILED") return "No failed candidates.";
  return "No candidates have been generated yet.";
};
