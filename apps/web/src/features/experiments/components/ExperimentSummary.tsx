import type { Experiment } from "../types/experiment";
import { TechnicalDetails } from "./TechnicalDetails";

const formatDate = (value: string) =>
  new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "UTC"
  }).format(new Date(value));
const friendlyReason = (value: string | null) =>
  value === "MAXIMUM_CANDIDATES"
    ? "Candidate limit reached"
    : value === "SEARCH_SPACE_EXHAUSTED"
      ? "Search space exhausted"
      : value === "EXPLICIT_STOP"
        ? "Stopped by user"
        : (value?.replaceAll("_", " ") ?? null);
const formatDuration = (start: string | null, end: string | null) => {
  if (!start) return null;
  const seconds = Math.max(
    0,
    Math.floor((Date.parse(end ?? new Date().toISOString()) - Date.parse(start)) / 1000)
  );
  return seconds >= 60 ? `${Math.floor(seconds / 60)}m ${seconds % 60}s` : `${seconds}s`;
};

export function ExperimentSummary({ experiment }: { experiment: Experiment }) {
  const progress = experiment.searchProgress;
  const processed = progress ? progress.completed + progress.failed : 0;
  return (
    <header className="feature-header experiment-summary">
      <div>
        <p className="eyebrow">Experiment monitor</p>
        <h1>{experiment.name}</h1>
        <p className="muted">
          {experiment.dataset
            ? `${experiment.dataset.provider} · ${experiment.dataset.pair} · ${experiment.dataset.timeframe}`
            : `Dataset ${experiment.datasetId}`}
        </p>
        {experiment.dataset && (
          <p className="muted">
            {formatDate(experiment.dataset.startTime)} → {formatDate(experiment.dataset.endTime)}{" "}
            UTC · {experiment.dataset.candleCount} candles
          </p>
        )}
      </div>
      <span className={`status status-${experiment.status.toLowerCase()}`}>
        {experiment.status.replaceAll("_", " ")}
      </span>
      {progress && (
        <dl aria-label="Authoritative Search progress" className="metric-grid">
          <div>
            <dt>Processed</dt>
            <dd>
              {processed}/{progress.allocated}
            </dd>
          </div>
          <div>
            <dt>Active</dt>
            <dd>{progress.active}</dd>
          </div>
          <div>
            <dt>Succeeded</dt>
            <dd>{progress.completed}</dd>
          </div>
          <div>
            <dt>Failed</dt>
            <dd>{progress.failed}</dd>
          </div>
          <div>
            <dt>Duration</dt>
            <dd>{formatDuration(experiment.startedAt, experiment.completedAt) ?? "—"}</dd>
          </div>
          <div>
            <dt>Candidate limit</dt>
            <dd>{progress.configuredMaximum}</dd>
          </div>
          {progress.terminalReason && (
            <div>
              <dt>Finished because</dt>
              <dd>{friendlyReason(progress.terminalReason)}</dd>
            </div>
          )}
        </dl>
      )}
      <TechnicalDetails
        values={[
          ["Experiment ID", experiment.experimentId],
          ["Dataset ID", experiment.datasetId],
          ["Dataset checksum", experiment.dataset?.checksum],
          ["Normalization version", experiment.dataset?.normalizationVersion],
          ["Terminal reason code", progress?.terminalReason]
        ]}
      />
      {experiment.failure && (
        <div role="alert" className="inline-error">
          <strong>{experiment.failure.code}</strong> — {experiment.failure.message}
        </div>
      )}
    </header>
  );
}
