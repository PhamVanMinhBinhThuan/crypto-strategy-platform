import { expect, test, type Page } from "@playwright/test";

const fixtureProfile = process.env.NEXT_PUBLIC_ENABLE_FIXTURES === "true";

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

async function installMarketBoundary(page: Page, requestedFrames: string[]) {
  await page.setExtraHTTPHeaders({ "x-playwright-auth-bypass": "f012-local-playwright" });
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname === "/api/v1/candles") {
      const pair = url.searchParams.get("pair") ?? "BTC/USDT";
      const timeframe = url.searchParams.get("timeframe") ?? "5m";
      requestedFrames.push(timeframe);
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(candlePage(pair, timeframe))
      });
      return;
    }
    await route.fulfill({ status: 503, contentType: "application/json", body: "{}" });
  });
}

test("contract có kiểm soát hiển thị composition LIVE với bốn chart độc lập", async ({ page }) => {
  test.skip(fixtureProfile, "Market public-boundary journey chạy với LIVE web composition.");
  const requestedFrames: string[] = [];
  await installMarketBoundary(page, requestedFrames);

  await page.goto("/market?pair=BTC%2FUSDT&timeframe=5m&timeframe=15m&timeframe=1h&timeframe=4h");

  await expect(page.locator(".market-panel")).toHaveCount(4);
  await expect(page.locator(".candle-chart")).toHaveCount(4);
  await expect(page.getByLabel("Panel 1 timeframe")).toHaveValue("5m");
  await expect(page.getByLabel("Panel 2 timeframe")).toHaveValue("15m");
  await expect(page.getByLabel("Panel 3 timeframe")).toHaveValue("1h");
  await expect(page.getByLabel("Panel 4 timeframe")).toHaveValue("4h");

  requestedFrames.length = 0;
  await page.getByLabel("Panel 2 timeframe").selectOption("4h");

  await expect(page).toHaveURL(/timeframe=5m.*timeframe=4h.*timeframe=1h.*timeframe=4h/);
  await expect(page.getByLabel("Panel 1 timeframe")).toHaveValue("5m");
  await expect(page.getByLabel("Panel 2 timeframe")).toHaveValue("4h");
  await expect(page.getByLabel("Panel 3 timeframe")).toHaveValue("1h");
  await expect(page.getByLabel("Panel 4 timeframe")).toHaveValue("4h");
  await expect(page.locator(".market-panel")).toHaveCount(4);
  await expect(page.locator(".candle-chart")).toHaveCount(4);
  await expect.poll(() => requestedFrames.length).toBeGreaterThanOrEqual(4);
});

test("nhãn FIXTURE DATA phản ánh đúng web composition", async ({ page }) => {
  await page.setExtraHTTPHeaders({ "x-playwright-auth-bypass": "f012-local-playwright" });
  await page.goto("/search");

  const badges = page.getByText("FIXTURE DATA", { exact: true });
  if (fixtureProfile) await expect(badges.first()).toBeVisible();
  else await expect(badges).toHaveCount(0);
});
