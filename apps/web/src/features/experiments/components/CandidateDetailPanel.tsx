"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import type { ApiClient, PublicError } from "@/src/foundation/http/contracts";
import type { CandidateDetail, CandidatePipelineItem } from "../types/experiment";
import { createExperimentService } from "../service/experiment-service";
import {
  failurePresentation,
  formatDateTime,
  formatPercent,
  formatScore,
  parameterSummary,
  statusLabel,
  strategyName
} from "./candidate-presentation";
import { StatusBadge } from "./CandidateTableParts";
import { TechnicalDetails } from "./TechnicalDetails";
import { experimentReturnUrl } from "@/src/foundation/navigation/resource-history";

export function CandidateDetailPanel({
  api,
  experimentId,
  candidateId,
  fallbackCandidate,
  returnView = "results"
}: {
  api: ApiClient;
  experimentId: string;
  candidateId: string;
  fallbackCandidate?: CandidatePipelineItem;
  returnView?: string;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const dialog = useRef<HTMLDialogElement>(null);
  const requestVersion = useRef(0);
  const [detail, setDetail] = useState<CandidateDetail>();
  const [error, setError] = useState<PublicError>();
  const [loading, setLoading] = useState(true);
  const displayedCandidate = detail ?? fallbackCandidate;

  const load = useCallback(async () => {
    const request = ++requestVersion.current;
    setLoading(true);
    setError(undefined);
    const result = await createExperimentService(api).readCandidate(experimentId, candidateId);
    if (request !== requestVersion.current) return;
    if (result.ok) setDetail(result.data);
    else setError(result.error);
    setLoading(false);
  }, [api, candidateId, experimentId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- route entry starts an external API synchronization
    void load();
    const requests = requestVersion;
    return () => {
      requests.current++;
    };
  }, [load]);

  useEffect(() => {
    const current = dialog.current;
    if (!current) return;
    if (!current.open) {
      if (typeof current.showModal === "function") current.showModal();
      else current.setAttribute("open", "");
    }
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previousOverflow;
      if (current.open) {
        if (typeof current.close === "function") current.close();
        else current.removeAttribute("open");
      }
    };
  }, []);

  const close = useCallback(() => {
    const trigger = document.querySelector<HTMLElement>(
      `[data-candidate-detail-trigger="${candidateId}"]`
    );
    const activeTab = document.querySelector<HTMLElement>(".monitor-tabs [aria-current='page']");
    const next = new URLSearchParams(searchParams.toString());
    next.delete("candidateId");
    if (!next.has("view")) next.set("view", returnView);
    if (dialog.current?.open) {
      if (typeof dialog.current.close === "function") dialog.current.close();
      else dialog.current.removeAttribute("open");
    }
    router.replace(`${pathname}?${next.toString()}`, { scroll: false });
    globalThis.setTimeout(() => (trigger?.isConnected ? trigger : activeTab)?.focus(), 0);
  }, [candidateId, pathname, returnView, router, searchParams]);

  return (
    <dialog
      ref={dialog}
      className="candidate-drawer"
      aria-labelledby="candidate-detail-heading"
      onCancel={(event) => {
        event.preventDefault();
        close();
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) close();
      }}
    >
      <div className="candidate-drawer-surface">
        <header className="candidate-drawer-header">
          <div>
            <p className="eyebrow">
              Candidate #{displayedCandidate ? displayedCandidate.generationIndex + 1 : "…"}
            </p>
            <h2 id="candidate-detail-heading">
              {displayedCandidate
                ? strategyName(displayedCandidate.definition)
                : "Candidate details"}
            </h2>
            {displayedCandidate && (
              <div className="candidate-drawer-statuses">
                <StatusBadge value={displayedCandidate.backtest.status} />
                <StatusBadge value={displayedCandidate.ranking.status} />
              </div>
            )}
          </div>
          <button
            type="button"
            className="candidate-drawer-close"
            onClick={close}
            aria-label="Close candidate details"
            autoFocus
          >
            <span aria-hidden="true">×</span>
          </button>
        </header>

        <div className="candidate-drawer-content">
          {loading && <DrawerSkeleton />}
          {!loading && error && !fallbackCandidate && (
            <section className="candidate-detail-error" role="alert">
              <h3>Unable to load candidate details</h3>
              <p>Candidate details are unavailable. Please retry.</p>
              <div className="candidate-detail-actions">
                {error.retryable && (
                  <button type="button" className="button" onClick={() => void load()}>
                    Retry
                  </button>
                )}
                <button type="button" className="button secondary" onClick={close}>
                  Close
                </button>
              </div>
            </section>
          )}
          {!loading && detail && (
            <CandidateDetailContent
              detail={detail}
              returnUrl={experimentReturnUrl(experimentId, returnView)}
            />
          )}
          {!loading && !detail && fallbackCandidate && (
            <CandidatePipelineFallbackContent detail={fallbackCandidate} />
          )}
        </div>
      </div>
    </dialog>
  );
}

