import { describe, expect, it } from "vitest";
import { candlePageSchema } from "@/src/features/market/api/schemas";
import { newsPageSchema } from "@/src/features/news/api/schemas";
import { strategyDescriptorSchema } from "@/src/features/strategy/api/schemas";
import {
  candlePageFixture,
  newsPageFixture,
  strategySummaryFixture
} from "../fixtures/f012/public-contract";

describe("F-009 OpenAPI parity", () => {
  it("accepts released Candle and News fixtures", () => {
    expect(candlePageSchema.parse(candlePageFixture).items[0].open).toBe("100000.00");
    expect(newsPageSchema.parse(newsPageFixture).items[0].sentiment?.confidence).toBe("0.80");
    expect(strategyDescriptorSchema.parse(strategySummaryFixture).strategyId).toBe("momentum");
  });
  it("rejects prototype-only News fields", () => {
    expect(
      newsPageSchema.safeParse({
        ...newsPageFixture,
        items: [{ ...newsPageFixture.items[0], summary: "invented" }]
      }).success
    ).toBe(false);
  });
});
