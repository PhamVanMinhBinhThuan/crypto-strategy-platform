import { expect, test } from "@playwright/test";
import { installF012Adapter } from "./f012-controllable-adapter";

test.beforeEach(async ({ page }) => installF012Adapter(page));

test("Market hiển thị bốn panel và không giữ Candle của selection cũ", async ({ page }) => {
  const duplicateKeyWarnings: string[] = [];
  page.on("console", (message) => {
    if (message.text().includes("same key")) duplicateKeyWarnings.push(message.text());
  });
  await page.goto("/market");
  await expect(page.getByRole("heading", { name: "Market Dashboard" })).toBeVisible();
  await expect(page.locator(".market-panel")).toHaveCount(4);
  await expect(page.locator(".candle-chart")).toHaveCount(4);
  await page.getByLabel("Panel 1 timeframe").selectOption("15m");
  await expect(page).toHaveURL(/timeframe=15m/);
  await expect(page.locator(".candle-chart")).toHaveCount(3);
  expect(duplicateKeyWarnings).toEqual([]);
});

test("Strategy tải hai catalog độc lập và mở form từ descriptor", async ({ page }) => {
  await page.goto("/strategies");
  await expect(page.getByRole("heading", { name: "Strategy Composer" })).toBeVisible();
  await expect(page.getByRole("button", { name: /Momentum cơ bản/ })).toBeVisible();
  await expect(page.getByText("Chưa có Strategy riêng.")).toBeVisible();
  await page.getByRole("button", { name: /Momentum cơ bản/ }).click();
  await expect(page.getByRole("heading", { name: "Tạo Strategy riêng" })).toBeVisible();
});

test("News hiển thị nội dung, sentiment và safe external link", async ({ page }) => {
  await page.goto("/news");
  await expect(page.getByRole("heading", { name: "News Sentiment" })).toBeVisible();
  const article = page.getByRole("article");
  await expect(article).toContainText("Thị trường tài sản số cập nhật");
  await expect(article).toContainText("NEUTRAL");
  await expect(article.getByRole("link")).toHaveAttribute("rel", "noopener noreferrer");
});
