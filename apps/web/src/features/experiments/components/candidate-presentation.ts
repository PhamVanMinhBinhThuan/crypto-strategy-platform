import type { CandidatePipelineItem } from "../types/experiment";

const strategyNames: Readonly<Record<string, string>> = {
  "bollinger-bands": "Bollinger Bands",
  "ma-crossover": "Moving Average Crossover",
  "rsi-threshold": "RSI Threshold",
  rsi: "RSI Threshold",
  "support-resistance": "Support / Resistance"
};

const parameterNames: Readonly<Record<string, string>> = {
  period: "Period",
  standardDeviation: "Standard deviation multiplier",
  fastPeriod: "Fast period",
  slowPeriod: "Slow period",
  buyThreshold: "Buy threshold",
  sellThreshold: "Sell threshold",
  lookback: "Lookback period",
  tolerance: "Price tolerance",
  tolerancePercent: "Price tolerance (%)",
  ruleMode: "Rule mode"
};

const readableValue = (value: unknown) => {
  const raw = value && typeof value === "object" && "value" in value
    ? (value as { value: unknown }).value
    : value;
  if (typeof raw === "string" && /^[A-Z][A-Z0-9_]*$/.test(raw)) {
    return raw.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
  }
  return String(raw);
};

export const candidateComponents = (definition: Readonly<Record<string, unknown>>) =>
  (Array.isArray(definition.components) ? definition.components : [definition]).filter(
    (component): component is Record<string, unknown> =>
      !!component && typeof component === "object"
  );

export const strategyName = (definition: Readonly<Record<string, unknown>>) => {
  const result = candidateComponents(definition)
    .map((component) => {
      const id = String(component.strategyId ?? "Strategy");
      return strategyNames[id] ?? id;
    })
    .join(" + ");
  return result || "Strategy";
};

export const parameterSummary = (definition: Readonly<Record<string, unknown>>) => {
  const components = candidateComponents(definition);
  return components
    .map((component) => {
      const values = component.parameters && typeof component.parameters === "object"
        ? component.parameters as Record<string, unknown>
        : {};
      const parameters = Object.entries(values)
        .map(([name, value]) => {
          const label = parameterNames[name]
            ?? name.replace(/([a-z])([A-Z])/g, "$1 $2").replace(/^./, (letter) => letter.toUpperCase());
          return `${label} ${readableValue(value)}`;
        })
        .join(" · ");
      if (components.length === 1) return parameters;
      const id = String(component.strategyId ?? "Strategy");
      return `${strategyNames[id] ?? id}${parameters ? ` — ${parameters}` : ""}`;
    })
    .filter(Boolean)
    .join(" · ");
};

const statusLabels: Readonly<Record<string, string>> = {
  NOT_IN_TOP_K: "Not in Top-K",
  NOT_STARTED: "Not started",
  RETRY_SCHEDULED: "Retry scheduled"
};

export const statusLabel = (value: string) => statusLabels[value]
  ?? value.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());

export const formatScore = (value: string | null) => {
  if (value === null) return "—";
  const number = Number(value);
  return Number.isFinite(number) ? number.toFixed(4) : value;
};

export const formatPercent = (value: string | null, showPositiveSign = false) => {
  if (value === null) return "—";
  const number = Number(value);
  if (!Number.isFinite(number)) return value;
  const percentage = number * 100;
  return `${showPositiveSign && percentage > 0 ? "+" : ""}${percentage.toFixed(2)}%`;
};

export const formatDateTime = (value: string | null) => {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.valueOf())) return value;
  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "UTC"
  }).format(date) + " UTC";
};

export const failurePresentation = (item: Pick<CandidatePipelineItem, "backtest">) => {
  const code = item.backtest.failure?.code ?? "UNKNOWN_FAILURE";
  const rawMessage = item.backtest.failure?.message ?? "";
  const message = `${code} ${rawMessage}`.toLowerCase();
  if (message.includes("lookback") || message.includes("historical candles")) {
    return { title: "Insufficient historical candles", code, rawMessage };
  }
  if (message.includes("jdbc") || message.includes("connection")) {
    return { title: "Database connection unavailable", code, rawMessage };
  }
  if (message.includes("parameter") || message.includes("validation")) {
    return { title: "Invalid strategy parameters", code, rawMessage };
  }
  if (message.includes("timeout") || message.includes("timed out") || message.includes("stale")) {
    return { title: "Worker timeout or retry exhausted", code, rawMessage };
  }
  return { title: "The candidate could not be processed", code, rawMessage };
};
