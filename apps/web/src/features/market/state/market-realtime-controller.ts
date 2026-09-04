import type {
  RealtimeClient,
  RealtimeEnvelope,
  RealtimeStatus
} from "@/src/foundation/realtime/contracts";
import { candleSchema } from "../api/schemas";
import type { MarketSelection } from "../model/market-selection";
export type ProviderStatus = "CONNECTING" | "CONNECTED" | "RECONNECTING" | "DISCONNECTED";
export function observeMarket(
  realtime: RealtimeClient,
  selection: MarketSelection,
  handlers: {
    onCandle: (panelId: string, candle: unknown, occurredAt: string) => void;
    onTransport: (status: RealtimeStatus) => void;
    onProvider: (status: ProviderStatus) => void;
    onRecovery: () => void;
  }
) {
  const ids = new Map(selection.panels.map((p) => [`market-${p.id}`, p]));
  const confirmed = new Set<string>();
  const buffered = new Map<string, RealtimeEnvelope[]>();
  const providerStates = new Map<string, ProviderStatus>(
    [...ids.keys()].map((id) => [id, "CONNECTING"])
  );
  let reportedProvider: ProviderStatus | undefined;
  const reportProvider = () => {
    const states = [...providerStates.values()];
    const status: ProviderStatus = states.every((value) => value === "CONNECTED")
      ? "CONNECTED"
      : states.every((value) => value === "DISCONNECTED")
        ? "DISCONNECTED"
        : states.some((value) => value === "RECONNECTING" || value === "DISCONNECTED")
          ? "RECONNECTING"
          : "CONNECTING";
    if (status !== reportedProvider) {
      reportedProvider = status;
      handlers.onProvider(status);
    }
  };
  const deliver = (event: RealtimeEnvelope) => {
    const panel = ids.get(event.subscriptionId);
    if (!panel) return;
    const parsed = candleSchema.safeParse(event.payload);
    if (
      parsed.success &&
      parsed.data.pair === selection.pair &&
      parsed.data.timeframe === panel.timeframe
    )
      handlers.onCandle(panel.id, parsed.data, event.occurredAt);
  };
  const removeStatus = realtime.onStatus((metadata) => {
    handlers.onTransport(metadata.status);
    if (metadata.status === "connected") handlers.onRecovery();
  });
  const removeEvent = realtime.onEvent((event: RealtimeEnvelope) => {
    const panel = ids.get(event.subscriptionId);
    if (!panel) return;
    if (event.eventType === "SUBSCRIPTION_CONFIRMED") {
      confirmed.add(event.subscriptionId);
      buffered.get(event.subscriptionId)?.forEach(deliver);
      buffered.delete(event.subscriptionId);
      return;
    }
    if (event.eventType === "CANDLE_UPDATED") {
      if (!confirmed.has(event.subscriptionId)) {
        buffered.set(event.subscriptionId, [
          ...(buffered.get(event.subscriptionId) ?? []).slice(-49),
          event
        ]);
      } else deliver(event);
    }
    if (event.eventType === "MARKET_CONNECTION_STATUS_CHANGED") {
      const status = (event.payload as { status?: string }).status;
      if (["CONNECTING", "CONNECTED", "RECONNECTING", "DISCONNECTED"].includes(status ?? "")) {
        providerStates.set(event.subscriptionId, status as ProviderStatus);
        reportProvider();
      }
    }
    if (event.eventType === "SUBSCRIPTION_ERROR") {
      providerStates.set(event.subscriptionId, "DISCONNECTED");
      reportProvider();
    }
  });
  selection.panels.forEach((panel) =>
    realtime.subscribe({
      subscriptionId: `market-${panel.id}`,
      eventType: "SUBSCRIBE_CANDLES",
      payload: { pair: selection.pair, timeframe: panel.timeframe }
    })
  );
  reportProvider();
  void realtime.connect();
  return () => {
    ids.forEach((_, id) => realtime.unsubscribe(id));
    buffered.clear();
    confirmed.clear();
    providerStates.clear();
    removeEvent();
    removeStatus();
  };
}
