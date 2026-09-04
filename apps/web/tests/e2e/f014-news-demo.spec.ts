import { expect, test, type Page } from "@playwright/test";

const analyzed = {
  newsId: "01J00000000000000000000201",
  title: "Bitcoin market structure remains constructive",
  source: "Demo Wire",
  url: "https://example.com/news/bitcoin-structure",
  publishedAt: "2026-09-04T02:00:00Z",
  analysisStatus: "ANALYZED",
  relatedAssetIds: ["01J00000000000000000000001"],
  sentiment: { label: "POSITIVE", confidence: "0.91", polarityScore: "0.72" }
};

const sentimentUnavailable = {
  newsId: "01J00000000000000000000202",
  title: "Ethereum network activity update",
  source: "Demo Wire",
  url: "https://example.com/news/ethereum-activity",
  publishedAt: "2026-09-04T01:00:00Z",
  analysisStatus: "FAILED_RETRYABLE",
  relatedAssetIds: ["01J00000000000000000000002"],
  sentiment: null
};

async function authorize(page: Page) {
  await page.setExtraHTTPHeaders({ "x-playwright-auth-bypass": "f012-local-playwright" });
}

test("News vẫn hiển thị khi một sentiment lỗi và filter đi qua public query", async ({ page }) => {
  await authorize(page);
  const queries: string[][] = [];
  await page.route("**/api/v1/news-items**", async (route) => {
    const url = new URL(route.request().url());
    const statuses = url.searchParams.getAll("analysisStatus");
    queries.push(statuses);
    const items = statuses.length
      ? [analyzed, sentimentUnavailable].filter((item) => statuses.includes(item.analysisStatus))
      : [analyzed, sentimentUnavailable];
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ items, nextCursor: null, hasMore: false })
    });
  });

  await page.goto("/news");
  await expect(page.getByText(analyzed.title)).toBeVisible();
  await expect(page.getByText("POSITIVE", { exact: true })).toBeVisible();
  await expect(page.getByText(sentimentUnavailable.title)).toBeVisible();
  await expect(page.getByText(/Sentiment tạm gián đoạn/)).toBeVisible();

  await page.getByLabel("ANALYZED", { exact: true }).check();
  await expect(page).toHaveURL(/analysisStatus=ANALYZED/);
  await expect(page.getByText(analyzed.title)).toBeVisible();
  await expect(page.getByText(sentimentUnavailable.title)).toHaveCount(0);
  await expect.poll(() => queries.some((values) => values.includes("ANALYZED"))).toBe(true);
});

test("News provider lỗi có retry rõ ràng và phục hồi về dữ liệu authoritative", async ({
  page
}) => {
  await authorize(page);
  let attempts = 0;
  await page.route("**/api/v1/news-items**", async (route) => {
    attempts += 1;
    if (attempts === 1) {
      await route.fulfill({
        status: 503,
        headers: { "Retry-After": "1" },
        contentType: "application/json",
        body: JSON.stringify({
          code: "DEPENDENCY_UNAVAILABLE",
          message: "News provider unavailable",
          retryable: true
        })
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ items: [analyzed], nextCursor: null, hasMore: false })
    });
  });

  await page.goto("/news");
  await expect(page.getByText(/News đang tạm gián đoạn/).first()).toBeVisible();
  await page.getByRole("button", { name: "Thử lại" }).click();
  await expect(page.getByText(analyzed.title)).toBeVisible();
  expect(attempts).toBe(2);
});
