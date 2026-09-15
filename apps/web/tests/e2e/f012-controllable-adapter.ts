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

const systemStrategies = [
  strategy,
  ...["ma-crossover", "bollinger-bands", "support-resistance"].map((strategyId, index) => ({
    ...strategy,
    strategyId,
    strategyVersionId: `01JTECHNICALVERSION0000000${index + 2}`,
    displayName: ["Moving Average Crossover", "Bollinger Bands", "Support / Resistance"][index],
    descriptorFingerprint: `sha256:technical-${index}`
  })),
  {
    ...strategy,
    strategyId: "sentiment-polarity",
    strategyVersionId: "01JSENTIMENTVERSION0000001",
    displayName: "Sentiment Polarity",
    description: "Aggregates a frozen News sentiment snapshot into BUY, SELL, or HOLD.",
    category: "SENTIMENT",
    requiredLookback: 1,
    parameters: [
      {
        name: "lookbackHours",
        type: "INTEGER",
        required: true,
        defaultValue: "24",
        minimum: "1",
        maximum: "168",
        allowedValues: [],
        description: "News lookback ending at the evaluation candle.",
        searchRangeHint: { minimum: "6", maximum: "48", step: "6" }
      },
      {
        name: "minimumArticles",
        type: "INTEGER",
        required: true,
        defaultValue: "2",
        minimum: "1",
        maximum: "100",
        allowedValues: [],
        description: "Minimum eligible analyzed articles.",
        searchRangeHint: { minimum: "1", maximum: "5", step: "1" }
      },
      {
        name: "buyThreshold",
        type: "DECIMAL",
        required: true,
        defaultValue: "0.25",
        minimum: "-1",
        maximum: "1",
        allowedValues: [],
        description: "Aggregate score at or above this value emits BUY.",
        searchRangeHint: { minimum: "0.1", maximum: "0.5", step: "0.1" }
      },
      {
        name: "sellThreshold",
        type: "DECIMAL",
        required: true,
        defaultValue: "-0.25",
        minimum: "-1",
        maximum: "1",
        allowedValues: [],
        description: "Aggregate score at or below this value emits SELL.",
        searchRangeHint: { minimum: "-0.5", maximum: "-0.1", step: "0.1" }
      }
    ],
    constraints: [{ lowerParameter: "sellThreshold", upperParameter: "buyThreshold" }],
    descriptorFingerprint: "strategy-descriptor-v1:sentiment-polarity:1.0.0"
  }
];

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
      body = { items: systemStrategies, nextCursor: null, hasMore: false };
    } else if (url.pathname === "/api/v1/user-strategies") {
      body = { items: [], nextCursor: null, hasMore: false };
    } else if (url.pathname === "/api/v1/news-items") {
      body = news;
    } else if (url.pathname === "/api/v1/news-items/sentiment-status") {
      body = {
        status: "AVAILABLE",
        message: "Sentiment analysis is available.",
        checkedAt: "2026-09-03T01:00:00Z"
      };
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
