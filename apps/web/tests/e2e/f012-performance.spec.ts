import { expect, test, type Page } from "@playwright/test";
import { installF012Adapter } from "./f012-controllable-adapter";

test.describe.configure({ timeout: 180_000 });
test.use({ trace: "off" });

const journeys = [
  ["/market", "Market Dashboard", ".candle-chart"],
  ["/strategies", "Strategy Composer", ".strategy-library button"],
  ["/news", "News Sentiment", ".news-card"]
] as const;

async function warmUp(page: Page, path: string, primaryContent: string) {
  try {
    await page.goto(path);
  } catch (error) {
    if (!(error instanceof Error) || !error.message.includes("interrupted by another navigation"))
      throw error;
    // A newly compiled route can make the Next.js development client reload the previous page.
    await page.goto(path);
  }
  await expect(page.locator(primaryContent).first()).toBeVisible({ timeout: 30_000 });
}

async function readiness(page: Page, linkName: string, primaryContent: string) {
  const startedAt = Date.now();
  await page.getByRole("link", { name: linkName, exact: true }).click({ noWaitAfter: true });
  try {
    await page.locator(primaryContent).first().waitFor({ state: "visible", timeout: 5_000 });
  } catch {
    return Number.POSITIVE_INFINITY;
  }
  return Date.now() - startedAt;
}

test("SC-001: ít nhất 95% lần mở từng route usable dưới 2 giây", async ({ page }) => {
  await installF012Adapter(page);
  for (const [path, , primaryContent] of journeys) await warmUp(page, path, primaryContent);
  for (const [path, linkName, primaryContent] of journeys) {
    const alternate = path === "/market" ? journeys[1] : journeys[0];
    const samples: number[] = [];
    for (let index = 0; index < 20; index += 1) {
      await page.getByRole("link", { name: alternate[1], exact: true }).click();
      await expect(page).toHaveURL(new RegExp(`${alternate[0]}(?:\\?|$)`));
      samples.push(await readiness(page, linkName, primaryContent));
      // Keep the synthetic loop below the browser's pushState abuse threshold.
      await page.waitForTimeout(150);
    }
    expect(
      samples.filter((duration) => duration < 2_000).length,
      `${path}: ${JSON.stringify(samples)}`
    ).toBeGreaterThanOrEqual(19);
  }
});
