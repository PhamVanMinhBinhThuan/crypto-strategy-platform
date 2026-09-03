import { describe, expect, it, vi } from "vitest";
import { listCandles } from "@/src/features/market/api/market-api";
import { candlePageFixture } from "../fixtures/f012/public-contract";
import type { ApiClient } from "@/src/foundation/http/contracts";
describe("Market API", () => {
  it("encodes query and preserves exact decimals", async () => {
    const request = vi.fn(async (path: string) => {
      expect(path).toContain("/candles?");
      return { ok: true as const, data: candlePageFixture };
    });
    const result = await listCandles({ request } as ApiClient, {
      pair: "BTC/USDT",
      timeframe: "1h",
      startTime: "2026-09-02T00:00:00Z",
      endTime: "2026-09-03T00:00:00Z"
    });
    expect(request.mock.calls[0][0]).toContain("pair=BTC%2FUSDT");
    expect(result.ok && result.data.items[0].open).toBe("100000.00");
  });
  it("rejects invalid UTC and decimal payloads", async () => {
    const bad = {
      ...candlePageFixture,
      items: [{ ...candlePageFixture.items[0], open: 2, openTime: "today" }]
    };
    const result = await listCandles(
      { request: async () => ({ ok: true, data: bad }) } as ApiClient,
      {
        pair: "BTC/USDT",
        timeframe: "1h",
        startTime: "2026-09-02T00:00:00Z",
        endTime: "2026-09-03T00:00:00Z"
      }
    );
    expect(result).toMatchObject({ ok: false, error: { code: "INVALID_PUBLIC_RESPONSE" } });
  });
});
