"use client";
import { useState } from "react";

export function TechnicalDetails({
  values,
  jsonValues = [],
  summaryLabel = "Technical details"
}: {
  values: ReadonlyArray<readonly [label: string, value: string | null | undefined]>;
  jsonValues?: ReadonlyArray<readonly [label: string, value: unknown]>;
  summaryLabel?: string;
}) {
  const [copied, setCopied] = useState<string>();
  const visible = values.filter((entry): entry is readonly [string, string] => !!entry[1]);
  if (!visible.length && !jsonValues.length) return null;
  const copy = (label: string, value: string) => {
    void globalThis.navigator?.clipboard?.writeText(value);
    setCopied(label);
  };
  return (
    <details className="technical-details">
      <summary>{summaryLabel}</summary>
      <dl>
        {visible.map(([label, value]) => (
          <div key={label}>
            <dt>{label}</dt>
            <dd>
              <span className="mono break-value">{value}</span>
              <button
                type="button"
                className="copy-button"
                onClick={() => copy(label, value)}
                aria-label={`Copy ${label}`}
              >
                {copied === label ? "Copied" : "Copy"}
              </button>
            </dd>
          </div>
        ))}
      </dl>
      {jsonValues.map(([label, value]) => {
        const serialized = JSON.stringify(value, null, 2) ?? "null";
        return (
          <section className="technical-json" key={label}>
            <div>
              <h4>{label}</h4>
              <button type="button" className="copy-button"
                onClick={() => copy(label, serialized)} aria-label={`Copy ${label}`}>
                {copied === label ? "Copied" : "Copy JSON"}
              </button>
            </div>
            <pre>{serialized}</pre>
          </section>
        );
      })}
      <span className="sr-only" aria-live="polite">
        {copied ? `${copied} copied to clipboard` : ""}
      </span>
    </details>
  );
}
