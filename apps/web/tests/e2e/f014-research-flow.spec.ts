import { expect, test, type Page, type Route } from "@playwright/test";
import { normalBacktestResult } from "../../src/features/backtests/fixtures/backtest-result-fixtures";
import {
  candidatePage,
  runningExperiment,
  runningJob
} from "../../src/features/experiments/fixtures/experiment-job-fixtures";
import { leaderboardPage } from "../../src/features/leaderboard/fixtures/leaderboard-fixtures";

const ids = {
  single: "01J00000000000000000000101",
  singleVersion: "01J00000000000000000000102",
  composite: "01J00000000000000000000103",
  compositeVersion: "01J00000000000000000000104"
};

const integer = (name: string, value: string) => ({
  name,
  type: "INTEGER",
  required: true,
  defaultValue: value,
  minimum: "2",
  maximum: "500",
  allowedValues: [],
  description: name
});

const descriptor = (
  strategyId: string,
  strategyVersionId: string,
  displayName: string,
  category: string,
  parameters: ReturnType<typeof integer>[]
) => ({
  strategyId,
  strategyVersionId,
  version: "1.0.0",
  contractVersion: "strategy-contract-v1",
  displayName,
  description: `${displayName} contract fixture`,
  category,
  supportedSignals: ["BUY", "SELL", "HOLD"],
  requiredLookback: 25,
  parameters,
  constraints: [],
  descriptorFingerprint: `strategy-descriptor-v1:${strategyId}:1.0.0`
});

const systemStrategies = [
  descriptor("ma-crossover", "01J00000000000000000000000", "Moving Average Crossover", "TREND", [
    integer("fastPeriod", "5"),
    integer("slowPeriod", "25")
  ]),
  descriptor("rsi-threshold", "01J00000000000000000000010", "RSI Threshold", "MOMENTUM", [
    integer("period", "14")
  ]),
  descriptor("bollinger-bands", "01J00000000000000000000020", "Bollinger Bands", "VOLATILITY", [
    integer("period", "20")
  ]),
  descriptor(
    "support-resistance",
    "01J00000000000000000000030",
    "Support / Resistance",
    "STRUCTURE",
    [integer("lookback", "20")]
  )
];

const selection = (index: number, parameters: Record<string, string>) => ({
  strategyId: systemStrategies[index].strategyId,
  strategyVersionId: systemStrategies[index].strategyVersionId,
  version: "1.0.0",
  parameters
});

const singleStrategy = {
  userStrategyId: ids.single,
  kind: "SINGLE",
  name: "MA cá nhân",
  description: "Tạo từ Strategy nền",
  status: "ACTIVE",
  archivedAt: null,
  createdAt: "2026-09-04T01:00:00Z",
  updatedAt: "2026-09-04T01:00:00Z",
  latestVersion: {
    userStrategyVersionId: ids.singleVersion,
    userStrategyId: ids.single,
    versionNo: 1,
    kind: "SINGLE",
    source: {
      type: "SINGLE",
      strategy: selection(0, { fastPeriod: "5", slowPeriod: "25" })
    },
    status: "DRAFT",
    fingerprint: "strategy-v1:single",
    publishedAt: null,
    createdAt: "2026-09-04T01:00:00Z"
  }
};

const compositeStrategy = {
  userStrategyId: ids.composite,
  kind: "COMPOSITE",
  name: "Composite demo",
  description: "MA và RSI, majority vote giải quyết xung đột",
  status: "ACTIVE",
  archivedAt: null,
  createdAt: "2026-09-04T01:01:00Z",
  updatedAt: "2026-09-04T01:01:00Z",
  latestVersion: {
    userStrategyVersionId: ids.compositeVersion,
    userStrategyId: ids.composite,
    versionNo: 1,
    kind: "COMPOSITE",
    source: {
      type: "COMPOSITE",
      policyId: "majority-vote",
      policyVersion: "1.0.0",
      policyParameters: {},
      components: [
        selection(0, { fastPeriod: "5", slowPeriod: "25" }),
        selection(1, { period: "14" })
      ]
    },
    status: "DRAFT",
    fingerprint: "strategy-v1:composite",
    publishedAt: null,
    createdAt: "2026-09-04T01:01:00Z"
  }
};

const publishedComposite = () => ({
  ...compositeStrategy,
  latestVersion: {
    ...compositeStrategy.latestVersion,
    status: "PUBLISHED",
    publishedAt: "2026-09-04T01:02:00Z"
  }
});

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

