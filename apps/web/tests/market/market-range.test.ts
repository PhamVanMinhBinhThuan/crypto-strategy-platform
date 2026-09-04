import { describe, expect, it } from "vitest";
import { marketRangeEndingAt } from "@/src/features/market/model/market-range";

describe("marketRangeEndingAt", () => {
  it.each([
    ["5m", "2026-09-04T01:35:00.000Z"],
    ["15m", "2026-09-04T01:30:00.000Z"],
    ["1h", "2026-09-04T01:00:00.000Z"],
    ["4h", "2026-09-04T00:00:00.000Z"]
  ] as const)("aligns the %s range to the API candle boundary", (timeframe, expectedEnd) => {
    const range = marketRangeEndingAt(new Date("2026-09-04T01:37:42.987Z"), timeframe);

    expect(range.endTime).toBe(expectedEnd);
    expect(Date.parse(range.endTime) - Date.parse(range.startTime)).toBe(24 * 60 * 60 * 1000);
  });
});
