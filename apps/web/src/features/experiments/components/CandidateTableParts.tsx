import Link from "next/link";
import type { ReactNode } from "react";
import type { CandidatePipelineItem, CandidatePipelineView } from "../types/experiment";
import { parameterSummary, statusLabel, strategyName } from "./candidate-presentation";
import { experimentReturnUrl } from "@/src/foundation/navigation/resource-history";

export function CandidateCell({ item }: { item: CandidatePipelineItem }) {
  const summary = parameterSummary(item.definition);
  return (
    <div className="candidate-cell">
      <div className="candidate-cell-heading">
        <strong>{strategyName(item.definition)}</strong>
        <span>Candidate #{item.generationIndex + 1}</span>
      </div>
      {summary && <small title={summary}>{summary}</small>}
    </div>
  );
}

export function StatusBadge({ value }: { value: string }) {
  return (
    <span className={`status status-${value.toLowerCase().replaceAll("_", "-")}`}>
      {statusLabel(value)}
    </span>
  );
}

export function ViewDetailsAction({
  experimentId,
  candidateId,
  backtestResultId,
  failed = false,
  candidateNumber,
  view
}: {
  experimentId: string;
  candidateId: string;
  backtestResultId: string | null;
  failed?: boolean;
  candidateNumber: number;
  view: CandidatePipelineView;
}) {
  const returnUrl = experimentReturnUrl(experimentId, view.toLowerCase());
  const href =
    backtestResultId && !failed && view !== "FAILED"
      ? `/backtests?resultId=${encodeURIComponent(backtestResultId)}&returnTo=${encodeURIComponent(returnUrl)}`
      : `/search/${encodeURIComponent(experimentId)}?view=${view.toLowerCase()}&candidateId=${encodeURIComponent(candidateId)}`;

  return (
    <Link
      className="button secondary candidate-action"
      data-candidate-detail-trigger={candidateId}
      href={href}
      scroll={false}
      aria-label={`View details for Candidate #${candidateNumber}`}
    >
      View details
    </Link>
  );
}

export function CandidateTableShell({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div
      className="table-scroll candidate-table-scroll"
      tabIndex={0}
      role="region"
      aria-label={label}
    >
      {children}
    </div>
  );
}

export function CandidatePagination({
  label,
  start,
  end,
  total,
  canPrevious,
  canNext,
  disabled,
  onPrevious,
  onNext
}: {
  label: string;
  start: number;
  end: number;
  total: number;
  canPrevious: boolean;
  canNext: boolean;
  disabled: boolean;
  onPrevious: () => void;
  onNext: () => void;
}) {
  return (
    <footer className="pagination" aria-label={`${label} pagination`}>
      <button type="button" disabled={!canPrevious || disabled} onClick={onPrevious}>
        Previous
      </button>
      <span aria-live="polite">
        Showing {start}–{end} of {total}
      </span>
      <button type="button" disabled={!canNext || disabled} onClick={onNext}>
        Next
      </button>
    </footer>
  );
}
