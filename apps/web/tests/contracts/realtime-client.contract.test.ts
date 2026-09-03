import { describe, expect, it, vi } from "vitest";
import { createRealtimeClient } from "@/src/foundation/realtime/realtime-client";
import type { ApiClient } from "@/src/foundation/http/contracts";

class Socket {
  onopen: (() => void) | null = null;
  onclose: (() => void) | null = null;
  sent: string[] = [];
  send(value: string) {
    this.sent.push(value);
  }
  close() {}
}
describe("realtime client", () => {
  it("uses a one-time ticket and restores subscriptions", async () => {
    const api = {
      request: vi.fn(async () => ({ ok: true, data: { ticket: "once" } }))
    } as unknown as ApiClient;
    const socket = new Socket();
    const client = createRealtimeClient(
      "wss://api.test/ws",
      api,
      vi.fn(),
      () => socket as unknown as WebSocket
    );
    client.subscribe({
      subscriptionId: "chart-1",
      eventType: "SUBSCRIBE_CANDLES",
      payload: { pair: "BTC/USDT", timeframe: "5m" }
    });
    await client.connect();
    socket.onopen?.();
    expect(api.request).toHaveBeenCalledWith("/api/v1/realtime/ticket", { method: "POST" });
    expect(socket.sent[0]).toContain("SUBSCRIBE_CANDLES");
    client.disconnect();
  });
});
