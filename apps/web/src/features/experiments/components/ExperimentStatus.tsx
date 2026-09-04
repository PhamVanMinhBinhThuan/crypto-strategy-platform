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
      {experiment.failure && (
        <div role="alert" className="inline-error">
          <strong>{experiment.failure.code}</strong> — {experiment.failure.message}
        </div>
      )}
    </header>
  );
}
