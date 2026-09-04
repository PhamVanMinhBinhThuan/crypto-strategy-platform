import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  workers: 1,
  use: { baseURL: "http://localhost:3000", trace: "retain-on-failure" },
  projects: [
    {
      name: "performance-desktop",
      testMatch: /f012-performance\.spec\.ts/,
      use: { ...devices["Desktop Chrome"], viewport: { width: 1440, height: 900 } }
    },
    {
      name: "performance-mobile",
      testMatch: /f012-performance\.spec\.ts/,
      use: { ...devices["iPhone 13"], viewport: { width: 360, height: 740 } }
    },
    {
      name: "desktop",
      testIgnore: /f012-performance\.spec\.ts/,
      use: { ...devices["Desktop Chrome"], viewport: { width: 1440, height: 900 } }
    },
    {
      name: "mobile",
      testIgnore: /f012-performance\.spec\.ts/,
      use: { ...devices["iPhone 13"], viewport: { width: 360, height: 740 } }
    }
  ],
  webServer: {
    command: "npm run dev",
    url: "http://localhost:3000",
    timeout: 180_000,
    reuseExistingServer: !process.env.CI,
    env: {
      NEXT_PUBLIC_SUPABASE_URL: process.env.NEXT_PUBLIC_SUPABASE_URL ?? "https://test.supabase.co",
      NEXT_PUBLIC_SUPABASE_ANON_KEY:
        process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ?? "public-anon-key-placeholder",
      NEXT_PUBLIC_API_BASE_URL:
        process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://127.0.0.1:8080/api/v1",
      NEXT_PUBLIC_WS_URL: process.env.NEXT_PUBLIC_WS_URL ?? "ws://127.0.0.1:8080/ws",
      NEXT_PUBLIC_ENABLE_FIXTURES: process.env.NEXT_PUBLIC_ENABLE_FIXTURES ?? "false",
      PLAYWRIGHT_AUTH_BYPASS_TOKEN:
        process.env.PLAYWRIGHT_AUTH_BYPASS_TOKEN ?? "f012-local-playwright"
    }
  }
});
