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

for (const width of [360, 768, 1024, 1440]) {
  test(`F-013 responsive and keyboard evidence at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await authenticate(page);
    await page.goto("/search?id=experiment-013");
    await expect(page.getByRole("button", { name: "Stop Experiment" })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true
    );
    const table = page.getByRole("region", { name: /Scrollable leaderboard/i });
    await table.focus();
    await expect(table).toBeFocused();
  });
}
