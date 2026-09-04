import { expect, test, type Page } from "@playwright/test";

// ---------------------------------------------------------------------------
// Non-gated: unauthenticated redirect — always runs without env vars
// ---------------------------------------------------------------------------
test.describe("Search success unauthenticated redirect", () => {
  test("unauthenticated navigation to search redirects to login", async ({ page, context }) => {
    await context.clearCookies();
    await page.goto("/search");
    await expect(page).toHaveURL(/\/login\?next=/);
  });
});

// ---------------------------------------------------------------------------
// Credential-gated: Start and Reproduce search experiment journey
// Works with live production backend (when F-010 is released)
// or development fixture mode (NEXT_PUBLIC_ENABLE_FIXTURES=true)
// ---------------------------------------------------------------------------
const isFixture = process.env.NEXT_PUBLIC_ENABLE_FIXTURES === "true";
const targetDatasetId =
  process.env.F013_E2E_DATASET_ID || (isFixture ? "dataset-btc-1h" : undefined);

const available = Boolean(
  process.env.F013_E2E_AUTH_EMAIL &&
  process.env.F013_E2E_AUTH_PASSWORD &&
  targetDatasetId &&
  (isFixture ||
    (process.env.NEXT_PUBLIC_API_BASE_URL &&
      process.env.NEXT_PUBLIC_WS_URL &&
      process.env.NEXT_PUBLIC_ENABLE_FIXTURES === "false"))
);

async function login(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email address").fill(process.env.F013_E2E_AUTH_EMAIL!);
  await page.getByLabel("Password", { exact: true }).fill(process.env.F013_E2E_AUTH_PASSWORD!);
  await page.getByRole("button", { name: "Welcome back" }).click();
  await page.waitForURL(/\/market|\/search/);
}

test.describe("F-013 T099: Start and Reproduce search experiment journey", () => {
  test.skip(!available, "Requires auth credentials and target dataset (or fixture mode)");

  test("starts, monitors, stops, reproduces and reads the leaderboard", async ({ page }) => {
    test.setTimeout(180_000);
    await login(page);
    await page.goto("/search");
    await page.getByLabel("Name").fill(`F-013 E2E ${Date.now()}`);
    await page.getByLabel("Dataset ID", { exact: true }).fill(targetDatasetId!);
    await page.getByLabel("fastPeriod minimum").fill("20");
    await page.getByLabel("fastPeriod maximum").fill("20");
    await page.getByLabel("slowPeriod minimum").fill("25");
    await page.getByLabel("slowPeriod maximum").fill("25");
    await page.getByLabel("Maximum candidates").fill("1");
    await page.getByLabel("Top-K").selectOption("10");
    await page.getByRole("button", { name: "Start Experiment" }).click();

    const createdLink = page.getByRole("link", { name: /Open Experiment/ });
    await expect(createdLink).toBeVisible({ timeout: 15_000 });
    const experimentHref = await createdLink.getAttribute("href");
    expect(experimentHref).toMatch(/^\/search\?id=/);
    await page.goto(experimentHref!);
    await expect(page).toHaveURL(/\/search\?id=/);
    await expect(page.getByRole("heading", { name: /Experiment|BTC trend search/i })).toBeVisible();

    const leaderboard = page.getByRole("region", {
      name: /Scrollable leaderboard|Scrollable experiment leaderboard/i
    });
    if (await leaderboard.isVisible()) await expect(leaderboard).toBeVisible();

    await expect
      .poll(
        async () => {
          await page.reload();
          return page.getByText("COMPLETED", { exact: true }).isVisible();
        },
        { timeout: 90_000, intervals: [2_000] }
      )
      .toBe(true);
    await expect(leaderboard).toBeVisible();

    await page.getByRole("button", { name: "Reproduce Experiment" }).click();
    await expect(page.getByRole("link", { name: /Open reproduced Experiment/ })).toBeVisible({
      timeout: 15_000
    });
  });
});
