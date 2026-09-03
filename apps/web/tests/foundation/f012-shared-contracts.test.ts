import { describe, expect, it, vi } from "vitest";
import { LatestRequest } from "@/src/features/shared/latest-request";
import { canonicalEnum, canonicalEnumList } from "@/src/features/shared/url-state";
import { requestPublic } from "@/src/features/shared/feature-api";
import { candlePageSchema } from "@/src/features/market/api/schemas";
import { candlePageFixture } from "../fixtures/f012/public-contract";
import type { ApiClient } from "@/src/foundation/http/contracts";

describe("F-012 shared contract helpers", () => {
  it("owns only the latest request", () => {
    const requests = new LatestRequest();
    const first = requests.next();
    const second = requests.next();
    expect(first.signal.aborted).toBe(true);
    expect(requests.isLatest(first.generation)).toBe(false);
    expect(requests.isLatest(second.generation)).toBe(true);
  });
  it("canonicalizes bounded public URL values", () => {
    expect(canonicalEnum("bad", ["1h", "4h"] as const, "1h")).toBe("1h");
    expect(canonicalEnumList(["1h", "bad", "1h", "4h"], ["1h", "4h"] as const, ["1h"], 4)).toEqual([
      "1h",
      "4h"
    ]);
  });
  it("validates responses without leaking parser details", async () => {
    let response: unknown = candlePageFixture;
    const request = vi.fn(async () => ({
      ok: true as const,
      data: response,
      correlationId: "corr"
    }));
    const client = { request } as ApiClient;
    expect((await requestPublic(client, candlePageSchema, "/candles")).ok).toBe(true);
    response = {};
    const invalid = await requestPublic(client, candlePageSchema, "/candles");
    expect(invalid).toMatchObject({ ok: false, error: { code: "INVALID_PUBLIC_RESPONSE" } });
  });
});
