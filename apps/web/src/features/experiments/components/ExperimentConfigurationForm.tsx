"use client";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { useExperimentConfiguration } from "../hooks/useExperimentConfiguration";
import { useExperimentCommands } from "../hooks/useExperimentCommands";
import { DependencyGateNotice } from "./DependencyGateNotice";
export function ExperimentConfigurationForm({
  api,
  fixture,
  reproduceId
}: {
  api: ApiClient;
  fixture: boolean;
  reproduceId?: string;
}) {
  const { draft, errors, update, updateParameter, validate } = useExperimentConfiguration();
  const commands = useExperimentCommands(api);
  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validate()) void commands.startExperiment(draft);
  };
  return (
    <section className="panel config-panel">
      <div className="section-heading">
        <h2>Configure Experiment</h2>
        {fixture && <span className="fixture-badge">FIXTURE DATA</span>}
      </div>
      <DependencyGateNotice />
      <form onSubmit={submit} noValidate>
        <div className="form-grid">
          <label>
            Name
            <input
              aria-label="Name"
              value={draft.name}
              onChange={(e) => update("name", e.target.value)}
              aria-invalid={!!errors.name}
            />
            {errors.name && <small role="alert">{errors.name}</small>}
          </label>
          <label>
            Dataset ID <small>Known ID; no production catalog</small>
            <input
              aria-label="Dataset ID"
              value={draft.datasetId}
              onChange={(e) => update("datasetId", e.target.value)}
              aria-invalid={!!errors.datasetId}
            />
            {errors.datasetId && <small role="alert">{errors.datasetId}</small>}
          </label>
          <label>
            Generator <small>Fixture-only discovery</small>
            <input
              aria-label="Generator"
              value={draft.generatorId}
              onChange={(e) => update("generatorId", e.target.value)}
            />
          </label>
          <label>
            Generator version
            <input
              aria-label="Generator version"
              value={draft.generatorVersion}
              onChange={(e) => update("generatorVersion", e.target.value)}
            />
          </label>
          <label>
            Seed
            <input
              aria-label="Seed"
              inputMode="numeric"
              value={draft.seed}
              onChange={(e) => update("seed", e.target.value)}
            />
          </label>
          <label>
            Strategy
            <input
              aria-label="Strategy"
              value={draft.strategyId}
              onChange={(e) => update("strategyId", e.target.value)}
            />
          </label>
          <label>
            Strategy version
            <input
              aria-label="Strategy version"
              value={draft.strategyVersion}
              onChange={(e) => update("strategyVersion", e.target.value)}
            />
          </label>
          {Object.entries(draft.parameters).map(([name, range]) => (
            <fieldset key={name}>
              <legend>{name} range</legend>
              <label>
                Minimum
                <input
                  aria-label={`${name} minimum`}
                  inputMode="decimal"
                  value={range.minimum}
                  onChange={(e) => updateParameter(name, "minimum", e.target.value)}
                />
              </label>
              <label>
                Maximum
                <input
                  aria-label={`${name} maximum`}
                  inputMode="decimal"
                  value={range.maximum}
                  onChange={(e) => updateParameter(name, "maximum", e.target.value)}
                />
              </label>
              {errors[`parameter-${name}`] && (
                <small role="alert">{errors[`parameter-${name}`]}</small>
              )}
            </fieldset>
          ))}
          <label>
            Maximum candidates
            <input
              aria-label="Maximum candidates"
              type="number"
              min="1"
              value={draft.maximumCandidates}
              onChange={(e) => update("maximumCandidates", e.target.value)}
            />
          </label>
          <label>
            Maximum duration (seconds)
            <input
              aria-label="Maximum duration (seconds)"
              type="number"
              min="1"
              value={draft.maximumDurationSeconds}
              onChange={(e) => update("maximumDurationSeconds", e.target.value)}
            />
          </label>
          <label>
            Top-K
            <select
              aria-label="Top-K"
              value={draft.topK}
              onChange={(e) => update("topK", Number(e.target.value))}
            >
              {[10, 25, 50].map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
            {errors.topK && <small role="alert">{errors.topK}</small>}
          </label>
        </div>
        {errors.stop && <p role="alert">{errors.stop}</p>}
        <button className="button primary" disabled={commands.start.status === "submitting"}>
          Start Experiment
        </button>
        {reproduceId && (
          <button
            type="button"
            className="button secondary"
            disabled={commands.reproduce.status === "submitting"}
            onClick={() => void commands.reproduceExperiment(reproduceId)}
          >
            Reproduce Experiment
          </button>
        )}
        {commands.start.status === "accepted" && (
          <p role="status">Fixture request accepted. A predefined response was returned.</p>
        )}
        {commands.start.status === "dependency-unavailable" && (
          <p role="alert">Production start remains blocked by the Search Coordinator dependency.</p>
        )}
        {commands.reproduce.status === "accepted" && (
          <p role="status">Fixture reproduction accepted as a new linked Experiment.</p>
        )}
        {commands.reproduce.status === "dependency-unavailable" && (
          <p role="alert">
            Production reproduction remains blocked by the Search Coordinator dependency.
          </p>
        )}
      </form>
    </section>
  );
}
