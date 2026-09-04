import { expect, test, type Locator, type Page, type Route } from "@playwright/test";
import { normalBacktestResult } from "../../src/features/backtests/fixtures/backtest-result-fixtures";

const candlePage = (pair: string, timeframe: string) => ({
  items: [
    {
      pair,
      timeframe,
      openTime: "2026-09-04T00:00:00Z",
      closeTime: "2026-09-04T00:04:59.999Z",
      open: "100000.00",
      high: "101000.00",
      low: "99500.00",
      close: "100500.00",
      volume: "12.50000000",
      closed: true
    }
  ],
  nextCursor: null,
  hasMore: false
});

const strategyPage = {
  items: [
    {
      strategyId: "ma-crossover",
      strategyVersionId: "01J00000000000000000000000",
      version: "1.0.0",
      contractVersion: "strategy-contract-v1",
      displayName: "Moving Average Crossover",
      description: "Moving-average strategy for accessibility checks",
      category: "TREND",
      supportedSignals: ["BUY", "SELL", "HOLD"],
      requiredLookback: 25,
      parameters: [
        {
          name: "fastPeriod",
          type: "INTEGER",
          required: true,
          defaultValue: "5",
          minimum: "2",
          maximum: "100",
          allowedValues: [],
          description: "Fast period"
        },
        {
          name: "slowPeriod",
          type: "INTEGER",
          required: true,
          defaultValue: "25",
          minimum: "3",
          maximum: "500",
          allowedValues: [],
          description: "Slow period"
        }
      ],
      constraints: [],
      descriptorFingerprint: "strategy-descriptor-v1:ma-crossover:1.0.0"
    }
  ],
  nextCursor: null,
  hasMore: false
};

const analyzedNews = {
  newsId: "01J00000000000000000000401",
  title: "Bitcoin market structure remains constructive",
  source: "Demo Wire",
  url: "https://example.com/news/bitcoin-structure",
  publishedAt: "2026-09-04T02:00:00Z",
  analysisStatus: "ANALYZED",
  relatedAssetIds: ["01J00000000000000000000001"],
  sentiment: { label: "POSITIVE", confidence: "0.91", polarityScore: "0.72" }
};

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

async function installControlledBoundary(page: Page) {
  await page.setExtraHTTPHeaders({ "x-playwright-auth-bypass": "f012-local-playwright" });
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const method = route.request().method();
    if (method === "GET" && url.pathname === "/api/v1/candles")
      return json(
        route,
        candlePage(
          url.searchParams.get("pair") ?? "BTC/USDT",
          url.searchParams.get("timeframe") ?? "5m"
        )
      );
    if (method === "GET" && url.pathname === "/api/v1/strategies") return json(route, strategyPage);
    if (method === "GET" && url.pathname === "/api/v1/user-strategies")
      return json(route, { items: [], nextCursor: null, hasMore: false });
    if (method === "GET" && url.pathname === "/api/v1/news-items")
      return json(route, { items: [analyzedNews], nextCursor: null, hasMore: false });
    if (method === "GET" && url.pathname === "/api/v1/backtest-results/result-a11y")
      return json(route, { ...normalBacktestResult, backtestId: null });
    if (method === "POST" && url.pathname === "/api/v1/realtime/ticket")
      return json(
        route,
        { code: "DEPENDENCY_UNAVAILABLE", message: "Realtime unavailable", retryable: true },
        503
      );
    return json(
      route,
      { code: "RESOURCE_NOT_FOUND", message: "Resource inaccessible", retryable: false },
      404
    );
  });
}

async function expectNoPageOverflow(page: Page) {
  await expect
    .poll(() =>
      page.evaluate(
        () =>
          document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1 &&
          document.body.scrollWidth <= document.documentElement.clientWidth + 1
      )
    )
    .toBe(true);
}

async function tabUntil(page: Page, target: Locator, maximumTabs = 60) {
  for (let index = 0; index < maximumTabs; index += 1) {
    await page.keyboard.press("Tab");
    if (await target.evaluate((element) => element === document.activeElement)) return;
  }
  throw new Error(`Không thể tới control bằng bàn phím sau ${maximumTabs} lần Tab`);
}

const viewports = [
  { width: 360, height: 800 },
  { width: 768, height: 1024 },
  { width: 1024, height: 768 },
  { width: 1440, height: 900 }
] as const;

for (const viewport of viewports) {
  test(`main journey không tràn toàn trang ở viewport ${viewport.width}px`, async ({ page }) => {
    await page.setViewportSize(viewport);
    await installControlledBoundary(page);

    await page.goto("/market?pair=BTC%2FUSDT&timeframe=5m&timeframe=15m&timeframe=1h&timeframe=4h");
    await expect(page.getByRole("heading", { name: "Market Dashboard" })).toBeVisible();
    await expect(page.locator(".market-panel")).toHaveCount(4);
    await expectNoPageOverflow(page);

    await page.goto("/search");
    await expect(page.getByRole("heading", { name: "Search & Leaderboard" })).toBeVisible();
    const start = page.getByRole("button", { name: "Start Experiment" });
    await start.scrollIntoViewIfNeeded();
    await expect(start).toBeInViewport();
    await expectNoPageOverflow(page);

    await page.goto("/backtests?resultId=result-a11y");
    await expect(page.getByRole("heading", { name: "Backtest Results" })).toBeVisible();
    const trades = page.getByRole("region", { name: "Scrollable trade history" });
    await trades.scrollIntoViewIfNeeded();
    await expect(trades).toBeInViewport();
    await expectNoPageOverflow(page);

    await page.goto("/news");
    await expect(page.getByRole("heading", { name: "News Sentiment" })).toBeVisible();
    await expect(page.getByText(analyzedNews.title)).toBeVisible();
    await expectNoPageOverflow(page);
  });
}

test("các thao tác chính dùng được chỉ bằng bàn phím", async ({ page }) => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await installControlledBoundary(page);

  await page.goto("/market?pair=BTC%2FUSDT&timeframe=5m&timeframe=15m&timeframe=1h&timeframe=4h");
  const searchLink = page.getByRole("link", { name: "Search & Leaderboard" });
  await tabUntil(page, searchLink);
  await expect(searchLink).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(page).toHaveURL(/\/search$/);

  const start = page.getByRole("button", { name: "Start Experiment" });
  await tabUntil(page, start);
  await expect(start).toBeFocused();

  await page.goto("/news");
  const analyzed = page.getByLabel("ANALYZED", { exact: true });
  await tabUntil(page, analyzed);
  await page.keyboard.press("Space");
  await expect(analyzed).toBeChecked();
  await expect(page).toHaveURL(/analysisStatus=ANALYZED/);

  await page.goto("/backtests?resultId=result-a11y");
  const trades = page.getByRole("region", { name: "Scrollable trade history" });
  await tabUntil(page, trades);
  await expect(trades).toBeFocused();
});
