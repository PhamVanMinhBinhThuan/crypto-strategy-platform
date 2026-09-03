import type { ApiClient } from "../http/contracts";
import type { LogicalSubscription, RealtimeClient, RealtimeStatus } from "./contracts";
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
    timer: ReturnType<typeof setTimeout> | null = null;
  const registry = new SubscriptionRegistry();
  async function connect() {
    state = attempt ? "reconnecting" : "connecting";
    const ticket = await api.request<{ ticket: string }>("/api/v1/realtime/ticket", {
      method: "POST"
    });
    if (!ticket.ok) {
      state = "disconnected";
      return;
    }
    socket = socketFactory(`${url}?ticket=${encodeURIComponent(ticket.data.ticket)}`);
    socket.onopen = () => {
      state = "connected";
      attempt = 0;
      registry.all().forEach(sendSubscribe);
      if (registry.all().length) onRecovery();
    };
    socket.onclose = () => {
      state = "reconnecting";
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
    if (timer) clearTimeout(timer);
    socket?.close();
    socket = null;
    registry.clear();
    state = "disconnected";
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
    status: () => state
  };
}
