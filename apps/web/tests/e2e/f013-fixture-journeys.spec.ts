import { test, expect, type Page } from "@playwright/test";

async function authenticate(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email address").fill(process.env.F013_E2E_AUTH_EMAIL!);
  await page.getByLabel("Password", { exact: true }).fill(process.env.F013_E2E_AUTH_PASSWORD!);
  await page.getByRole("button", { name: "Welcome back" }).click();
  await page.waitForURL(/\/market/);
}

test.skip(
  !process.env.F013_E2E_AUTH_EMAIL ||
    !process.env.F013_E2E_AUTH_PASSWORD ||
    process.env.NEXT_PUBLIC_ENABLE_FIXTURES !== "true",
  "Requires real development Supabase credentials and explicit fixture mode"
);

test.describe("F-013 protected fixture journeys", () => {
  test("standalone result, monitoring, leaderboard, Stop and configuration gates", async ({
    page
  }) => {
    await authenticate(page);
    await page.goto("/backtests?backtestId=backtest-013");
    await expect(page.getByRole("heading", { name: "Backtest Results" })).toBeVisible();
    await expect(page.getByText("Total Return")).toBeVisible();
    await expect(page.getByRole("region", { name: "Scrollable trade history" })).toBeVisible();
    await page.goto("/search?id=experiment-013");
    await expect(page.getByRole("heading", { name: "BTC trend search" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Evaluation Result ID" })).toBeVisible();
    await page.getByRole("button", { name: "Stop Experiment" }).click();
    await expect(page.getByRole("dialog")).toBeVisible();
    await page.getByRole("button", { name: "Cancel" }).click();
    await expect(page.getByRole("button", { name: "Start Experiment" })).toBeVisible();
    await expect(page.getByText(/Fixture mode uses predefined accepted responses/i)).toBeVisible();
  });
  test("unauthenticated protected navigation returns to the safe login lifecycle", async ({
    page,
    context
  }) => {
    await context.clearCookies();
    await page.goto("/search?id=experiment-013");
    await expect(page).toHaveURL(/\/login\?next=/);
  });
});
