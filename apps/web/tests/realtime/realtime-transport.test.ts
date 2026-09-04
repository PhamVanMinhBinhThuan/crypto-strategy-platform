import { describe, expect, it, vi } from "vitest";
import { createRealtimeClient } from "@/src/foundation/realtime/realtime-client";
import { FakeSocket, ticketApi } from "./helpers";
describe("F-011 realtime transport", () => {
  it("opens exactly one socket, dispatches envelopes/status/close metadata and resubscribes", async () => {
    const sockets: FakeSocket[] = [];
    const statuses: unknown[] = [],
      events: unknown[] = [];
    const client = createRealtimeClient(
      "wss://api.test/ws",
      ticketApi("one", "two"),
      vi.fn(),
      (url) => {
        const s = new FakeSocket(url);
        sockets.push(s);
        return s;
      },
      {
        setTimer: ((fn: () => void) => {
          fn();
          return 1;
        }) as typeof setTimeout,
        clearTimer: vi.fn()
      }
    );
    client.onStatus((v) => statuses.push(v));
    client.onEnvelope((v) => events.push(v));
    client.subscribe({
      subscriptionId: "experiment-1",
      eventType: "SUBSCRIBE_EXPERIMENT",
      payload: { experimentId: "experiment-1" }
    });
    await client.connect();
    await client.connect();
    expect(sockets).toHaveLength(1);
    sockets[0]!.open();
    expect(sockets[0]!.sent[0]).toContain("SUBSCRIBE_EXPERIMENT");
    sockets[0]!.message({
      eventType: "EXPERIMENT_PROGRESS_UPDATED",
      eventVersion: 1,
      eventId: "e",
      occurredAt: "2026-09-03T00:00:00Z",
      correlationId: "c",
      subscriptionId: "experiment-1",
      payload: { experimentId: "experiment-1" }
    });
    expect(events).toHaveLength(1);
    sockets[0]!.closeWith(1006, "lost");
    await vi.waitFor(() => expect(sockets).toHaveLength(2));
    expect(sockets[1]!.url).toContain("ticket=two");
    sockets[1]!.open();
    expect(sockets[1]!.sent[0]).toContain("SUBSCRIBE_EXPERIMENT");
    expect(statuses).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ status: "reconnecting", closeCode: 1006, closeReason: "lost" })
      ])
    );
  });
});
