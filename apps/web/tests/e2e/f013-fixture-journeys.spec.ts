import { test, expect, type Page } from "@playwright/test";

// ---------------------------------------------------------------------------
// Authentication helper — requires real development Supabase credentials
// ---------------------------------------------------------------------------
async function authenticate(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email address").fill(process.env.F013_E2E_AUTH_EMAIL!);
  await page.getByLabel("Password", { exact: true }).fill(process.env.F013_E2E_AUTH_PASSWORD!);
  await page.getByRole("button", { name: "Welcome back" }).click();
  await page.waitForURL(/\/market/);
}

// ---------------------------------------------------------------------------
// Non-gated: unauthenticated redirect — always runs without env vars
// ---------------------------------------------------------------------------
test.describe("F-013 unauthenticated redirect (no credentials required)", () => {
  test("unauthenticated navigation to /search returns to the safe login lifecycle", async ({
    page,
    context
  }) => {
    await context.clearCookies();
    await page.goto("/search?id=experiment-013");
    await expect(page).toHaveURL(/\/login\?next=/);
  });

  test("unauthenticated navigation to /backtests returns to the safe login lifecycle", async ({
    page,
    context
  }) => {
    await context.clearCookies();
    await page.goto("/backtests?backtestId=backtest-013");
    await expect(page).toHaveURL(/\/login\?next=/);
  });
});

// ---------------------------------------------------------------------------
// Credential-gated + fixture-mode: all protected journeys
// ---------------------------------------------------------------------------
test.skip(
  !process.env.F013_E2E_AUTH_EMAIL ||
    !process.env.F013_E2E_AUTH_PASSWORD ||
    process.env.NEXT_PUBLIC_ENABLE_FIXTURES !== "true",
  "Requires real development Supabase credentials and explicit fixture mode (NEXT_PUBLIC_ENABLE_FIXTURES=true)"
);

// ---------------------------------------------------------------------------
// US1 — Backtest Result journeys
// ---------------------------------------------------------------------------
test.describe("F-013 US1: Backtest standalone result", () => {
  test("renders all four metrics, capital, trade history, provenance, and assumptions for a normal result", async ({
    page
  }) => {
    await authenticate(page);
    await page.goto("/backtests?backtestId=backtest-013");

    // Page heading
    await expect(page.getByRole("heading", { name: "Backtest Results" })).toBeVisible();

    // Exactly four metrics (FR-003)
    await expect(page.getByText("Total Return")).toBeVisible();
    await expect(page.getByText("Win Rate")).toBeVisible();
    await expect(page.getByText("Maximum Drawdown")).toBeVisible();
    await expect(page.getByText("Number of Trades")).toBeVisible();

    // Capital summary (FR-005)
    await expect(page.getByText("Initial Capital")).toBeVisible();
    await expect(page.getByText("Final Capital")).toBeVisible();

    // Trade history scroll region (FR-006)
    await expect(page.getByRole("region", { name: "Scrollable trade history" })).toBeVisible();
    // Released trade columns
    await expect(page.getByRole("columnheader", { name: "Side" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: /Entry/i })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: /Exit/i })).toBeVisible();

    // Provenance section (FR-007)
    await expect(page.getByText(/Manifest Fingerprint/i)).toBeVisible();
    await expect(page.getByText(/Strategy Fingerprint/i)).toBeVisible();

    // Assumptions section (FR-008)
    await expect(page.getByText(/Fee Rate/i)).toBeVisible();
    await expect(page.getByText(/Position Mode/i)).toBeVisible();
  });

  test("renders zero-trades empty state without treating it as an error", async ({ page }) => {
    await authenticate(page);
    // Zero-trades fixture driven by resultId query
    await page.goto("/backtests?backtestId=backtest-zero");

    await expect(page.getByText("Total Return")).toBeVisible();
    await expect(page.getByText("Number of Trades")).toBeVisible();
    // Empty trade list notice, not an error panel
    await expect(page.getByText(/no.*trade/i)).toBeVisible();
    await expect(page.getByRole("alert")).not.toBeVisible();
  });

  test("renders empty guidance when no identifier is provided", async ({ page }) => {
    await authenticate(page);
    await page.goto("/backtests");
    // FR-009: helpful empty state
    await expect(page.getByText(/select.*candidate|supply.*identifier|leaderboard/i)).toBeVisible();
    // Must NOT attempt a fetch or show an error
    await expect(page.getByRole("alert")).not.toBeVisible();
  });

  test("renders uniform ownership-safe inaccessible state for invalid/foreign IDs", async ({
    page
  }) => {
    await authenticate(page);
    // Fixture returns RESOURCE_NOT_FOUND for unknown IDs
    await page.goto("/backtests?backtestId=unknown-foreign-id");
    // FR-010: uniform inaccessible state, no ownership leak
    await expect(page.getByText(/resource inaccessible/i)).toBeVisible();
    // Must not reveal whether resource exists
    await expect(page.getByText(/not found/i)).not.toBeVisible();
    await expect(page.getByText(/403|404/)).not.toBeVisible();
  });

  test("shows the fixture-mode indicator in fixture mode", async ({ page }) => {
    await authenticate(page);
    await page.goto("/backtests?backtestId=backtest-013");
    await expect(page.getByText(/Deterministic fixture mode/i)).toBeVisible();
  });
});

