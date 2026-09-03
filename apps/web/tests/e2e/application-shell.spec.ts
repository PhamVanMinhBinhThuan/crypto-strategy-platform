import { test, expect } from "@playwright/test";

test.describe("Application Shell Accessibility & Navigation", () => {
  test("Desktop viewport rendering", async ({ page }) => {
    // Set viewport to 1440px width
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto("/login");

    // The form should be visible
    const form = page.getByRole("form");
    await expect(form).toBeVisible();

    // Verify contrast or specific shell elements
    // We expect the auth layout to have a main element
    const main = page.locator("main");
    await expect(main).toBeVisible();
  });

  test("Mobile viewport rendering", async ({ page }) => {
    // Set viewport to 360px width
    await page.setViewportSize({ width: 360, height: 740 });
    await page.goto("/login");

    // Form should still be visible but constrained
    const form = page.getByRole("form");
    await expect(form).toBeVisible();
  });

  test("Keyboard navigation focuses fields correctly", async ({ page }) => {
    await page.goto("/login");

    // Press Tab to focus first element (might be skip link or first input)
    await page.keyboard.press("Tab");

    // We expect focus to cycle through inputs and buttons
    const emailInput = page.getByLabel("Email address");

    // Focus the email explicitly for testing subsequent tab
    await emailInput.focus();
    await expect(emailInput).toBeFocused();

    await page.keyboard.press("Tab");
    const passwordInput = page.getByLabel("Password", { exact: true });
    await expect(passwordInput).toBeFocused();

    await page.keyboard.press("Tab");
    const loginButton = page.getByRole("button", { name: "Welcome back" });
    await expect(loginButton).toBeFocused();
  });
});
