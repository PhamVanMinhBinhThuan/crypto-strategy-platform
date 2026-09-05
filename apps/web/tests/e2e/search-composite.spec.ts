import { expect, test, type Page, type Route } from "@playwright/test";

const descriptor = (
  strategyId: string,
  strategyVersionId: string,
  displayName: string,
  parameterName: string
) => ({
  strategyId,
  strategyVersionId,
  version: "1.0.0",
  contractVersion: "strategy-contract-v1",
  displayName,
  description: `${displayName} fixture`,
  category: "TECHNICAL",
  supportedSignals: ["BUY", "SELL", "HOLD"],
  requiredLookback: 20,
  parameters: [
    {
      name: parameterName,
      type: "INTEGER",
      required: true,
      defaultValue: "10",
      minimum: "2",
      maximum: "20",
      allowedValues: [],
      description: `${parameterName} period`
    }
  ],
  constraints: [],
  descriptorFingerprint: `strategy-descriptor-v1:${strategyId}:1.0.0`
});

const frozenDataset = {
  datasetId: "dataset-f015-e2e",
  version: "candle-v1",
  provider: "BINANCE",
  pair: "BTC/USDT",
  timeframe: "1h",
  normalizationVersion: "binance-v1",
  startTime: "2026-01-01T00:00:00Z",
  endTime: "2026-02-01T00:00:00Z",
  membershipCount: 744,
  checksum: `sha256:${"d".repeat(64)}`,
  status: "READY",
  createdAt: "2026-02-01T00:00:01Z"
};

async function fulfill(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

async function installBoundary(page: Page, capture?: (payload: unknown) => void) {
  await page.setExtraHTTPHeaders({ "x-playwright-auth-bypass": "f012-local-playwright" });
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const method = route.request().method();
    if (method === "GET" && url.pathname === "/api/v1/strategies")
      return fulfill(route, {
        items: [
          descriptor("ma-crossover", "strategy-version-ma", "Moving Average", "period"),
          descriptor("rsi", "strategy-version-rsi", "RSI", "period")
        ],
        nextCursor: null,
        hasMore: false
      });
    if (method === "GET" && url.pathname === "/api/v1/user-strategies")
      return fulfill(route, { items: [], nextCursor: null, hasMore: false });
    if (method === "GET" && url.pathname === "/api/v1/datasets")
      return fulfill(route, { items: [], nextCursor: null });
    if (method === "GET" && url.pathname === "/api/v1/search/generators")
      return fulfill(route, {
        items: [
          { generatorId: "random-search", version: "1.0.0", displayName: "Random Search" }
        ]
      });
    if (method === "POST" && url.pathname === "/api/v1/datasets") {
      const request = route.request().postDataJSON();
      expect(request).toMatchObject({
        pair: "BTC/USDT",
        timeframe: "1h",
        startTime: "2026-01-01T00:00:00.000Z",
        endTime: "2026-02-01T00:00:00.000Z"
      });
      return fulfill(route, frozenDataset, 201);
    }
    if (method === "POST" && url.pathname === "/api/v1/experiments") {
      capture?.(route.request().postDataJSON());
      return fulfill(
        route,
        {
          experimentId: "experiment-f015-e2e",
          jobId: "job-f015-e2e",
          searchRunId: "run-f015-e2e",
          status: "QUEUED",
          configurationVersion: 2,
          configurationFingerprint: `sha256:${"f".repeat(64)}`,
          monitorPath: "/search/experiment-f015-e2e"
        },
        202
      );
    }
    return fulfill(
      route,
      { code: "RESOURCE_NOT_FOUND", message: "Resource inaccessible", retryable: false },
      404
    );
  });
}

async function expectNoPageOverflow(page: Page) {
  await expect
    .poll(() =>
      page.evaluate(
        () =>
          document.documentElement.scrollWidth <= document.documentElement.clientWidth + 1 &&
          document.body.scrollWidth <= document.body.clientWidth + 1
      )
    )
    .toBe(true);
}

for (const width of [360, 768, 1024, 1440]) {
  test(`F-015 configuration remains usable at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: width === 360 ? 800 : 900 });
    await installBoundary(page);
    await page.goto("/search");

    await expect(page.getByRole("heading", { name: "Configure Experiment" })).toBeVisible();
    await expect(page.getByLabel("Pair")).toBeVisible();
    await expect(page.getByLabel("Start UTC")).toBeVisible();
    await expect(page.getByLabel("End UTC")).toBeVisible();
    await expect(page.getByRole("group", { name: "Strategy pool" })).toBeVisible();
    await expect(page.getByLabel("Maximum candidates")).toBeVisible();
    await page.getByRole("button", { name: "Start Experiment" }).scrollIntoViewIfNeeded();
    await expectNoPageOverflow(page);
  });
}

test("keyboard flow creates a frozen range and starts a two-strategy composite search", async ({
  page
}) => {
  let startPayload: Record<string, unknown> | undefined;
  await page.setViewportSize({ width: 1024, height: 900 });
  await installBoundary(page, (payload) => {
    startPayload = payload as Record<string, unknown>;
  });
  await page.goto("/search");

  await page.getByLabel("Name").fill("F-015 browser flow");
  await page.getByLabel("Pair").fill("BTC/USDT");
  await page.getByLabel("Timeframe").selectOption("1h");
  await page.getByLabel("Start UTC").fill("2026-01-01T00:00");
  await page.getByLabel("End UTC").fill("2026-02-01T00:00");
  const createDataset = page.getByRole("button", { name: "Create dataset" });
  await createDataset.focus();
  await expect(createDataset).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(page.getByText(/Frozen dataset ready with 744 candles/)).toBeVisible();

  const includeRsi = page.getByLabel("Include RSI");
  await includeRsi.focus();
  await page.keyboard.press("Space");
  await expect(includeRsi).toBeChecked();
  await page.getByLabel("Maximum components").fill("2");
  await page.getByLabel("Maximum candidates").fill("100");
  await page.getByLabel("Worker concurrency").fill("4");
  await page.getByLabel("Top-K").selectOption("10");

  const start = page.getByRole("button", { name: "Start Experiment" });
  await start.focus();
  await expect(start).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(page).toHaveURL(/\/search\/experiment-f015-e2e$/);
  await expect.poll(() => startPayload).toBeTruthy();
  expect(startPayload).toMatchObject({
    configurationVersion: 2,
    datasetId: "dataset-f015-e2e",
    searchSpace: {
      schemaVersion: 2,
      minComponents: 1,
      maxComponents: 2,
      combinationPolicy: { policyId: "majority-vote", version: "1.0.0" }
    },
    generator: { generatorId: "random-search", version: "1.0.0" },
    stopConditions: { maximumCandidates: 100 },
    topK: 10,
    requestedConcurrency: 4
  });
  const searchSpace = startPayload?.searchSpace as { strategyPool?: readonly unknown[] };
  expect(searchSpace.strategyPool).toHaveLength(2);
});
