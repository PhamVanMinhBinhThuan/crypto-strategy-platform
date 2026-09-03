import { expect, test, type Page } from "@playwright/test";
import { installF012Adapter } from "./f012-controllable-adapter";

const journeys = [
  ["/market", ".candle-chart"],
  ["/strategies", ".strategy-library button"],
  ["/news", ".news-card"]
] as const;

async function readiness(page: Page, path: string, primaryContent: string) {
  await page.goto(path);
  await expect(page.locator(primaryContent).first()).toBeVisible();
  return page.evaluate(() => performance.now());
}

test("SC-001: ít nhất 95% lần mở từng route usable dưới 2 giây", async ({ page }) => {
  await installF012Adapter(page);
  for (const [path, primaryContent] of journeys) {
    await readiness(page, path, primaryContent);
    const samples: number[] = [];
    for (let index = 0; index < 20; index += 1)
      samples.push(await readiness(page, path, primaryContent));
    expect(samples.filter((duration) => duration < 2_000).length, path).toBeGreaterThanOrEqual(19);
  }
});
