import { expect, test, type Page } from "@playwright/test";

// ---------------------------------------------------------------------------
// Non-gated: unauthenticated redirect — always runs without env vars
// ---------------------------------------------------------------------------
test.describe("Backtest resultId unauthenticated redirect", () => {
  test("unauthenticated navigation to resultId redirects to login", async ({ page, context }) => {
    await context.clearCookies();
    await page.goto("/backtests?resultId=result-013");
    await expect(page).toHaveURL(/\/login\?next=/);
  });
});

// ---------------------------------------------------------------------------
// Credential-gated: candidate result lookup by canonical result ID
// Works with live production backend (NEXT_PUBLIC_ENABLE_FIXTURES=false + F013_E2E_RESULT_ID)
// or development fixture mode (NEXT_PUBLIC_ENABLE_FIXTURES=true)
// ---------------------------------------------------------------------------
const targetResultId =
  process.env.F013_E2E_RESULT_ID ||
  (process.env.NEXT_PUBLIC_ENABLE_FIXTURES === "true" ? "result-013" : undefined);

const available = Boolean(
  process.env.F013_E2E_AUTH_EMAIL && process.env.F013_E2E_AUTH_PASSWORD && targetResultId
);

async function authenticate(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email address").fill(process.env.F013_E2E_AUTH_EMAIL!);
  await page.getByLabel("Password", { exact: true }).fill(process.env.F013_E2E_AUTH_PASSWORD!);
  await page.getByRole("button", { name: "Welcome back" }).click();
  await page.waitForURL(/\/market|\/backtests/);
}

test.describe("F-013 T041: Candidate result lookup by canonical result ID", () => {
  test.skip(
    !available,
    "Requires real auth credentials and an owned Backtest Result ID (or fixture mode)"
  );

  test("reads an owned Search candidate result by its canonical result ID without translating identity", async ({
    page
  }) => {
    await authenticate(page);
    await page.goto(`/backtests?resultId=${encodeURIComponent(targetResultId!)}`);
    await expect(page.getByRole("heading", { name: "Backtest Results" })).toBeVisible();
    await expect(page.getByText("Total Return")).toBeVisible();
    await expect(page.getByRole("region", { name: "Scrollable trade history" })).toBeVisible();
  });
});
