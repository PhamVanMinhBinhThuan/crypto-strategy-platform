import type { Page } from "@playwright/test";

const candle = {
  items: [
    {
      pair: "BTC/USDT",
      timeframe: "1h",
      openTime: "2026-09-03T00:00:00Z",
      closeTime: "2026-09-03T00:59:59.999Z",
      open: "100000.00",
      high: "101250.00",
      low: "99500.00",
      close: "100750.00",
      volume: "12.50000000",
      closed: true
    }
  ],
  nextCursor: null,
  hasMore: false
};

const strategy = {
  strategyId: "momentum",
  strategyVersionId: "01JSTRATEGYVERSION00000001",
  version: "1.0.0",
  contractVersion: "1",
  displayName: "Momentum cơ bản",
  description: "Chiến lược mẫu",
  category: "MOMENTUM",
  supportedSignals: ["BUY", "SELL", "HOLD"],
  requiredLookback: 20,
  parameters: [],
  constraints: [],
  descriptorFingerprint: "sha256:example"
};

const news = {
  items: [
    {
      newsId: "01JNEWS00000000000000001",
      title: "Thị trường tài sản số cập nhật",
      source: "Example News",
      url: "https://example.com/news/market-update",
      publishedAt: "2026-09-03T01:00:00Z",
      analysisStatus: "ANALYZED",
      relatedAssetIds: ["01JASSET0000000000000001"],
      sentiment: { label: "NEUTRAL", confidence: "0.80", polarityScore: "0.00" }
    }
  ],
  nextCursor: null,
  hasMore: false
};

export async function installF012Adapter(page: Page) {
  await page.setExtraHTTPHeaders({ "x-playwright-auth-bypass": "f012-local-playwright" });
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    let body: unknown;
    if (url.pathname === "/api/v1/candles") {
      body = {
        ...candle,
        items: candle.items.map((item) => ({
          ...item,
          pair: url.searchParams.get("pair") ?? item.pair,
          timeframe: url.searchParams.get("timeframe") ?? item.timeframe
        }))
      };
    } else if (url.pathname === "/api/v1/strategies") {
      body = { items: [strategy], nextCursor: null, hasMore: false };
    } else if (url.pathname === "/api/v1/user-strategies") {
      body = { items: [], nextCursor: null, hasMore: false };
    } else if (url.pathname === "/api/v1/news-items") {
      body = news;
    } else {
      await route.fulfill({ status: 404, contentType: "application/json", body: "{}" });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(body)
    });
  });
}
