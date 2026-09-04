import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useExperimentRealtime } from "@/src/features/experiments/hooks/useExperimentRealtime";
import { MockRealtimeClient } from "@/src/foundation/testing/mock-realtime-client";

describe("F-013 subscription lifecycle", () => {
  it("releases the old target on target change and unmount", async () => {
    const realtime = new MockRealtimeClient();
    const refresh = vi.fn();
    const candidates = vi.fn();
    const { rerender, unmount } = renderHook(
      ({ id }) => useExperimentRealtime(realtime, id, refresh, candidates),
      { initialProps: { id: "one" } }
    );
    await waitFor(() => expect(realtime.subscriptions.has("experiment-one")).toBe(true));
    rerender({ id: "two" });
    expect(realtime.subscriptions.has("experiment-one")).toBe(false);
    expect(realtime.subscriptions.has("experiment-two")).toBe(true);
    unmount();
    expect(realtime.subscriptions.size).toBe(0);
  });
  it("isolates subscription errors and exposes exhaustion/manual retry", async () => {
    const realtime = new MockRealtimeClient();
    const refresh = vi.fn();
    const candidates = vi.fn();
    const { result } = renderHook(() =>
      useExperimentRealtime(realtime, "one", refresh, candidates)
    );
    act(() => realtime.confirm("experiment-one"));
    await waitFor(() => expect(refresh).toHaveBeenCalledOnce());
    act(() => realtime.fail("other", "WRONG"));
    expect(result.current.subscriptionError).toBeUndefined();
    act(() => realtime.fail("experiment-one", "RATE_LIMIT_EXCEEDED"));
    expect(result.current.subscriptionError).toBe("RATE_LIMIT_EXCEEDED");
    act(() => realtime.emitStatus("disconnected", { exhausted: true }));
    expect(result.current.connection.exhausted).toBe(true);
    await act(() => result.current.reconnect());
    expect(result.current.connection.status).toBe("connected");
  });
});
