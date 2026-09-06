import type { Experiment } from "../types/experiment";
export function ExperimentStatus({ experiment }: { experiment: Experiment }) {
  return (
    <header className="feature-header">
      <div>
        <p className="eyebrow">Experiment · {experiment.experimentId}</p>
        <h1>{experiment.name}</h1>
        <p className="muted">Dataset {experiment.datasetId}</p>
      </div>
      <span className={`status status-${experiment.status.toLowerCase()}`}>
        {experiment.status.replaceAll("_", " ")}
      </span>
      {experiment.searchProgress && (
        <dl aria-label="Authoritative Search progress" className="metric-grid">
          <div>
            <dt>Allocated</dt>
            <dd>{experiment.searchProgress.allocated}</dd>
          </div>
          <div>
            <dt>Active</dt>
            <dd>{experiment.searchProgress.active}</dd>
          </div>
          <div>
            <dt>Completed</dt>
            <dd>{experiment.searchProgress.completed}</dd>
          </div>
          <div>
            <dt>Failed</dt>
            <dd>{experiment.searchProgress.failed}</dd>
          </div>
          <div>
            <dt>Remaining</dt>
            <dd>{experiment.searchProgress.remainingCapacity}</dd>
          </div>
          <div>
            <dt>Maximum</dt>
            <dd>{experiment.searchProgress.configuredMaximum}</dd>
          </div>
          {experiment.searchProgress.terminalReason && (
            <div>
              <dt>Terminal reason</dt>
              <dd>{experiment.searchProgress.terminalReason}</dd>
            </div>
          )}
        </dl>
      )}
      {experiment.failure && (
        <div role="alert" className="inline-error">
          <strong>{experiment.failure.code}</strong> — {experiment.failure.message}
        </div>
      )}
    </header>
  );
}
