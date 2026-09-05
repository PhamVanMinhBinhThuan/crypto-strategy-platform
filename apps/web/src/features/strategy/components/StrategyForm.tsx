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
    [componentIds, setComponentIds] = useState<string[]>([]);
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
                  item.parameters.map((field) => [field.name, field.defaultValue ?? ""])
                )
              )
            ).length > 0
        ),
    [componentIds, systemStrategies]
  );
  if (!descriptor)
    return (
      <section className="strategy-form">
        <h2>Tạo Strategy</h2>
        <p>Chọn Strategy hệ thống để bắt đầu.</p>
      </section>
    );
  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (
      !name.trim() ||
      Object.keys(issues).length ||
      (kind === "COMPOSITE" && (componentIds.length < 2 || compositeInvalid))
    )
      return;
    const selection = (item: StrategyDescriptor) => ({
      strategyId: item.strategyId,
      version: item.version,
      parameters: serializeStrategyParameters(
        item,
        Object.fromEntries(item.parameters.map((field) => [field.name, field.defaultValue ?? ""]))
      )
    });
    await onSubmit({
      name: name.trim(),
      description,
      kind,
      source:
        kind === "SINGLE"
          ? {
              type: "SINGLE",
              strategy: {
                ...selection(descriptor),
                parameters: serializeStrategyParameters(descriptor, effectiveValues)
              }
            }
          : {
              type: "COMPOSITE",
              policyId: "majority-vote",
              policyVersion: "1.0.0",
              policyParameters: {},
              components: systemStrategies
                .filter((item) => componentIds.includes(item.strategyVersionId))
                .map(selection)
            }
    });
  };
  return (
    <form className="strategy-form" onSubmit={submit}>
      <h2>Tạo Strategy riêng</h2>
      <fieldset>
        <legend>Loại Strategy</legend>
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
        Tên
        <input
          aria-label="Tên Strategy"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          maxLength={200}
        />
      </label>
      <label>
        Mô tả
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
          <legend>Thành phần (ít nhất 2)</legend>
          <p role="note">
            Quy tắc majority vote: tín hiệu có nhiều phiếu nhất được chọn; nếu xung đột hòa thì trả
            HOLD.
          </p>
          {systemStrategies.map((item) => (
            <label key={item.strategyVersionId}>
              <input
                type="checkbox"
                checked={componentIds.includes(item.strategyVersionId)}
                onChange={(event) =>
                  setComponentIds((current) =>
                    event.target.checked
                      ? [...current, item.strategyVersionId]
                      : current.filter((id) => id !== item.strategyVersionId)
                  )
                }
              />{" "}
              {item.displayName} · v{item.version}
            </label>
          ))}
        </fieldset>
      )}
      <button
        className="button"
        disabled={
          pending ||
          !name.trim() ||
          Object.keys(issues).length > 0 ||
          (kind === "COMPOSITE" && (componentIds.length < 2 || compositeInvalid))
        }
      >
        {pending ? "Đang lưu…" : "Lưu Strategy"}
      </button>
    </form>
  );
}