async function installResearchBoundary(page: Page) {
  let owned: (typeof singleStrategy | typeof compositeStrategy)[] = [];
  let compositeIsPublished = false;
  await page.setExtraHTTPHeaders({ "x-playwright-auth-bypass": "f012-local-playwright" });
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();

    if (method === "GET" && url.pathname === "/api/v1/strategies")
      return json(route, { items: systemStrategies, nextCursor: null, hasMore: false });
    if (method === "GET" && url.pathname === "/api/v1/user-strategies")
      return json(route, {
        items: owned.map(({ userStrategyId, kind, name, description, createdAt }) => ({
          userStrategyId,
          kind,
          name,
          description,
          createdAt
        })),
        nextCursor: null,
        hasMore: false
      });
    if (method === "GET" && url.pathname === `/api/v1/user-strategies/${ids.single}`)
      return json(route, singleStrategy);
    if (method === "GET" && url.pathname === `/api/v1/user-strategies/${ids.composite}`)
      return json(route, compositeIsPublished ? publishedComposite() : compositeStrategy);
    if (method === "POST" && url.pathname === "/api/v1/user-strategies") {
      const draft = request.postDataJSON() as Record<string, unknown>;
      const source = draft.source as Record<string, unknown> | undefined;
      if (draft.kind === "SINGLE") {
        const strategy = source?.strategy as Record<string, unknown> | undefined;
        const values = (strategy?.parameters ?? {}) as Record<string, unknown>;
        if (typeof values.fastPeriod !== "number" || typeof values.slowPeriod !== "number")
          return json(route, { code: "STRATEGY_PARAMETERS_INVALID" }, 422);
        owned = [...owned, singleStrategy];
        return json(route, singleStrategy, 201);
      }
      if (
        source?.policyId !== "majority-vote" ||
        source.policyVersion !== "1.0.0" ||
        !Array.isArray(source.components) ||
        source.components.length < 2
      )
        return json(route, { code: "STRATEGY_PARAMETERS_INVALID" }, 422);
      owned = [...owned, compositeStrategy];
      return json(route, compositeStrategy, 201);
    }
    if (
      method === "POST" &&
      url.pathname ===
        `/api/v1/user-strategies/${ids.composite}/versions/${ids.compositeVersion}/publish`
    ) {
      compositeIsPublished = true;
      return json(route, publishedComposite().latestVersion);
    }
    if (method === "POST" && url.pathname === "/api/v1/experiments") {
      const command = request.postDataJSON() as Record<string, unknown>;
      if (command.userStrategyVersionId !== ids.compositeVersion)
        return json(route, { code: "STRATEGY_PARAMETERS_INVALID" }, 422);
      return json(
        route,
        {
          experimentId: "experiment-013",
          jobId: "job-search-013",
          status: "QUEUED"
        },
        202
      );
    }
    if (method === "GET" && url.pathname === "/api/v1/experiments/experiment-013")
      return json(route, runningExperiment);
    if (method === "GET" && url.pathname === "/api/v1/jobs/job-search-013")
      return json(route, runningJob);
    if (method === "GET" && url.pathname === "/api/v1/experiments/experiment-013/candidates")
      return json(route, candidatePage);
    if (method === "GET" && url.pathname === "/api/v1/experiments/experiment-013/leaderboard")
      return json(route, leaderboardPage);
    if (method === "GET" && url.pathname.startsWith("/api/v1/backtest-results/"))
      return json(route, { ...normalBacktestResult, backtestId: null });
    return json(route, { code: "RESOURCE_NOT_FOUND" }, 404);
  });
}

test("tạo Strategy cá nhân, publish composite rồi dùng trong Search và mở Result", async ({
  page
}) => {
  await installResearchBoundary(page);
  await page.goto("/strategies");

  await page.getByRole("button", { name: /Moving Average Crossover/ }).click();
  await page.getByLabel("Tên Strategy").fill("MA cá nhân");
  await page.getByRole("button", { name: "Lưu Strategy" }).click();
  await expect(page.getByRole("heading", { name: "MA cá nhân" })).toBeVisible();

  await page.getByRole("button", { name: /Moving Average Crossover/ }).click();
  await page.getByLabel("Tên Strategy").fill("Composite demo");
  await page.getByLabel("Composite").check();
  await page.getByLabel(/Moving Average Crossover · v1.0.0/).check();
  await page.getByLabel(/RSI Threshold · v1.0.0/).check();
  await page.getByRole("button", { name: "Lưu Strategy" }).click();
  await page.getByRole("button", { name: /Composite demo/ }).click();
  await expect(page.getByRole("heading", { name: "Composite demo" })).toBeVisible();
  await expect(page.getByText(/Quy tắc xung đột: majority-vote/)).toBeVisible();

  page.once("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Publish version" }).click();
  await expect(page.getByText(/1 · PUBLISHED/)).toBeVisible();

  await page.goto("/search");
  await page.getByLabel("Name").fill("F014 composite search");
  await page.getByLabel("Dataset ID", { exact: true }).fill("dataset-btc-1h");
  await page.getByLabel("Strategy", { exact: true }).selectOption({ label: "Composite demo" });
  await page.getByLabel("Maximum candidates").fill("1");
  await page.getByRole("button", { name: "Start Experiment" }).click();

  const experiment = page.getByRole("link", { name: /Open Experiment/ });
  await expect(experiment).toBeVisible();
  await experiment.click();
  const result = page.getByRole("link", { name: "View Backtest" }).first();
  await expect(result).toBeVisible();
  await result.click();
  await expect(page).toHaveURL(/\/backtests\?resultId=/);
  await expect(page.getByText("Total Return")).toBeVisible();
  await expect(page.getByText(/Entry\/Exit evidence/)).toBeVisible();
  await expect(page.getByRole("region", { name: "Scrollable trade history" })).toBeVisible();
});
