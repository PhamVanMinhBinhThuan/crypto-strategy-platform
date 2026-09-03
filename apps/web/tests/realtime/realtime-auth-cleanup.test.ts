import { describe, expect, it, vi } from "vitest";
import { createRealtimeClient } from "@/src/foundation/realtime/realtime-client";
import { clearPrivateClientState } from "@/src/foundation/auth/logout";
import { FakeSocket, ticketApi } from "./helpers";
describe("realtime authentication cleanup", () => {
  it("close 4001 recovers, obtains a fresh ticket, and resubscribes", async () => {
    const sockets: FakeSocket[] = [];
    const recover = vi.fn(async () => ({ userId: "u" }));
    const api = ticketApi("first", "fresh");
    const client = createRealtimeClient(
      "wss://api.test/ws",
      api,
      recover,
      (url) => {
        const s = new FakeSocket(url);
        sockets.push(s);
        return s;
      },
      {
        setTimer: ((fn: () => void) => {
          fn();
          return 1;
        }) as typeof setTimeout
      }
    );
    client.subscribe({
      subscriptionId: "s",
      eventType: "SUBSCRIBE_EXPERIMENT",
      payload: { experimentId: "e" }
    });
    await client.connect();
    sockets[0]!.open();
    sockets[0]!.closeWith(4001);
    await vi.waitFor(() => expect(sockets).toHaveLength(2));
    expect(recover).toHaveBeenCalledOnce();
    expect(api.request).toHaveBeenCalledTimes(2);
    expect(sockets[1]!.url).toContain("fresh");
    sockets[1]!.open();
    expect(sockets[1]!.sent[0]).toContain("SUBSCRIBE_EXPERIMENT");
  });
  it("failed recovery and logout clear socket, listeners and logical subscriptions", async () => {
    const sockets: FakeSocket[] = [];
    const client = createRealtimeClient(
      "wss://api.test/ws",
      ticketApi("first"),
      vi.fn(async () => null),
      (url) => {
        const s = new FakeSocket(url);
        sockets.push(s);
        return s;
      }
    );
    const listener = vi.fn();
    client.onEnvelope(listener);
    client.subscribe({
      subscriptionId: "s",
      eventType: "SUBSCRIBE_EXPERIMENT",
      payload: { experimentId: "e" }
    });
    await client.connect();
    sockets[0]!.open();
    sockets[0]!.closeWith(4001);
    await vi.waitFor(() => expect(client.status()).toBe("disconnected"));
    await clearPrivateClientState();
    sockets[0]!.message({ eventType: "X" });
    expect(listener).not.toHaveBeenCalled();
    await client.connect();
    sockets[1]!.open();
    expect(sockets[1]!.sent).toEqual([]);
  });
});
