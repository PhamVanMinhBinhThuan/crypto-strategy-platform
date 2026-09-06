"use client";
import { useMemo, useState } from "react";
import type { StrategyDescriptor } from "../model/strategy";
import type { StrategyDraft } from "../model/strategy-draft";
import { validateStrategyParameters } from "../state/strategy-parameter-validator";
import { serializeStrategyParameters } from "../state/strategy-parameter-serializer";
export function StrategyForm({
  descriptor,
  systemStrategies,
  pending,
  onSubmit
}: {
  descriptor?: StrategyDescriptor;
  systemStrategies: readonly StrategyDescriptor[];
  pending: boolean;
  onSubmit: (draft: StrategyDraft) => Promise<void>;
}) {
  const [name, setName] = useState(""),
    [description, setDescription] = useState(""),
    [values, setValues] = useState<Record<string, string>>({}),
    [kind, setKind] = useState<"SINGLE" | "COMPOSITE">("SINGLE"),
    [componentIds, setComponentIds] = useState<string[]>([]),
    [componentValues, setComponentValues] = useState<Record<string, Record<string, string>>>({}),
    [policyId, setPolicyId] = useState<"majority-vote" | "weighted-vote">("majority-vote"),
    [componentWeights, setComponentWeights] = useState<Record<string, string>>({}),
    [copied, setCopied] = useState(false);
  const effectiveValues = useMemo(
    () =>
      Object.fromEntries(
        (descriptor?.parameters ?? []).map((field) => [
          field.name,
          values[field.name] ?? field.defaultValue ?? ""
        ])
      ),
    [descriptor, values]
  );
  const issues = useMemo(
    () => (descriptor ? validateStrategyParameters(descriptor, effectiveValues) : {}),
    [descriptor, effectiveValues]
  );
  const compositeInvalid = useMemo(
    () =>
      systemStrategies
        .filter((item) => componentIds.includes(item.strategyVersionId))
        .some(
          (item) =>
            Object.keys(
              validateStrategyParameters(
                item,
                Object.fromEntries(
                  item.parameters.map((field) => [
                    field.name,
                    componentValues[item.strategyVersionId]?.[field.name] ??
                      field.defaultValue ??
                      ""
                  ])
                )
              )
            ).length > 0
        ),
    [componentIds, componentValues, systemStrategies]
  );
  const weightsValid = useMemo(
    () =>
      policyId === "majority-vote" ||
      systemStrategies
        .filter((item) => componentIds.includes(item.strategyVersionId))
        .every((item) => {
          const raw = componentWeights[item.strategyVersionId] ?? "1";
          return raw.trim() !== "" && Number.isFinite(Number(raw)) && Number(raw) > 0;
        }),
    [componentIds, componentWeights, policyId, systemStrategies]
  );
  const preview = useMemo<StrategyDraft | undefined>(() => {
    if (!descriptor) return undefined;
    const selection = (item: StrategyDescriptor, configuredValues?: Record<string, string>) => ({
      strategyId: item.strategyId,
      version: item.version,
      parameters: serializeStrategyParameters(
        item,
        Object.fromEntries(
          item.parameters.map((field) => [
            field.name,
            configuredValues?.[field.name] ?? field.defaultValue ?? ""
          ])
        )
      )
    });
    return {
      name: name.trim(),
      description,
      kind,
      source:
        kind === "SINGLE"
          ? {
              type: "SINGLE",
              strategy: {
                ...selection(descriptor, effectiveValues),
                parameters: serializeStrategyParameters(descriptor, effectiveValues)
              }
            }
          : {
              type: "COMPOSITE",
              policyId,
              policyVersion: "1.0.0",
              policyParameters:
                policyId === "weighted-vote"
                  ? Object.fromEntries(
                      systemStrategies
                        .filter((item) => componentIds.includes(item.strategyVersionId))
                        .map((item) => [
                          `weight.${item.strategyVersionId}`,
                          componentWeights[item.strategyVersionId] ?? "1"
                        ])
                    )
                  : {},
              components: systemStrategies
                .filter((item) => componentIds.includes(item.strategyVersionId))
                .map((item) => selection(item, componentValues[item.strategyVersionId]))
            }
    };
  }, [
    componentIds,
    componentValues,
    componentWeights,
    description,
    descriptor,
    effectiveValues,
    kind,
    name,
    policyId,
    systemStrategies
  ]);
  const parametersValid =
    kind === "SINGLE" ? Object.keys(issues).length === 0 : !compositeInvalid && weightsValid;
  const sourceValid =
    kind === "SINGLE" || (componentIds.length >= 2 && compositeInvalid === false && weightsValid);

  if (!descriptor)
    return (
      <section className="strategy-form">
        <h2>Create a strategy</h2>
        <p>Select a system strategy to get started.</p>
      </section>
    );
  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!name.trim() || Object.keys(issues).length || (kind === "COMPOSITE" && !sourceValid))
      return;
    if (preview) await onSubmit(preview);
  };
  return (
    <form className="strategy-form" onSubmit={submit}>
      <h2>Create a personal strategy</h2>
      <div className="strategy-form-body">
        <section className="strategy-form-fields">
          <fieldset>
            <legend>Strategy type</legend>
            <label>
              <input type="radio" checked={kind === "SINGLE"} onChange={() => setKind("SINGLE")} />{" "}
              Single
            </label>
            <label>
              <input
                type="radio"
                checked={kind === "COMPOSITE"}
                onChange={() => setKind("COMPOSITE")}
              />{" "}
              Composite
            </label>
          </fieldset>
          <label>
            Name
            <input
              aria-label="Strategy name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              maxLength={200}
            />
          </label>
          <label>
            Description
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={2000}
            />
          </label>
          {kind === "SINGLE" &&
            descriptor.parameters.map((field) => {
              const update = (value: string) =>
                setValues((current) => ({ ...current, [field.name]: value }));
              return (
                <label key={field.name}>
                  {field.name}
                  {field.type === "ENUM" || field.type === "BOOLEAN" ? (
                    <select
                      aria-label={field.name}
                      value={effectiveValues[field.name]}
                      onChange={(event) => update(event.target.value)}
                      aria-invalid={Boolean(issues[field.name])}
                    >
                      {(field.type === "BOOLEAN" ? ["true", "false"] : field.allowedValues).map(
                        (option) => (
                          <option key={option} value={option}>
                            {option}
                          </option>
                        )
                      )}
                    </select>
                  ) : (
                    <input
                      aria-label={field.name}
                      inputMode={field.type === "INTEGER" ? "numeric" : "decimal"}
                      value={effectiveValues[field.name]}
                      onChange={(event) => update(event.target.value)}
                      aria-invalid={Boolean(issues[field.name])}
                    />
                  )}
                  {issues[field.name] && <small role="alert">{issues[field.name]}</small>}
                </label>
              );
            })}
          {kind === "COMPOSITE" && (
            <fieldset>
              <legend>Components (at least 2)</legend>
              <section className="combination-policy-card" aria-label="Combination policy">
                <header>
                  <div>
                    <span className="strategy-section-kicker">Combination policy</span>
                    <strong>
                      {policyId === "majority-vote" ? "Majority Vote" : "Weighted Vote"} · v1.0.0
                    </strong>
                  </div>
                  <span className="policy-badge">
                    {policyId === "majority-vote"
                      ? "Each strategy = 1 vote"
                      : "Each strategy has its own weight"}
                  </span>
                </header>
                <label className="combination-policy-select">
                  Combination policy
                  <select
                    value={policyId}
                    onChange={(event) =>
                      setPolicyId(event.target.value as "majority-vote" | "weighted-vote")
                    }
                  >
                    <option value="majority-vote">Majority Vote</option>
                    <option value="weighted-vote">Weighted Vote</option>
                  </select>
                </label>
                {policyId === "majority-vote" ? (
                  <ol>
                    <li>Each component strategy produces a BUY, SELL, or HOLD signal.</li>
                    <li>The signal with the most votes becomes the composite result.</li>
                    <li>If the leading signals are tied, the system returns HOLD.</li>
                  </ol>
                ) : (
                  <ol>
                    <li>Each signal is multiplied by the weight of its strategy.</li>
                    <li>The signal with the highest total weight becomes the composite result.</li>
                    <li>If the highest totals are tied, the system returns HOLD.</li>
                  </ol>
                )}
                <div className="policy-examples">
                  <span>
                    <code>
                      {policyId === "majority-vote" ? "BUY 2 · SELL 1" : "BUY 0.7 · SELL 0.3"}
                    </code>
                    <strong>→ BUY</strong>
                  </span>
                  <span>
                    <code>
                      {policyId === "majority-vote" ? "BUY 1 · SELL 1" : "BUY 0.5 · SELL 0.5"}
                    </code>
                    <strong>→ HOLD</strong>
                  </span>
                </div>
              </section>
              {systemStrategies.map((item) => {
                const selected = componentIds.includes(item.strategyVersionId);
                const resolvedValues = Object.fromEntries(
                  item.parameters.map((field) => [
                    field.name,
                    componentValues[item.strategyVersionId]?.[field.name] ??
                      field.defaultValue ??
                      ""
                  ])
                );
                const componentIssues = validateStrategyParameters(item, resolvedValues);
                return (
                  <div
                    className={`composite-component${selected ? " is-selected" : ""}`}
                    key={item.strategyVersionId}
                  >
                    <label className="composite-component-toggle">
                      <input
                        type="checkbox"
                        checked={selected}
                        onChange={(event) =>
                          setComponentIds((current) =>
                            event.target.checked
                              ? [...current, item.strategyVersionId]
                              : current.filter((id) => id !== item.strategyVersionId)
                          )
                        }
                      />
                      <span>
                        <strong>{item.displayName}</strong>
                        <small>
                          {item.category} · v{item.version}
                        </small>
                      </span>
                    </label>
                    {selected && (
                      <>
                        {policyId === "weighted-vote" && (
                          <label className="component-weight">
                            Voting weight
                            <input
                              aria-label={`${item.displayName} · voting weight`}
                              type="number"
                              min="0.01"
                              step="0.01"
                              value={componentWeights[item.strategyVersionId] ?? "1"}
                              onChange={(event) =>
                                setComponentWeights((current) => ({
                                  ...current,
                                  [item.strategyVersionId]: event.target.value
                                }))
                              }
                              aria-invalid={
                                !Number.isFinite(
                                  Number(componentWeights[item.strategyVersionId] ?? "1")
                                ) || Number(componentWeights[item.strategyVersionId] ?? "1") <= 0
                              }
                            />
                            <small>The weight for this strategy must be greater than zero.</small>
                          </label>
                        )}
                        <div className="composite-parameter-grid">
                          {item.parameters.length ? (
                            item.parameters.map((field) => {
                              const update = (value: string) =>
                                setComponentValues((current) => ({
                                  ...current,
                                  [item.strategyVersionId]: {
                                    ...current[item.strategyVersionId],
                                    [field.name]: value
                                  }
                                }));
                              return (
                                <label key={field.name}>
                                  {field.name}
                                  {field.type === "ENUM" || field.type === "BOOLEAN" ? (
                                    <select
                                      aria-label={`${item.displayName} · ${field.name}`}
                                      value={resolvedValues[field.name]}
                                      onChange={(event) => update(event.target.value)}
                                      aria-invalid={Boolean(componentIssues[field.name])}
                                    >
                                      {(field.type === "BOOLEAN"
                                        ? ["true", "false"]
                                        : field.allowedValues
                                      ).map((option) => (
                                        <option key={option} value={option}>
                                          {option}
                                        </option>
                                      ))}
                                    </select>
                                  ) : (
                                    <input
                                      aria-label={`${item.displayName} · ${field.name}`}
                                      inputMode={field.type === "INTEGER" ? "numeric" : "decimal"}
                                      value={resolvedValues[field.name]}
                                      onChange={(event) => update(event.target.value)}
                                      aria-invalid={Boolean(componentIssues[field.name])}
                                    />
                                  )}
                                  {componentIssues[field.name] && (
                                    <small role="alert">{componentIssues[field.name]}</small>
                                  )}
                                </label>
                              );
                            })
                          ) : (
                            <small>This strategy has no configurable parameters.</small>
                          )}
                        </div>
                      </>
                    )}
                  </div>
                );
              })}
            </fieldset>
          )}
        </section>
        <aside className="strategy-form-inspector">
          <section className="strategy-validation" aria-label="Strategy validation">
            <header>
              <div>
                <span className="strategy-section-kicker">Validation</span>
                <h3>Pre-save validation</h3>
              </div>
              <span
                className={
                  name.trim() && parametersValid && sourceValid ? "is-valid" : "is-pending"
                }
              >
                {name.trim() && parametersValid && sourceValid ? "Ready" : "Needs attention"}
              </span>
            </header>
            <div className={name.trim() ? "validation-row is-valid" : "validation-row is-pending"}>
              <span>{name.trim() ? "✓" : "!"}</span>
              <p>
                <strong>Strategy name</strong>
                <small>{name.trim() ? "The name is valid" : "Enter a name"}</small>
              </p>
            </div>
            <div
              className={parametersValid ? "validation-row is-valid" : "validation-row is-invalid"}
            >
              <span>{parametersValid ? "✓" : "!"}</span>
              <p>
                <strong>Parameters</strong>
                <small>
                  {parametersValid ? "All parameters are valid" : "Some parameters need attention"}
                </small>
              </p>
            </div>
            <div className={sourceValid ? "validation-row is-valid" : "validation-row is-pending"}>
              <span>{sourceValid ? "✓" : "!"}</span>
              <p>
                <strong>Strategy structure</strong>
                <small>
                  {sourceValid
                    ? "The structure can be saved"
                    : "A composite requires at least two valid components"}
                </small>
              </p>
            </div>
            {kind === "COMPOSITE" && policyId === "weighted-vote" && (
              <div
                className={weightsValid ? "validation-row is-valid" : "validation-row is-invalid"}
              >
                <span>{weightsValid ? "✓" : "!"}</span>
                <p>
                  <strong>Voting weights</strong>
                  <small>
                    {weightsValid
                      ? "Every strategy has a valid weight"
                      : "All weights must be greater than zero"}
                  </small>
                </p>
              </div>
            )}
          </section>
          <section className="strategy-json-preview">
            <header>
              <div>
                <span className="strategy-section-kicker">Payload</span>
                <h3>Strategy definition (JSON)</h3>
              </div>
              <button
                type="button"
                onClick={() => {
                  if (!preview) return;
                  void navigator.clipboard
                    .writeText(JSON.stringify(preview, null, 2))
                    .then(() => setCopied(true));
                }}
              >
                {copied ? "Copied ✓" : "Copy"}
              </button>
            </header>
            <pre>{JSON.stringify(preview, null, 2)}</pre>
          </section>
        </aside>
      </div>
      <button
        className="button"
        disabled={
          pending ||
          !name.trim() ||
          Object.keys(issues).length > 0 ||
          (kind === "COMPOSITE" && !sourceValid)
        }
      >
        {pending ? "Saving…" : "Save strategy"}
      </button>
    </form>
  );
}
