import { describe, expect, it, vi } from "vitest";
import type { ApiClient } from "@/src/foundation/http/contracts";
import { createRealtimeClient } from "@/src/foundation/realtime/realtime-client";

class ObservableSocket {
  onopen: (() => void) | null = null;
  onclose: (() => void) | null = null;
  onmessage: ((event: MessageEvent<string>) => void) | null = null;
  sent: string[] = [];
  send(value: string) {
    this.sent.push(value);
  }
  close() {}
}

const setup = () => {
  const api = {
    request: vi.fn(async () => ({ ok: true, data: { ticket: "once" } }))
  } as unknown as ApiClient;
  const socket = new ObservableSocket();
  const client = createRealtimeClient(
    "wss://api.test/ws",
    api,
    vi.fn(),
    () => socket as unknown as WebSocket
  );
  return { client, socket };
};

describe("RealtimeClient observer extension", () => {
  it("keeps existing methods and publishes every transport transition", async () => {
    const { client, socket } = setup();
    const listener = vi.fn();
    client.onStatus(listener);

    await client.connect();
    socket.onopen?.();
    socket.onclose?.();
    client.disconnect();

    expect(listener.mock.calls.map(([metadata]) => metadata.status)).toEqual([
      "connecting",
      "connected",
      "reconnecting",
      "disconnected"
    ]);
    expect(client.status()).toBe("disconnected");
    expect(client.subscribe).toBeTypeOf("function");
    expect(client.unsubscribe).toBeTypeOf("function");
  });

  it("publishes validated generic envelopes to multiple listeners with scoped cleanup", async () => {
    const { client, socket } = setup();
    const first = vi.fn();
    const second = vi.fn();
    const removeFirst = client.onEvent(first);
    client.onEvent(second);
    await client.connect();
    socket.onopen?.();
    const envelope = {
      eventType: "CANDLE_UPDATED",
      eventVersion: 1,
      eventId: "evt-1",
      occurredAt: "2026-09-03T01:00:01Z",
      correlationId: "corr-1",
      subscriptionId: "market-panel-1",
      payload: { pair: "BTC/USDT" }
    };

    socket.onmessage?.({ data: JSON.stringify(envelope) } as MessageEvent<string>);
    removeFirst();
    socket.onmessage?.({ data: JSON.stringify(envelope) } as MessageEvent<string>);
    socket.onmessage?.({ data: "not-json" } as MessageEvent<string>);

    expect(first).toHaveBeenCalledTimes(1);
    expect(second).toHaveBeenCalledTimes(2);
    expect(second).toHaveBeenLastCalledWith(envelope);
    client.disconnect();
  });
});
