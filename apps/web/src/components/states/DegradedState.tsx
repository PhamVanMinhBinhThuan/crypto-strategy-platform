export type AvailabilityState = "ready" | "degraded" | "stale" | "recovering";

const presentation: Record<AvailabilityState, { label: string; symbol: string }> = {
  ready: { label: "Ready", symbol: "✓" },
  degraded: { label: "Limited availability", symbol: "!" },
  stale: { label: "Stale snapshot", symbol: "↻" },
  recovering: { label: "Recovering", symbol: "…" }
};

export function DegradedState({
  state = "degraded",
  message
}: {
  state?: AvailabilityState;
  message: string;
}) {
  const value = presentation[state];
  return (
    <div
      className={`status availability-state availability-${state}`}
      data-availability={state}
      role="status"
      aria-live={state === "ready" ? "polite" : "assertive"}
    >
      <span className="availability-symbol" aria-hidden="true">
        {value.symbol}
      </span>
      <span>
        <strong>{value.label}</strong>
        <span>{message}</span>
      </span>
    </div>
  );
}
