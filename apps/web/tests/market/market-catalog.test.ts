import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import {
  DEFAULT_MARKET_PAIR,
  MARKET_CATALOG_VERSION,
  MARKET_PAIRS,
  MARKET_TIMEFRAMES,
  isMarketPair,
  isMarketTimeframe
} from "@/src/features/market/model/market-catalog";

describe("versioned Market catalog", () => {
  it("contains only canonical released UI values", () => {
    const websocketDocs = readFileSync("../../docs/api/websocket-events.md", "utf8");
    expect(MARKET_CATALOG_VERSION).toBe(1);
    expect(MARKET_PAIRS).toEqual(["BTC/USDT"]);
    expect(MARKET_TIMEFRAMES).toEqual(["5m", "15m", "1h", "4h"]);
    expect(websocketDocs).toContain(
      "`pair` | string | Có | Canonical pair dạng `BASE/QUOTE`, ví dụ `BTC/USDT`"
    );
    expect(websocketDocs).toContain("MVP UI dùng `5m`, `15m`, `1h`, `4h`");
  });

  it("rejects arbitrary provider symbols and unsupported timeframes", () => {
    expect(isMarketPair(DEFAULT_MARKET_PAIR)).toBe(true);
    expect(isMarketPair("BTCUSDT")).toBe(false);
    expect(isMarketTimeframe("1h")).toBe(true);
    expect(isMarketTimeframe("3m")).toBe(false);
  });
});
