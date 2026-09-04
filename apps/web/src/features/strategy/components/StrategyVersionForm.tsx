"use client";
import { useMemo, useState } from "react";
import type { StrategyDescriptor, UserStrategy } from "../model/strategy";
import type { StrategySelectionDraft, StrategySourceDraft } from "../model/strategy-draft";
import { serializeStrategyParameters } from "../state/strategy-parameter-serializer";
import { validateStrategyParameters } from "../state/strategy-parameter-validator";

const selectionKey = (strategyId: string, version: string) => `${strategyId}@${version}`;
const stringValues = (values: Readonly<Record<string, string>>) => ({ ...values });

export function StrategyVersionForm({
  owned,
  systemStrategies,
  pending,
  onSubmit,
  onCancel
}: {
  owned: UserStrategy;
  systemStrategies: readonly StrategyDescriptor[];
  pending: boolean;
  onSubmit: (source: StrategySourceDraft) => Promise<void>;
  onCancel: () => void;
}) {
  const current = owned.latestVersion.source;
  const singleSource = current.type === "SINGLE" ? current : undefined;
  const [values, setValues] = useState<Record<string, string>>(() =>
    singleSource ? stringValues(singleSource.strategy.parameters) : {}
  );
  const [componentKeys, setComponentKeys] = useState<string[]>(() =>
    current.type === "COMPOSITE"
      ? current.components.map((item) => selectionKey(item.strategyId, item.version))
      : []
  );
  const descriptor = singleSource
    ? systemStrategies.find(
        (item) =>
          item.strategyId === singleSource.strategy.strategyId &&
          item.version === singleSource.strategy.version
      )
    : undefined;
  const issues = useMemo(
    () => (descriptor ? validateStrategyParameters(descriptor, values) : {}),
    [descriptor, values]
  );
  const selection = (item: StrategyDescriptor): StrategySelectionDraft => {
    const previous =
      current.type === "COMPOSITE"
        ? current.components.find(
            (component) =>
              component.strategyId === item.strategyId && component.version === item.version
          )
        : undefined;
    const sourceValues =
      previous?.parameters ??
      Object.fromEntries(item.parameters.map((field) => [field.name, field.defaultValue ?? ""]));
    return {
      strategyId: item.strategyId,
      version: item.version,
      parameters: serializeStrategyParameters(item, sourceValues)
    };
  };
  const nextSource: StrategySourceDraft | undefined = descriptor
    ? {
        type: "SINGLE",
        strategy: {
          strategyId: descriptor.strategyId,
          version: descriptor.version,
          parameters: serializeStrategyParameters(descriptor, values)
        }
      }
    : current.type === "COMPOSITE"
      ? {
          type: "COMPOSITE",
          policyId: current.policyId,
          policyVersion: current.policyVersion,
          policyParameters: current.policyParameters,
          components: systemStrategies
            .filter((item) => componentKeys.includes(selectionKey(item.strategyId, item.version)))
            .map(selection)
        }
      : undefined;
  const originalSource: StrategySourceDraft | undefined = descriptor
    ? {
        type: "SINGLE",
        strategy: {
          strategyId: descriptor.strategyId,
          version: descriptor.version,
          parameters: serializeStrategyParameters(descriptor, singleSource!.strategy.parameters)
        }
      }
    : current.type === "COMPOSITE"
      ? {
          type: "COMPOSITE",
          policyId: current.policyId,
          policyVersion: current.policyVersion,
          policyParameters: current.policyParameters,
          components: systemStrategies
            .filter((item) =>
              current.components.some(
                (component) =>
                  component.strategyId === item.strategyId && component.version === item.version
              )
            )
            .map(selection)
        }
      : undefined;
  const changed = JSON.stringify(nextSource) !== JSON.stringify(originalSource);
  const invalid =
    !nextSource ||
    Object.keys(issues).length > 0 ||
    (nextSource.type === "COMPOSITE" && nextSource.components.length < 2) ||
    !changed;

  if (!nextSource)
    return (
      <section className="strategy-form">
        <h2>Không thể tạo version mới</h2>
        <p>Strategy hệ thống gốc không còn khả dụng trong catalog.</p>
        <button type="button" onClick={onCancel}>
          Đóng
        </button>
      </section>
    );

  return (
    <form
      className="strategy-form"
      onSubmit={(event) => {
        event.preventDefault();
        if (!invalid) void onSubmit(nextSource);
      }}
    >
      <h2>Tạo version {owned.latestVersion.versionNo + 1}</h2>
      <p>Thay đổi cấu hình bên dưới. Version hiện tại vẫn được giữ nguyên.</p>
      {descriptor &&
        descriptor.parameters.map((field) => (
          <label key={field.name}>
            {field.name}
            {field.type === "ENUM" || field.type === "BOOLEAN" ? (
              <select
                aria-label={field.name}
                value={values[field.name] ?? ""}
                onChange={(event) =>
                  setValues((currentValues) => ({
                    ...currentValues,
                    [field.name]: event.target.value
                  }))
                }
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
                value={values[field.name] ?? ""}
                onChange={(event) =>
                  setValues((currentValues) => ({
                    ...currentValues,
                    [field.name]: event.target.value
                  }))
                }
                aria-invalid={Boolean(issues[field.name])}
              />
            )}
            {issues[field.name] && <small role="alert">{issues[field.name]}</small>}
          </label>
        ))}
      {current.type === "COMPOSITE" && (
        <fieldset>
          <legend>Thành phần (ít nhất 2)</legend>
          {systemStrategies.map((item) => {
            const key = selectionKey(item.strategyId, item.version);
            return (
              <label key={item.strategyVersionId}>
                <input
                  type="checkbox"
                  checked={componentKeys.includes(key)}
                  onChange={(event) =>
                    setComponentKeys((existing) =>
                      event.target.checked
                        ? [...existing, key]
                        : existing.filter((value) => value !== key)
                    )
                  }
                />{" "}
                {item.displayName} · v{item.version}
              </label>
            );
          })}
        </fieldset>
      )}
      {!changed && <small role="note">Hãy thay đổi ít nhất một tham số hoặc thành phần.</small>}
      <div className="strategy-actions">
        <button type="button" disabled={pending} onClick={onCancel}>
          Hủy
        </button>
        <button className="button" disabled={pending || invalid}>
          {pending ? "Đang lưu…" : "Lưu version mới"}
        </button>
      </div>
    </form>
  );
}
