import { expect, test, type Page } from "@playwright/test";

// ---------------------------------------------------------------------------
// Non-gated: unauthenticated redirect — always runs without env vars
// ---------------------------------------------------------------------------
test.describe("Leaderboard unauthenticated redirect", () => {
  test("unauthenticated navigation to search experiment redirects to login", async ({
    page,
    context
  }) => {
    await context.clearCookies();
    await page.goto("/search?id=experiment-013");
    await expect(page).toHaveURL(/\/login\?next=/);
  });
});

// ---------------------------------------------------------------------------
// Credential-gated: leaderboard row to candidate result navigation
// Works with live production backend (NEXT_PUBLIC_ENABLE_FIXTURES=false + F013_E2E_EXPERIMENT_ID)
// or development fixture mode (NEXT_PUBLIC_ENABLE_FIXTURES=true)
// ---------------------------------------------------------------------------
const targetExperimentId =
  process.env.F013_E2E_EXPERIMENT_ID ||
  (process.env.NEXT_PUBLIC_ENABLE_FIXTURES === "true" ? "experiment-013" : undefined);

const available = Boolean(
  process.env.F013_E2E_AUTH_EMAIL && process.env.F013_E2E_AUTH_PASSWORD && targetExperimentId
);

async function authenticate(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email address").fill(process.env.F013_E2E_AUTH_EMAIL!);
  await page.getByLabel("Password", { exact: true }).fill(process.env.F013_E2E_AUTH_PASSWORD!);
  await page.getByRole("button", { name: "Welcome back" }).click();
  await page.waitForURL(/\/market|\/search/);
}

test.describe("F-013 T074: Leaderboard row to candidate result navigation", () => {
  test.skip(
    !available,
    "Requires real auth credentials and an owned Experiment with a leaderboard (or fixture mode)"
  );

  test("navigates from an authoritative leaderboard row to its canonical result ID without ID conversion", async ({
    page
  }) => {
    await authenticate(page);
    await page.goto(`/search?id=${encodeURIComponent(targetExperimentId!)}`);
    const resultLink = page.getByRole("link", { name: "View Backtest" }).first();
    await expect(resultLink).toBeVisible();
    await resultLink.click();
    await expect(page).toHaveURL(/\/backtests\?resultId=/);
    await expect(page.getByText("Total Return")).toBeVisible();
  });
});