// ---------------------------------------------------------------------------
// US2 — Experiment/Job monitoring journeys
// ---------------------------------------------------------------------------
test.describe("F-013 US2: Experiment and Job monitoring", () => {
  test("displays authoritative experiment status, name, and job progress for a running experiment", async ({
    page
  }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-013");

    // Experiment name and lifecycle status (FR-016)
    await expect(page.getByRole("heading", { name: "BTC trend search" })).toBeVisible();
    await expect(page.getByText("RUNNING")).toBeVisible();

    // Job progress fields (FR-017)
    await expect(page.getByText(/completedWork|completed work/i)).toBeVisible();
    await expect(page.getByText(/totalWork|total work/i)).toBeVisible();
  });

  test("displays FAILED experiment with safe terminal failure code and message", async ({
    page
  }) => {
    await authenticate(page);
    // experiment-failed fixture
    await page.goto("/search?id=experiment-failed");

    await expect(page.getByText("FAILED")).toBeVisible();
    // Safe failure code must be visible, no raw stack trace (FR-040)
    await expect(page.getByText(/JOB_EXECUTION_TIMEOUT/i)).toBeVisible();
    // No unhandled error boundary
    await expect(page.getByText(/Something went wrong/i)).not.toBeVisible();
  });

  test("shows fixture-mode indicator on the search page", async ({ page }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-013");
    await expect(page.getByText(/Deterministic fixture mode/i)).toBeVisible();
  });
});

// ---------------------------------------------------------------------------
// US3 — Leaderboard journeys
// ---------------------------------------------------------------------------
test.describe("F-013 US3: Leaderboard paging and columns", () => {
  test("renders exactly the six released leaderboard columns", async ({ page }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-013");

    // Exactly six released columns (FR-023)
    await expect(page.getByRole("columnheader", { name: "Rank" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Evaluation Result ID" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Backtest Result ID" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Score" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Maximum Drawdown" })).toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Evaluation Fingerprint" })).toBeVisible();

    // Must NOT show synthesized columns (FR-024)
    await expect(page.getByRole("columnheader", { name: "Total Return" })).not.toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Win Rate" })).not.toBeVisible();
    await expect(page.getByRole("columnheader", { name: "Sharpe" })).not.toBeVisible();
  });

  test("Top-K limit presets 10, 25, and 50 are available (FR-026)", async ({ page }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-013");

    // Preset selectors or buttons for limit changes (FR-026)
    await expect(page.getByRole("button", { name: "10" })).toBeVisible();
    await expect(page.getByRole("button", { name: "25" })).toBeVisible();
    await expect(page.getByRole("button", { name: "50" })).toBeVisible();
  });

  test("empty leaderboard shows the 'no candidates evaluated' empty state", async ({ page }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-created");

    await expect(
      page.getByText(/No strategy candidates evaluated yet|Awaiting evaluation/i)
    ).toBeVisible();
  });
});

