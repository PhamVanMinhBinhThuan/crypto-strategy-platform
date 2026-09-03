export const candlePageFixture = {
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
} as const;

export const strategySummaryFixture = {
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
} as const;

export const newsPageFixture = {
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
} as const;

export const candleUpdatedFixture = {
  eventType: "CANDLE_UPDATED",
  eventVersion: 1,
  eventId: "01JEVENT0000000000000001",
  occurredAt: "2026-09-03T01:00:01Z",
  correlationId: "01JCORRELATION00000000001",
  subscriptionId: "market-panel-1",
  payload: candlePageFixture.items[0]
} as const;
