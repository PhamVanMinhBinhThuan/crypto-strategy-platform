import { describe, expect, it } from "vitest";
import {
  newsItemSchema,
  newsPageSchema,
  sentimentServiceStatusSchema
} from "@/src/features/news/api/schemas";
import { newsPageFixture } from "../fixtures/f012/public-contract";
import { getSentimentServiceStatus, listNewsItems } from "@/src/features/news/api/news-api";
import type { ApiClient } from "@/src/foundation/http/contracts";

describe("News API Contract", () => {
  it("validates valid news page payload", () => {
    const result = newsPageSchema.safeParse(newsPageFixture);
    expect(result.success).toBe(true);
  });

  it("rejects invalid status", () => {
    const invalidPayload = {
      ...newsPageFixture.items[0],
      analysisStatus: "UNKNOWN"
    };
    const result = newsItemSchema.safeParse(invalidPayload);
    expect(result.success).toBe(false);
  });

  it("rejects invalid sentiment confidence", () => {
    const invalidPayload = {
      ...newsPageFixture.items[0],
      sentiment: { label: "POSITIVE", confidence: "1.5", polarityScore: "0.5" }
    };
    const result = newsItemSchema.safeParse(invalidPayload);
    expect(result.success).toBe(false);
  });

  it("allows null sentiment for PENDING", () => {
    const pendingPayload = {
      ...newsPageFixture.items[0],
      analysisStatus: "PENDING",
      sentiment: null
    };
    const result = newsItemSchema.safeParse(pendingPayload);
    expect(result.success).toBe(true);
  });

  it("rejects inconsistent analysis and sentiment states", () => {
    expect(
      newsItemSchema.safeParse({
        ...newsPageFixture.items[0],
        analysisStatus: "ANALYZED",
        sentiment: null
      }).success
    ).toBe(false);
    expect(
      newsItemSchema.safeParse({ ...newsPageFixture.items[0], analysisStatus: "PENDING" }).success
    ).toBe(false);
  });

  it("uses repeated public analysisStatus parameters and no pair filter", async () => {
    let requested = "";
    const api = {
      request: async (path: string) => {
        requested = path;
        return { ok: true as const, data: newsPageFixture };
      }
    } as ApiClient;
    expect((await listNewsItems(api, { statuses: ["PENDING", "FAILED"] })).ok).toBe(true);
    expect(requested).toContain("analysisStatus=PENDING&analysisStatus=FAILED");
    expect(requested).not.toContain("tradingPairId");
  });

  it("validates and requests the Sentiment service status", async () => {
    let requested = "";
    const payload = {
      status: "DEGRADED",
      message: "Sentiment service is unavailable.",
      checkedAt: "2026-09-08T00:00:00Z"
    };
    expect(sentimentServiceStatusSchema.safeParse(payload).success).toBe(true);
    const api = {
      request: async (path: string) => {
        requested = path;
        return { ok: true as const, data: payload };
      }
    } as ApiClient;

    expect((await getSentimentServiceStatus(api)).ok).toBe(true);
    expect(requested).toBe("/api/v1/news-items/sentiment-status");
  });
});