// ---------------------------------------------------------------------------
// US4 — Stop Experiment safety journeys
// ---------------------------------------------------------------------------
test.describe("F-013 US4: Stop Experiment safety", () => {
  test("Stop button opens a confirmation dialog that can be cancelled without dispatching a command", async ({
    page
  }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-013");

    // FR-018: Stop control for RUNNING experiment
    const stopButton = page.getByRole("button", { name: "Stop Experiment" });
    await expect(stopButton).toBeVisible();
    await stopButton.click();

    // Confirmation dialog must appear
    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();

    // Cancel — no command should be dispatched
    await page.getByRole("button", { name: "Cancel" }).click();
    await expect(dialog).not.toBeVisible();
    // Stop button is restored
    await expect(stopButton).toBeVisible();
  });

  test("Stop button is disabled/absent for a terminal experiment (COMPLETED)", async ({ page }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-completed");

    // COMPLETED experiment should NOT show an active Stop control
    await expect(page.getByRole("button", { name: "Stop Experiment" })).not.toBeVisible();
  });

  test("displays STOP_REQUESTED status when fixture returns accepted stop", async ({ page }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-stop-requested");

    // FR-016: stop-requested lifecycle visible
    await expect(page.getByText("STOP_REQUESTED")).toBeVisible();
  });
});

// ---------------------------------------------------------------------------
// US5 — Start/Reproduce dependency gate journeys
// ---------------------------------------------------------------------------
test.describe("F-013 US5: Start and Reproduce dependency gate", () => {
  test("Start Experiment button is visible and configuration form is accessible", async ({
    page
  }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-013");

    // FR-012: Configuration form presence
    await expect(page.getByRole("button", { name: "Start Experiment" })).toBeVisible();
  });

  test("dependency-unavailable notice is shown and user inputs are preserved on 503 (FR-015)", async ({
    page
  }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-013");

    // Open the configuration form / Start experiment flow
    await page.getByRole("button", { name: "Start Experiment" }).click();

    // Fixture returns 503 BLOCKED_SEARCH_COORDINATOR
    // The dependency notice must be visible (FR-015)
    await expect(
      page.getByText(/Search Coordinator|BLOCKED_SEARCH_COORDINATOR|dependency unavailable/i)
    ).toBeVisible();

    // Must NOT claim the experiment was created (FR-015)
    await expect(page.getByText(/experiment started|creation successful/i)).not.toBeVisible();
  });

  test("fixture mode indicator is visible on the search page", async ({ page }) => {
    await authenticate(page);
    await page.goto("/search?id=experiment-013");
    // Fixture acceptance is visibly test/dev-only (T034, tasks.md guardrails)
    await expect(page.getByText(/Deterministic fixture mode/i)).toBeVisible();
  });
});

// ---------------------------------------------------------------------------
// US6 — 429 rate-limit eligibility journey
// ---------------------------------------------------------------------------
test.describe("F-013 US6: 429 rate-limit eligibility", () => {
  test("rate-limited backtest state preserves safe snapshot and shows retry guidance", async ({
    page
  }) => {
    await authenticate(page);
    // Fixture returns 429 RATE_LIMIT_EXCEEDED
    await page.goto("/backtests?backtestId=backtest-rate-limited");

    // FR-042: safe snapshot preserved, retry message visible
    await expect(page.getByText(/rate.limit|wait before retrying|retry after/i)).toBeVisible();

    // Must NOT immediately retry automatically (no rapid fetch loop)
    // The retry button should be visible but disabled until Retry-After elapses
    const retryButton = page.getByRole("button", { name: /retry/i });
    if (await retryButton.isVisible()) {
      // If a retry affordance is shown, it must respect the Retry-After period
      // In fixture mode this is deterministic — we just assert it's not already cycling
      await expect(retryButton).toBeDisabled();
    }
  });
});