function CandidateDetailContent({
  detail,
  returnUrl
}: {
  detail: CandidateDetail;
  returnUrl: string;
}) {
  const failure = failurePresentation(detail);
  const failed = detail.failureStage !== null;
  const retrying = detail.backtest.retryable && detail.backtestResultId === null;
  const summary = parameterSummary(detail.definition);
  return (
    <>
      <section className="candidate-detail-section">
        <h3>Overview</h3>
        <p className="candidate-dataset-summary">
          <strong>{detail.dataset.pair}</strong>
          <span>·</span>
          <span>{detail.dataset.timeframe}</span>
          <span>·</span>
          <span>{new Intl.NumberFormat("en-US").format(detail.dataset.candleCount)} candles</span>
        </p>
        <p className="muted">
          {formatDateTime(detail.dataset.startTime)} – {formatDateTime(detail.dataset.endTime)}
        </p>
        {detail.backtestResultId && (
          <Link
            className="button candidate-primary-action"
            href={`/backtests?resultId=${encodeURIComponent(detail.backtestResultId)}&returnTo=${encodeURIComponent(returnUrl)}`}
            replace
          >
            View full backtest result
          </Link>
        )}
      </section>

      {failed || retrying ? (
        <section className="candidate-detail-section candidate-failure-card" role="status">
          <p className="eyebrow">
            {retrying
              ? "Retry scheduled"
              : detail.failureStage
                ? `${statusLabel(detail.failureStage)} failure`
                : "Pipeline failure"}
          </p>
          <h3>{failure.title}</h3>
          {detail.backtest.retryable && (
            <p>
              {detail.backtest.attemptNo ? `Attempt ${detail.backtest.attemptNo}. ` : ""}
              {detail.backtest.nextRetryAt
                ? `Next retry ${formatDateTime(detail.backtest.nextRetryAt)}.`
                : "The system will retry this candidate."}
            </p>
          )}
        </section>
      ) : detail.metrics ? (
        <section className="candidate-detail-section">
          <h3>Performance</h3>
          <div className="candidate-performance-grid">
            <Metric label="Total return" value={formatPercent(detail.metrics.totalReturn, true)} />
            <Metric label="Win rate" value={formatPercent(detail.metrics.winRate)} />
            <Metric
              label="Maximum drawdown"
              value={formatPercent(detail.metrics.maximumDrawdown)}
            />
            <Metric label="Trades" value={String(detail.metrics.numberOfTrades)} />
          </div>
          {detail.evaluation.score !== null && (
            <p className="candidate-score">
              Evaluation score{" "}
              <strong className="numeric">{formatScore(detail.evaluation.score)}</strong>
            </p>
          )}
          {detail.evaluation.eligibilityReason?.code === "MINIMUM_TRADES_NOT_MET" && (
            <p className="muted">
              Not eligible for ranking: {detail.evaluation.eligibilityReason.actualTrades ?? 0} of{" "}
              {detail.evaluation.eligibilityReason.requiredTrades} required trades.
            </p>
          )}
        </section>
      ) : (
        <section className="candidate-detail-section">
          <h3>Performance</h3>
          <p className="muted">Performance metrics are not available yet.</p>
        </section>
      )}

      <section className="candidate-detail-section">
        <h3>Strategy configuration</h3>
        <p>
          <strong>{strategyName(detail.definition)}</strong>
        </p>
        <p className="muted">{summary || "Default strategy parameters"}</p>
      </section>

      <section className="candidate-detail-section">
        <TechnicalDetails
          values={[
            ["Candidate ID", detail.candidateId],
            ["Candidate fingerprint", detail.candidateFingerprint],
            ["Dataset ID", detail.dataset.datasetId],
            ["Dataset checksum", detail.dataset.checksum],
            ["Dataset version", detail.dataset.version],
            ["Normalization version", detail.dataset.normalizationVersion],
            ["Job ID", detail.backtest.jobId],
            ["Backtest result ID", detail.backtest.backtestResultId],
            ["Evaluation result ID", detail.evaluation.evaluationResultId],
            ["Ranking version", detail.ranking.rankingVersion],
            ["Metric version", detail.evaluation.metricVersion],
            ["Raw backtest status", detail.backtest.status],
            ["Raw evaluation status", detail.evaluation.status],
            ["Raw ranking status", detail.ranking.status],
            ["Failure code", detail.backtest.failure?.code],
            ["Raw failure message", detail.backtest.failure?.message]
          ]}
          jsonValues={[
            ["Immutable strategy definition", detail.definition],
            ["Generator state", detail.generatorState]
          ]}
        />
      </section>
    </>
  );
}

