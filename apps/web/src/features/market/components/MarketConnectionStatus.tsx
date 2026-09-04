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
      <strong>{live ? "Live" : "Dữ liệu lưu gần nhất"}</strong>
      <span>
        Transport: {transport} · Provider: {provider}
      </span>
      <span>
        {lastDataAt ? (
          <>
            Dữ liệu gần nhất: <time dateTime={lastDataAt}>{lastDataAt}</time>
          </>
        ) : (
          "Chưa nhận được dữ liệu"
        )}
      </span>
    </div>
  );
}
