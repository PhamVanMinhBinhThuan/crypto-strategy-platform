"use client";

import type { ReactNode } from "react";
import type { SearchParameterDomain } from "../types/experiment-configuration";

export function ExperimentConfigSection({
  id,
  number,
  title,
  description,
  status,
  children
}: {
  id: string;
  number: number;
  title: string;
  description: string;
  status?: string;
  children: ReactNode;
}) {
  return (
    <section
      id={id}
      className="experiment-config-section"
      aria-labelledby={`${id}-title`}
      tabIndex={-1}
    >
      <header className="experiment-config-section-header">
        <span className="experiment-config-section-number" aria-hidden="true">
          {number}
        </span>
        <div>
          <div className="experiment-config-section-title-row">
            <h3 id={`${id}-title`}>{title}</h3>
            {status && <span className="experiment-config-section-status">{status}</span>}
          </div>
          <p>{description}</p>
        </div>
      </header>
      <div className="experiment-config-section-body">{children}</div>
    </section>
  );
}

export function StrategySelectionCard({
  name,
  description,
  selected,
  disabled,
  badge,
  onChange,
  children
}: {
  name: string;
  description: string;
  selected: boolean;
  disabled?: boolean;
  badge?: string;
  onChange: () => void;
  children?: ReactNode;
}) {
  return (
    <article className={`experiment-strategy-card${selected ? " is-selected" : ""}`}>
      <label>
        <input
          type="checkbox"
          checked={selected}
          disabled={disabled}
          onChange={onChange}
          aria-label={`${selected ? "Remove" : "Include"} ${name}`}
        />
        <span className="experiment-strategy-copy">
          <span className="experiment-strategy-name-row">
            <strong>{name}</strong>
            {selected && <span className="experiment-config-badge">Selected</span>}
            {badge && <span className="experiment-config-badge subtle">{badge}</span>}
          </span>
          <small>{description || "A reusable strategy available for candidate generation."}</small>
        </span>
      </label>
      {children}
    </article>
  );
}

export function SearchParameterEditor({
  strategyLabel,
  parameterName,
  label,
  description,
  domain,
  recommended,
  error,
  onBlur,
  onChange,
  onReset
}: {
  strategyLabel: string;
  parameterName: string;
  label: string;
  description?: string;
  domain: SearchParameterDomain;
  recommended?: SearchParameterDomain;
  error?: string;
  onBlur: () => void;
  onChange: (domain: SearchParameterDomain) => void;
  onReset?: () => void;
}) {
  if (domain.kind === "OPTIONS") {
    return (
      <div className="experiment-parameter-row">
        <div className="experiment-parameter-copy">
          <strong>{label}</strong>
          {description && <small>{description}</small>}
        </div>
        <label className="experiment-parameter-options">
          <span>Options</span>
          <input
            aria-label={`${strategyLabel} ${parameterName} options`}
            value={domain.options.join(", ")}
            onBlur={onBlur}
            aria-invalid={!!error}
            onChange={(event) =>
              onChange({
                kind: "OPTIONS",
                options: event.target.value
                  .split(",")
                  .map((value) => value.trim())
                  .filter(Boolean)
              })
            }
          />
        </label>
        {error && (
          <small className="experiment-field-error" role="alert">
            {error}
          </small>
        )}
      </div>
    );
  }

  const recommendedRange = recommended?.kind === "RANGE" ? recommended : undefined;
  const inputMode = domain.valueType === "DECIMAL" ? "decimal" : "numeric";
  return (
    <div className="experiment-parameter-row">
      <div className="experiment-parameter-heading">
        <div className="experiment-parameter-copy">
          <strong>{label}</strong>
          {description && <small>{description}</small>}
          {recommendedRange && (
            <small className="experiment-recommended-range">
              Recommended: {recommendedRange.minimum}–{recommendedRange.maximum}, step{" "}
              {recommendedRange.step}
            </small>
          )}
        </div>
        {recommendedRange && onReset && (
          <button type="button" className="experiment-text-button" onClick={onReset}>
            Reset recommended values
          </button>
        )}
      </div>
      <div className="experiment-parameter-inputs">
        <label>
          <span>Minimum</span>
          <input
            aria-label={`${strategyLabel} ${parameterName} minimum`}
            inputMode={inputMode}
            value={domain.minimum}
            onBlur={onBlur}
            aria-invalid={!!error}
            onChange={(event) => onChange({ ...domain, minimum: event.target.value })}
          />
        </label>
        <label>
          <span>Maximum</span>
          <input
            aria-label={`${strategyLabel} ${parameterName} maximum`}
            inputMode={inputMode}
            value={domain.maximum}
            onBlur={onBlur}
            aria-invalid={!!error}
            onChange={(event) => onChange({ ...domain, maximum: event.target.value })}
          />
        </label>
        <label>
          <span>Step</span>
          <input
            aria-label={`${strategyLabel} ${parameterName} step`}
            inputMode={inputMode}
            value={domain.step ?? (domain.valueType === "DECIMAL" ? "0.1" : "1")}
            onBlur={onBlur}
            aria-invalid={!!error}
            onChange={(event) => onChange({ ...domain, step: event.target.value })}
          />
        </label>
      </div>
      {error && (
        <small className="experiment-field-error" role="alert">
          {error}
        </small>
      )}
    </div>
  );
}

export function ExperimentConfigurationSummary({
  headingId = "experiment-summary-title",
  dataset,
  strategyCount,
  cardinality,
  candidateLimit,
  timeLimit,
  concurrency,
  leaderboardSize
}: {
  headingId?: string;
  dataset: string;
  strategyCount: number;
  cardinality: string;
  candidateLimit: string;
  timeLimit: string;
  concurrency: number;
  leaderboardSize: number;
}) {
  return (
    <aside className="experiment-config-summary" aria-labelledby={headingId}>
      <h3 id={headingId}>Experiment summary</h3>
      <dl>
        <div>
          <dt>Dataset</dt>
          <dd>{dataset}</dd>
        </div>
        <div>
          <dt>Strategies</dt>
          <dd>{strategyCount || "None selected"}</dd>
        </div>
        <div>
          <dt>Possible configurations</dt>
          <dd>{cardinality}</dd>
        </div>
        <div>
          <dt>Candidate limit</dt>
          <dd>{candidateLimit || "Not set"}</dd>
        </div>
        <div>
          <dt>Time limit</dt>
          <dd>{timeLimit}</dd>
        </div>
        <div>
          <dt>Parallel backtests</dt>
          <dd>{concurrency}</dd>
        </div>
        <div>
          <dt>Leaderboard size</dt>
          <dd>{leaderboardSize}</dd>
        </div>
      </dl>
    </aside>
  );
}