function CandidatePipelineFallbackContent({ detail }: { detail: CandidatePipelineItem }) {
  const failure = failurePresentation(detail);
  return (
    <>
      <section className="candidate-detail-section candidate-failure-card" role="status">
        <p className="eyebrow">
          {detail.failureStage ? `${statusLabel(detail.failureStage)} failure` : "Pipeline failure"}
        </p>
        <h3>{failure.title}</h3>
        {detail.backtest.retryable && (
          <p>
            {detail.backtest.attemptNo ? `Attempt ${detail.backtest.attemptNo}. ` : ""}
            {detail.backtest.nextRetryAt
              ? `Next retry ${formatDateTime(detail.backtest.nextRetryAt)}.`
              : "The system will retry this candidate."}
          </p>
        )}
      </section>
      <section className="candidate-detail-section">
        <h3>Strategy configuration</h3>
        <p>
          <strong>{strategyName(detail.definition)}</strong>
        </p>
        <p className="muted">
          {parameterSummary(detail.definition) || "Default strategy parameters"}
        </p>
      </section>
      <section className="candidate-detail-section">
        <TechnicalDetails
          values={[
            ["Candidate ID", detail.candidateId],
            ["Candidate fingerprint", detail.candidateFingerprint],
            ["Job ID", detail.backtest.jobId],
            ["Raw backtest status", detail.backtest.status],
            ["Raw evaluation status", detail.evaluation.status],
            ["Raw ranking status", detail.ranking.status],
            ["Failure code", detail.backtest.failure?.code],
            ["Raw failure message", detail.backtest.failure?.message]
          ]}
          jsonValues={[["Immutable strategy definition", detail.definition]]}
        />
      </section>
    </>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="candidate-performance-card">
      <span>{label}</span>
      <strong className="numeric">{value}</strong>
    </div>
  );
}

function DrawerSkeleton() {
  return (
    <div className="candidate-drawer-skeleton" role="status" aria-label="Loading candidate details">
      <span />
      <span />
      <span />
      <span />
    </div>
  );
}
