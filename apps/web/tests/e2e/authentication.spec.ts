import { test, expect } from "@playwright/test";

test.describe("Authentication Flows", () => {
  test("User can register, sign in, and reset password", async ({ page }) => {
    page.on("pageerror", (err) => console.log("PAGE ERROR:", err.message));
    page.on("console", (msg) => {
      if (msg.type() === "error") console.log("CONSOLE ERROR:", msg.text());
    });

    // Navigate to register and wait for Next.js hydration
    await page.goto("/register");
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveTitle(/Register/i);

    // Register form elements should be present
    const emailInput = page.getByLabel("Email address");
    const passwordInput = page.getByLabel("Password", { exact: true });
    const confirmInput = page.getByLabel("Confirm password");
    const registerBtn = page.getByRole("button", { name: "Create your account" });

    // Fill form and ensure values stick
    await emailInput.fill("test@example.com");
    await expect(emailInput).toHaveValue("test@example.com");

    await passwordInput.fill("Password123!");
    await expect(passwordInput).toHaveValue("Password123!");

    await confirmInput.fill("Mismatch123!");
    await expect(confirmInput).toHaveValue("Mismatch123!");

    // Mock the Supabase register route early just in case
    await page.route("**/auth/v1/signup*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ id: "user-123", email: "test@example.com" })
      });
    });

    // Check mismatch
    await registerBtn.click();
    await expect(page.getByText("Passwords do not match.")).toBeVisible();

    // Valid register
    await confirmInput.fill("Password123!");
    await expect(confirmInput).toHaveValue("Password123!");
    await registerBtn.click();

    // It should now show success
    await expect(page.getByText(/Check your email/i)).toBeVisible();

    // Navigate to login
    await page.goto("/login");
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveTitle(/Login/i);

    const loginEmail = page.getByLabel("Email address");
    const loginPassword = page.getByLabel("Password", { exact: true });
    const loginBtn = page.getByRole("button", { name: "Welcome back" });

    // Mock login route
    await page.route("**/auth/v1/token?grant_type=password*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          access_token: "mock-token",
          token_type: "bearer",
          expires_in: 3600,
          refresh_token: "mock-refresh",
          user: { id: "user-123" }
        })
      });
    });

    await loginEmail.fill("test@example.com");
    await expect(loginEmail).toHaveValue("test@example.com");
    await loginPassword.fill("Password123!");
    await expect(loginPassword).toHaveValue("Password123!");

    await loginBtn.click();

    // We expect the middleware to bounce the user back to login since the session cookie isn't set.
    // Waiting for this URL change ensures the next test steps aren't interrupted by a delayed redirect.
    await page.waitForURL(/.*next=%2Fmarket/);

    // Forgot password flow
    await page.goto("/forgot-password");
    await page.waitForLoadState("networkidle");
    await expect(page).toHaveURL(/.*forgot-password/);

    const forgotEmail = page.getByLabel("Email address");
    const resetBtn = page.getByRole("button", { name: "Reset your password" });

    // Mock reset route
    await page.route("**/auth/v1/recover*", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({})
      });
    });

    await forgotEmail.fill("test@example.com");
    await expect(forgotEmail).toHaveValue("test@example.com");

    await resetBtn.click();

    // Verify neutral recovery response
    await expect(page.getByText(/If an account can receive email/)).toBeVisible();
  });
});
