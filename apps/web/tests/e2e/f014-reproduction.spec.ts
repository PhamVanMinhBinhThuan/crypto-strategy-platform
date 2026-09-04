import { expect, test, type Page, type Route } from "@playwright/test";
import { normalBacktestResult } from "../../src/features/backtests/fixtures/backtest-result-fixtures";
import { emptyLeaderboard } from "../../src/features/leaderboard/fixtures/leaderboard-fixtures";

const sourceExperimentId = "experiment-source-014";
const reproductionExperimentId = "experiment-reproduction-014";

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

async function authorize(page: Page) {
  await page.setExtraHTTPHeaders({ "x-playwright-auth-bypass": "f012-local-playwright" });
}

test("traces canonical Result evidence then creates a separately linked reproduction", async ({
  page
}) => {
  await authorize(page);
  let reproduceCalls = 0;
  const experiment = (id: string) => ({
    experimentId: id,
    name: id === sourceExperimentId ? "Source research run" : "Independent reproduction",
    status: "COMPLETED",
    datasetId: "dataset-version-014",
    jobIds: [],
    derivedFromExperimentId: null,
    reproducesExperimentId: id === sourceExperimentId ? null : sourceExperimentId,
    startedAt: "2026-09-04T08:00:00Z",
    completedAt: "2026-09-04T08:05:00Z",
    failure: null,
    createdAt: "2026-09-04T07:59:00Z"
  });
  const canonicalResult = {
    ...normalBacktestResult,
    backtestId: null,
    provenance: {
      ...normalBacktestResult.provenance,
      experimentId: sourceExperimentId,
      successfulAttemptId: "attempt-accepted-014",
      manifestVersion: "experiment-manifest-v1",
      dataset: {
        datasetVersionId: "dataset-version-014",
        version: "binance-btcusdt-1h-v1",
        checksum: `sha256:${"b".repeat(64)}`,
        provider: "binance",
        tradingPair: "BTCUSDT",
        timeframe: "1h",
        normalizationVersion: "ohlcv-v1",
        rangeStart: "2026-09-01T00:00:00Z",
        rangeEnd: "2026-09-03T00:00:00Z",
        candleCount: 48
      },
      strategy: {
        kind: "SINGLE",
        singleStrategy: {
          strategyVersionId: "strategy-version-014",
          pluginId: "moving-average-crossover",
          implementationVersion: "1.0.0"
        },
        parameters: {
          fastPeriod: { type: "INTEGER", value: "20" },
          slowPeriod: { type: "INTEGER", value: "50" }
        },
        compositePolicyId: null,
        compositePolicyVersion: null,
        components: [],
        sourceUserStrategyVersionId: null,
        fingerprint: `strategy-v1:sha256:${"c".repeat(64)}`
      },
      candidate: {
        candidateId: normalBacktestResult.provenance.candidateId,
        generationIndex: 7,
        definition: { fastPeriod: 20, slowPeriod: 50 },
        fingerprint: "sha256:candidate014",
        createdAt: "2026-09-04T08:01:00Z"
      },
      softwareVersion: "f014-demo",
      gitCommit: "50c28d9"
    }
  };

  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const method = route.request().method();
    if (method === "GET" && url.pathname === "/api/v1/backtest-results/result-013")
      return json(route, canonicalResult);
    if (
      method === "GET" &&
      url.pathname.startsWith("/api/v1/experiments/") &&
      !url.pathname.endsWith("/candidates") &&
      !url.pathname.endsWith("/leaderboard") &&
      !url.pathname.endsWith("/reproduction-verification")
    ) {
      const id = decodeURIComponent(url.pathname.split("/").at(-1) ?? "");
      return json(route, experiment(id));
    }
    if (method === "GET" && url.pathname.endsWith("/candidates"))
      return json(route, { items: [], nextCursor: null, hasMore: false });
    if (method === "GET" && url.pathname.endsWith("/leaderboard"))
      return json(route, {
        ...emptyLeaderboard,
        experimentId: url.pathname.split("/")[4]
      });
    if (
      method === "GET" &&
      url.pathname === `/api/v1/experiments/${reproductionExperimentId}/reproduction-verification`
    )
      return json(route, {
        verificationId: "verification-014",
        sourceExperimentId,
        reproductionExperimentId,
        status: "MATCHED",
        tradesMatched: true,
        metricsMatched: true,
        fingerprintsMatched: true,
        sourceEvidenceFingerprint: "sha256:source-evidence",
        reproductionEvidenceFingerprint: "sha256:reproduction-evidence",
        differences: {},
        failure: null,
        startedAt: "2026-09-04T08:05:01Z",
        finishedAt: "2026-09-04T08:05:02Z",
        updatedAt: "2026-09-04T08:05:02Z"
      });
    if (
      method === "POST" &&
      url.pathname === `/api/v1/experiments/${sourceExperimentId}/reproductions`
    ) {
      reproduceCalls += 1;
      expect(route.request().headers()["idempotency-key"]).toBeTruthy();
      return json(
        route,
        {
          experimentId: reproductionExperimentId,
          jobId: "job-reproduction-014",
          status: "QUEUED"
        },
        202
      );
    }
    if (method === "GET" && url.pathname === "/api/v1/strategies")
      return json(route, { items: [], nextCursor: null, hasMore: false });
    if (method === "GET" && url.pathname === "/api/v1/user-strategies")
      return json(route, { items: [], nextCursor: null, hasMore: false });
    if (method === "POST" && url.pathname === "/api/v1/realtime/ticket")
      return json(
        route,
        { code: "DEPENDENCY_UNAVAILABLE", message: "Realtime unavailable", retryable: true },
        503
      );
    return json(
      route,
      { code: "RESOURCE_NOT_FOUND", message: "Resource inaccessible", retryable: false },
      404
    );
  });

  await page.goto("/backtests?resultId=result-013");
  await expect(page.getByRole("heading", { name: "Dataset evidence" })).toBeVisible();
  await expect(page.getByText("binance-btcusdt-1h-v1")).toBeVisible();
  await expect(page.getByText(`sha256:${"b".repeat(64)}`)).toBeVisible();
  await expect(page.getByText("moving-average-crossover@1.0.0")).toBeVisible();
  await expect(page.getByText(/fastPeriod=20/)).toBeVisible();
  await expect(page.getByText(/"slowPeriod":50/)).toBeVisible();
  await expect(page.getByText("attempt-accepted-014")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Reproduction comparison inputs" })).toBeVisible();

  await page.goto(`/search?id=${sourceExperimentId}`);
  await expect(page.getByRole("heading", { name: "Source research run" })).toBeVisible();
  await page.getByRole("button", { name: "Reproduce Experiment" }).click();
  const linked = page.getByRole("link", { name: /Open reproduced Experiment/ });
  await expect(linked).toHaveAttribute("href", `/search?id=${reproductionExperimentId}`);
  expect(reproduceCalls).toBe(1);

  await linked.click();
  await expect(page.getByRole("heading", { name: "Independent reproduction" })).toBeVisible();
  await expect(
    page.getByText(new RegExp(`Linked reproduction of Experiment ${sourceExperimentId}`))
  ).toBeVisible();
  await expect(page.getByText(/Verdict:\s*MATCHED/)).toBeVisible();
  await expect(page.getByText("Ordered trades").locator("..")).toContainText("MATCHED");
});
