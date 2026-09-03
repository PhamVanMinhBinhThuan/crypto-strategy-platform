import type { ApiClient } from "../http/contracts";
import type {
  LogicalSubscription,
  RealtimeClient,
  RealtimeEnvelope,
  RealtimeStatus
} from "./contracts";
import { reconnectDelay } from "./reconnect-policy";
import { SubscriptionRegistry } from "./subscription-registry";
import { registerPrivateStateCleanup } from "../auth/logout";
export function createRealtimeClient(
  url: string,
  api: ApiClient,
  onRecovery: () => void,
  socketFactory = (value: string) => new WebSocket(value)
): RealtimeClient {
  let socket: WebSocket | null = null,
    state: RealtimeStatus = "disconnected",
    attempt = 0,
    timer: ReturnType<typeof setTimeout> | null = null,
    manuallyClosed = false;
  const registry = new SubscriptionRegistry();
  const eventListeners = new Set<(event: RealtimeEnvelope) => void>();
  const statusListeners = new Set<(status: RealtimeStatus) => void>();
  function setStatus(next: RealtimeStatus) {
    state = next;
    statusListeners.forEach((listener) => listener(next));
  }
  function isEnvelope(value: unknown): value is RealtimeEnvelope {
    if (!value || typeof value !== "object") return false;
    const candidate = value as Record<string, unknown>;
    return (
      typeof candidate.eventType === "string" &&
      typeof candidate.eventVersion === "number" &&
      typeof candidate.eventId === "string" &&
      typeof candidate.occurredAt === "string" &&
      typeof candidate.correlationId === "string" &&
      typeof candidate.subscriptionId === "string" &&
      "payload" in candidate
    );
  }
  async function connect() {
    manuallyClosed = false;
    setStatus(attempt ? "reconnecting" : "connecting");
    const ticket = await api.request<{ ticket: string }>("/api/v1/realtime/ticket", {
      method: "POST"
    });
    if (!ticket.ok) {
      setStatus("disconnected");
      return;
    }
    socket = socketFactory(`${url}?ticket=${encodeURIComponent(ticket.data.ticket)}`);
    socket.onopen = () => {
      setStatus("connected");
      attempt = 0;
      registry.all().forEach(sendSubscribe);
      if (registry.all().length) onRecovery();
    };
    socket.onmessage = (message) => {
      if (typeof message.data !== "string") return;
      try {
        const envelope: unknown = JSON.parse(message.data);
        if (isEnvelope(envelope)) eventListeners.forEach((listener) => listener(envelope));
      } catch {
        // Invalid transport messages are ignored; feature adapters validate typed payloads.
      }
    };
    socket.onclose = () => {
      if (manuallyClosed) return;
      setStatus("reconnecting");
      timer = setTimeout(() => {
        attempt++;
        void connect();
      }, reconnectDelay(attempt));
    };
  }
  function sendSubscribe(v: LogicalSubscription) {
    socket?.send(
      JSON.stringify({
        eventType: v.eventType,
        eventVersion: 1,
        eventId: crypto.randomUUID(),
        occurredAt: new Date().toISOString(),
        correlationId: crypto.randomUUID(),
        subscriptionId: v.subscriptionId,
        payload: v.payload
      })
    );
  }
  function disconnect() {
    manuallyClosed = true;
    if (timer) clearTimeout(timer);
    socket?.close();
    socket = null;
    registry.clear();
    setStatus("disconnected");
    eventListeners.clear();
    statusListeners.clear();
  }
  registerPrivateStateCleanup(disconnect);
  return {
    connect,
    disconnect,
    subscribe(v) {
      registry.set(v);
      if (state === "connected") sendSubscribe(v);
    },
    unsubscribe(id) {
      const sub = registry.get(id);
      registry.delete(id);
      if (state === "connected" && sub) {
        const unsubscribeType = sub.eventType.startsWith("SUBSCRIBE_")
          ? sub.eventType.replace("SUBSCRIBE_", "UNSUBSCRIBE_")
          : "UNSUBSCRIBE_" + sub.eventType;

        socket?.send(
          JSON.stringify({
            eventType: unsubscribeType,
            eventVersion: 1,
            eventId: crypto.randomUUID(),
            occurredAt: new Date().toISOString(),
            correlationId: crypto.randomUUID(),
            subscriptionId: id,
            payload: {}
          })
        );
      }
    },
    status: () => state,
    onEvent(listener) {
      eventListeners.add(listener);
      return () => eventListeners.delete(listener);
    },
    onStatus(listener) {
      statusListeners.add(listener);
      return () => statusListeners.delete(listener);
    }
  };
}
