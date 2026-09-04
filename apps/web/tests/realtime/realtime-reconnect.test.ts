import { describe, expect, it, vi } from "vitest";
import { createRealtimeClient } from "@/src/foundation/realtime/realtime-client";
import { reconnectDelay } from "@/src/foundation/realtime/reconnect-policy";
import { FakeSocket, ticketApi } from "./helpers";
describe("bounded realtime reconnect", () => {
  it("uses finite exponential backoff with injected jitter", () => {
    expect([0, 1, 2].map((n) => reconnectDelay(n, () => 0))).toEqual([375, 750, 1500]);
    expect(reconnectDelay(0, () => 1)).toBe(625);
  });
  it("exhausts, permits manual reconnect, and clears timers without duplicate loops", async () => {
    vi.useFakeTimers();
    const sockets: FakeSocket[] = [],
      statuses: unknown[] = [];
    const client = createRealtimeClient(
      "wss://api.test/ws",
      ticketApi("one", "two", "manual"),
      vi.fn(),
      (url) => {
        const s = new FakeSocket(url);
        sockets.push(s);
        return s;
      },
      { maxAttempts: 1, random: () => 0, setTimer: setTimeout, clearTimer: clearTimeout }
    );
    client.onStatus((v) => statuses.push(v));
    await client.connect();
    sockets[0]!.open();
    sockets[0]!.closeWith();
    await vi.advanceTimersByTimeAsync(375);
    expect(sockets).toHaveLength(2);
    sockets[1]!.closeWith();
    expect(statuses).toEqual(
      expect.arrayContaining([expect.objectContaining({ status: "disconnected", exhausted: true })])
    );
    await client.connect();
    expect(sockets).toHaveLength(3);
    client.disconnect();
    expect(sockets[2]!.closed).toBe(true);
    expect(vi.getTimerCount()).toBe(0);
    vi.useRealTimers();
  });
});
