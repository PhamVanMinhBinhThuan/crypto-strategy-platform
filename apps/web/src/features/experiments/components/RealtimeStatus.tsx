import type { RealtimeStatusMetadata } from "@/src/foundation/realtime/contracts";
export function RealtimeStatus({
  value,
  error,
  onReconnect
}: {
  value: RealtimeStatusMetadata;
  error?: string;
  onReconnect: () => void;
}) {
  const stale = value.status !== "connected";
  const label = `${value.status.charAt(0).toUpperCase()}${value.status.slice(1)}`;
  return (
    <aside className={`realtime-status ${stale ? "stale" : "fresh"}`} aria-live="polite">
      <span aria-hidden="true">{stale ? "○" : "●"}</span>
      <strong>{label}</strong>
      {value.status === "reconnecting" && <span>Attempt {value.attempt}; snapshot is stale.</span>}
      {value.exhausted && (
        <>
          <span>Automatic retries exhausted.</span>
          <button className="button secondary" onClick={onReconnect}>
            Reconnect
          </button>
        </>
      )}
      {error && <span role="alert">{error}</span>}
    </aside>
  );
}
