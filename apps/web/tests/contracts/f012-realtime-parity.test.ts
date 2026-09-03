import { describe, expect, it } from "vitest";
import { candleSchema } from "@/src/features/market/api/schemas";
import { candleUpdatedFixture } from "../fixtures/f012/public-contract";
describe("F-009 realtime parity", () => {
  it("keeps versioned envelope fields and validates Candle payload", () => {
    expect(candleUpdatedFixture.eventVersion).toBe(1);
    expect(candleSchema.parse(candleUpdatedFixture.payload).pair).toBe("BTC/USDT");
  });
});
