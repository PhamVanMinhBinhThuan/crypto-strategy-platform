import type { RealtimeStatus } from "@/src/foundation/realtime/contracts";
import type { ProviderStatus } from "../state/market-realtime-controller";
export function MarketConnectionStatus({
  transport,
  provider,
  lastDataAt
}: {
  transport: RealtimeStatus;
  provider: ProviderStatus;
  lastDataAt?: string;
}) {
  const live = transport === "connected" && provider === "CONNECTED";
  return (
    <div className={`market-status ${live ? "is-live" : ""}`} role="status" aria-live="polite">
      <strong>{live ? "Live" : "Last saved data"}</strong>
      <span>
        Transport: {transport} · Provider: {provider}
      </span>
      <span>
        {lastDataAt ? (
          <>
            Latest data: <time dateTime={lastDataAt}>{lastDataAt}</time>
          </>
        ) : (
          "No data received yet"
        )}
      </span>
    </div>
  );
}
