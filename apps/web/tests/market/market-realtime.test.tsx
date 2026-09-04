import { describe, expect, it, vi } from "vitest";
import { MockRealtimeClient } from "@/src/foundation/testing/mock-realtime-client";
import { observeMarket } from "@/src/features/market/state/market-realtime-controller";
import { candleUpdatedFixture } from "../fixtures/f012/public-contract";
describe("Market realtime", () => {
  it("subscribes per panel, filters foreign events and cleans up", async () => {
    const realtime = new MockRealtimeClient(),
      onCandle = vi.fn();
    const cleanup = observeMarket(
      realtime,
      { pair: "BTC/USDT", panels: [{ id: "panel-1", timeframe: "1h" }] },
      { onCandle, onTransport: vi.fn(), onProvider: vi.fn(), onRecovery: vi.fn() }
    );
    await Promise.resolve();
    expect(realtime.subscriptions.size).toBe(1);
    realtime.emit({ ...candleUpdatedFixture, subscriptionId: "market-panel-1" });
    expect(onCandle).not.toHaveBeenCalled();
    realtime.emit({
      ...candleUpdatedFixture,
      eventType: "SUBSCRIPTION_CONFIRMED",
      subscriptionId: "market-panel-1",
      payload: { status: "ACTIVE" }
    });
    expect(onCandle).toHaveBeenCalledOnce();
    realtime.emit({ ...candleUpdatedFixture, subscriptionId: "other" });
    expect(onCandle).toHaveBeenCalledOnce();
    cleanup();
    expect(realtime.subscriptions.size).toBe(0);
  });

  it("aggregates connection state across every market panel", async () => {
    const realtime = new MockRealtimeClient(),
      onProvider = vi.fn();
    observeMarket(
      realtime,
      {
        pair: "BTC/USDT",
        panels: [
          { id: "panel-1", timeframe: "5m" },
          { id: "panel-2", timeframe: "15m" }
        ]
      },
      { onCandle: vi.fn(), onTransport: vi.fn(), onProvider, onRecovery: vi.fn() }
    );
    await Promise.resolve();

    const status = (subscriptionId: string, value: string) =>
      realtime.emit({
        ...candleUpdatedFixture,
        eventType: "MARKET_CONNECTION_STATUS_CHANGED",
        subscriptionId,
        payload: { status: value }
      });

    expect(onProvider).toHaveBeenLastCalledWith("CONNECTING");
    status("market-panel-1", "CONNECTED");
    expect(onProvider).toHaveBeenLastCalledWith("CONNECTING");
    status("market-panel-2", "CONNECTED");
    expect(onProvider).toHaveBeenLastCalledWith("CONNECTED");
    status("market-panel-1", "RECONNECTING");
    expect(onProvider).toHaveBeenLastCalledWith("RECONNECTING");
    status("market-panel-1", "CONNECTED");
    expect(onProvider).toHaveBeenLastCalledWith("CONNECTED");
    status("market-panel-2", "DISCONNECTED");
    expect(onProvider).toHaveBeenLastCalledWith("RECONNECTING");
    status("market-panel-1", "DISCONNECTED");
    expect(onProvider).toHaveBeenLastCalledWith("DISCONNECTED");
  });
});
