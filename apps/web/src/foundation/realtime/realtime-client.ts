import type { ApiClient } from "../http/contracts";
import type {
  LogicalSubscription,
  RealtimeClient,
  RealtimeEnvelope,
  RealtimeStatus,
  RealtimeStatusMetadata
} from "./contracts";
import { DEFAULT_MAX_RECONNECT_ATTEMPTS, reconnectDelay } from "./reconnect-policy";
import { SubscriptionRegistry } from "./subscription-registry";
import { registerPrivateStateCleanup } from "../auth/logout";

type SocketLike = Pick<WebSocket, "send" | "close"> & {
  onopen: ((event: Event) => void) | null;
  onclose: ((event: CloseEvent) => void) | null;
  onmessage: ((event: MessageEvent) => void) | null;
};
export type RealtimeOptions = Readonly<{
  maxAttempts?: number;
  random?: () => number;
  setTimer?: typeof setTimeout;
  clearTimer?: typeof clearTimeout;
}>;

export function createRealtimeClient(
  url: string,
  api: ApiClient,
  recoverAuthentication: () => Promise<unknown> | unknown,
  socketFactory: (value: string) => SocketLike = (value) => new WebSocket(value),
  options: RealtimeOptions = {}
): RealtimeClient {
  let socket: SocketLike | null = null;
  let state: RealtimeStatus = "disconnected";
  let attempt = 0;
  let timer: ReturnType<typeof setTimeout> | null = null;
  let intentional = false;
  let generation = 0;
  const registry = new SubscriptionRegistry();
  const envelopes = new Set<(value: RealtimeEnvelope) => void>();
  const statuses = new Set<(value: RealtimeStatusMetadata) => void>();
  const maxAttempts = options.maxAttempts ?? DEFAULT_MAX_RECONNECT_ATTEMPTS;
  const schedule = options.setTimer ?? setTimeout;
  const cancel = options.clearTimer ?? clearTimeout;
  const transition = (status: RealtimeStatus, extra: Partial<RealtimeStatusMetadata> = {}) => {
    state = status;
    statuses.forEach((listener) => listener({ status, attempt, ...extra }));
  };
  const serialize = (value: LogicalSubscription, eventType = value.eventType) =>
    JSON.stringify({
      eventType,
      eventVersion: 1,
      eventId: crypto.randomUUID(),
      occurredAt: new Date().toISOString(),
      correlationId: crypto.randomUUID(),
      subscriptionId: value.subscriptionId,
      payload: eventType.startsWith("UNSUBSCRIBE_") ? {} : value.payload
    });
  const sendSubscribe = (value: LogicalSubscription) => socket?.send(serialize(value));

  async function connect(): Promise<void> {
    intentional = false;
    if (socket && (state === "connecting" || state === "connected")) return;
    const current = ++generation;
    transition(attempt ? "reconnecting" : "connecting");
    const ticket = await api.request<{ ticket: string }>("/api/v1/realtime/ticket", {
      method: "POST"
    });
    if (current !== generation || intentional) return;
    if (!ticket.ok) {
      transition("disconnected", { exhausted: true });
      return;
    }
    const created = socketFactory(`${url}?ticket=${encodeURIComponent(ticket.data.ticket)}`);
    socket = created;
    created.onmessage = (event) => {
      try {
        const parsed = JSON.parse(String(event.data)) as RealtimeEnvelope;
        if (parsed && typeof parsed.eventType === "string")
          envelopes.forEach((listener) => listener(parsed));
      } catch {
        /* malformed transport input is not trusted */
      }
    };
    created.onopen = () => {
      if (created !== socket) return;
      attempt = 0;
      transition("connected");
      registry.all().forEach(sendSubscribe);
    };
    created.onclose = (event) => {
      if (created !== socket || intentional) return;
      socket = null;
      void handleClose(event);
    };
  }
  async function handleClose(event: CloseEvent) {
    transition("reconnecting", { closeCode: event.code, closeReason: event.reason });
    if (event.code === 4001 && !(await recoverAuthentication())) {
      disconnect();
      return;
    }
    if (attempt >= maxAttempts) {
      transition("disconnected", {
        exhausted: true,
        closeCode: event.code,
        closeReason: event.reason
      });
      return;
    }
    const delay = reconnectDelay(attempt, options.random);
    attempt += 1;
    timer = schedule(() => {
      timer = null;
      void connect();
    }, delay);
  }
  function disconnect() {
    intentional = true;
    generation += 1;
    if (timer) cancel(timer);
    timer = null;
    const current = socket;
    socket = null;
    current?.close();
    registry.clear();
    envelopes.clear();
    statuses.clear();
    attempt = 0;
    state = "disconnected";
  }
  registerPrivateStateCleanup(disconnect);
  return {
    connect,
    disconnect,
    subscribe(value) {
      registry.set(value);
      if (state === "connected") sendSubscribe(value);
    },
    unsubscribe(id) {
      const value = registry.get(id);
      registry.delete(id);
      if (state === "connected" && value)
        socket?.send(
          serialize(
            value,
            value.eventType.startsWith("SUBSCRIBE_")
              ? value.eventType.replace("SUBSCRIBE_", "UNSUBSCRIBE_")
              : `UNSUBSCRIBE_${value.eventType}`
          )
        );
    },
    status: () => state,
    onEnvelope(listener) {
      envelopes.add(listener);
      return () => envelopes.delete(listener);
    },
    onStatus(listener) {
      statuses.add(listener);
      return () => statuses.delete(listener);
    }
  };
}
