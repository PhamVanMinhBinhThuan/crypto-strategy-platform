"use client";
import { useMemo, useState } from "react";
import type { StrategyDescriptor } from "../model/strategy";
import type { StrategyDraft } from "../model/strategy-draft";
import { validateStrategyParameters } from "../state/strategy-parameter-validator";
export function StrategyForm({
  descriptor,
  pending,
  onSubmit
}: {
  descriptor?: StrategyDescriptor;
  pending: boolean;
  onSubmit: (draft: StrategyDraft) => Promise<void>;
}) {
  const [name, setName] = useState(""),
    [description, setDescription] = useState(""),
    [values, setValues] = useState<Record<string, string>>({});
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
  if (!descriptor)
    return (
      <section className="strategy-form">
        <h2>Tạo Strategy</h2>
        <p>Chọn Strategy hệ thống để bắt đầu.</p>
      </section>
    );
  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!name.trim() || Object.keys(issues).length) return;
    await onSubmit({
      name: name.trim(),
      description,
      kind: "SINGLE",
      source: {
        type: "SINGLE",
        strategy: {
          strategyId: descriptor.strategyId,
          version: descriptor.version,
          parameters: effectiveValues
        }
      }
    });
  };
  return (
    <form className="strategy-form" onSubmit={submit}>
      <h2>Tạo Strategy riêng</h2>
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
      {descriptor.parameters.map((field) => (
        <label key={field.name}>
          {field.name}
          <input
            aria-label={field.name}
            value={effectiveValues[field.name]}
            onChange={(e) => setValues((current) => ({ ...current, [field.name]: e.target.value }))}
            aria-invalid={Boolean(issues[field.name])}
          />
          {issues[field.name] && <small role="alert">{issues[field.name]}</small>}
        </label>
      ))}
      <button
        className="button"
        disabled={pending || !name.trim() || Object.keys(issues).length > 0}
      >
        {pending ? "Đang lưu…" : "Lưu Strategy"}
      </button>
    </form>
  );
}
