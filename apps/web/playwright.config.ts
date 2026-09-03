import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  use: { baseURL: "http://localhost:3000", trace: "retain-on-failure" },
  projects: [
    {
      name: "desktop",
      use: { ...devices["Desktop Chrome"], viewport: { width: 1440, height: 900 } }
    },
    { name: "mobile", use: { ...devices["iPhone 13"], viewport: { width: 360, height: 740 } } }
  ],
  webServer: {
    command: "npm run dev",
    url: "http://localhost:3000",
    reuseExistingServer: !process.env.CI,
    env: {
      PLAYWRIGHT_AUTH_BYPASS_TOKEN: "f012-local-playwright",
      NEXT_PUBLIC_SUPABASE_URL: "https://example.supabase.co",
      NEXT_PUBLIC_SUPABASE_ANON_KEY: "public-anon-placeholder",
      NEXT_PUBLIC_API_BASE_URL: "http://localhost:3000",
      NEXT_PUBLIC_WS_URL: "ws://localhost:3000/ws",
      NEXT_PUBLIC_ENABLE_FIXTURES: "false"
    }
  }
});
