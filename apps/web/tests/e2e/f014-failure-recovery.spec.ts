import { expect, test, type Page, type Route } from "@playwright/test";
import {
  candidatePage,
  runningExperiment,
  runningJob
} from "../../src/features/experiments/fixtures/experiment-job-fixtures";
import { emptyLeaderboard } from "../../src/features/leaderboard/fixtures/leaderboard-fixtures";

const experimentId = "experiment-013";

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

async function authorize(page: Page) {
  await page.setExtraHTTPHeaders({ "x-playwright-auth-bypass": "f012-local-playwright" });
}

test("mất realtime giữ snapshot cũ, báo stale và reconcile terminal sau reconnect", async ({
  page
}) => {
  await authorize(page);
  let recovered = false;
  let snapshotReads = 0;

  await page.addInitScript(
    ({ experimentId: browserExperimentId }) => {
      const NativeWebSocket = window.WebSocket;
      type Envelope = {
        eventType: string;
        subscriptionId: string;
        payload: Record<string, unknown>;
      };
      class F014Socket {
        static active: F014Socket[] = [];
        onopen: ((event: Event) => void) | null = null;
        onclose: ((event: CloseEvent) => void) | null = null;
        onmessage: ((event: MessageEvent) => void) | null = null;

        constructor(url: string) {
          void url;
          F014Socket.active.push(this);
          setTimeout(() => this.onopen?.(new Event("open")), 0);
        }

        send(raw: string) {
          const request = JSON.parse(raw) as Envelope;
          if (typeof request.eventType !== "string" || !request.eventType.startsWith("SUBSCRIBE_"))
            return;
          setTimeout(
            () =>
              this.onmessage?.(
                new MessageEvent("message", {
                  data: JSON.stringify({
                    eventType: "SUBSCRIPTION_CONFIRMED",
                    eventVersion: 1,
                    eventId: crypto.randomUUID(),
                    occurredAt: new Date().toISOString(),
                    correlationId: "f014-browser-recovery",
                    subscriptionId: request.subscriptionId,
                    payload: {
                      status: "ACTIVE",
                      snapshotUrl: `/api/v1/experiments/${browserExperimentId}`
                    }
                  })
                })
              ),
            0
          );
        }

        close() {}

        crash() {
          this.onclose?.({ code: 1006, reason: "simulated queue interruption" } as CloseEvent);
        }
      }

      function WebSocketProxy(url: string | URL, protocols?: string | string[]) {
        if (String(url).startsWith("ws://127.0.0.1:8080/ws")) return new F014Socket(String(url));
        return protocols === undefined
          ? new NativeWebSocket(url)
          : new NativeWebSocket(url, protocols);
      }
      Object.assign(WebSocketProxy, {
        CONNECTING: NativeWebSocket.CONNECTING,
        OPEN: NativeWebSocket.OPEN,
        CLOSING: NativeWebSocket.CLOSING,
        CLOSED: NativeWebSocket.CLOSED
      });
      Object.defineProperty(window, "WebSocket", {
        configurable: true,
        value: WebSocketProxy
      });
      Object.assign(window, {
        __f014CrashRealtime: () => F014Socket.active.at(-1)?.crash()
      });
    },
    { experimentId }
  );

  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const method = route.request().method();
    if (method === "POST" && url.pathname === "/api/v1/realtime/ticket")
      return json(route, { ticket: "single-use-browser-test-ticket" });
    if (method === "GET" && url.pathname === `/api/v1/experiments/${experimentId}`) {
      snapshotReads += 1;
      return json(
        route,
        recovered
          ? {
              ...runningExperiment,
              status: "COMPLETED",
              completedAt: "2026-09-04T05:00:05Z"
            }
          : runningExperiment
      );
    }
    if (method === "GET" && url.pathname === "/api/v1/jobs/job-search-013")
      return json(route, runningJob);
    if (method === "GET" && url.pathname === `/api/v1/experiments/${experimentId}/candidates`)
      return json(route, candidatePage);
    if (method === "GET" && url.pathname === `/api/v1/experiments/${experimentId}/leaderboard`)
      return json(route, emptyLeaderboard);
    if (method === "GET" && url.pathname === "/api/v1/strategies")
      return json(route, { items: [], nextCursor: null, hasMore: false });
    if (method === "GET" && url.pathname === "/api/v1/user-strategies")
      return json(route, { items: [], nextCursor: null, hasMore: false });
    return json(route, { code: "RESOURCE_NOT_FOUND" }, 404);
  });

  await page.goto(`/search?id=${experimentId}`);
  await expect(page.getByRole("heading", { name: runningExperiment.name })).toBeVisible();
  await expect(page.locator(".status-running")).toHaveText("RUNNING");
  await expect(page.locator(".realtime-status strong")).toHaveText("connected");

  recovered = true;
  await page.evaluate(() =>
    (window as typeof window & { __f014CrashRealtime: () => void }).__f014CrashRealtime()
  );

  await expect(page.locator(".realtime-status strong")).toHaveText("reconnecting");
  await expect(page.getByText(/snapshot is stale/)).toBeVisible();
  await expect(page.locator(".status-running")).toHaveText("RUNNING");

  await expect(page.getByText("COMPLETED", { exact: true })).toBeVisible();
  expect(snapshotReads).toBeGreaterThanOrEqual(2);
});

test("Sentiment degraded vẫn giữ News đọc được và retry về dữ liệu authoritative", async ({
  page
}) => {
  await authorize(page);
  let recovered = false;
  const item = {
    newsId: "01J00000000000000000000202",
    title: "News remains available during sentiment outage",
    source: "Demo Wire",
    url: "https://example.com/news/recovery",
    publishedAt: "2026-09-04T01:00:00Z",
    analysisStatus: "FAILED_RETRYABLE",
    relatedAssetIds: [],
    sentiment: null
  };
  await page.route("**/api/v1/news-items**", async (route) => {
    await json(route, {
      items: recovered
        ? [
            {
              ...item,
              analysisStatus: "ANALYZED",
              sentiment: { label: "POSITIVE", confidence: "0.91", polarityScore: "0.72" }
            }
          ]
        : [item],
      nextCursor: null,
      hasMore: false
    });
  });

  await page.goto("/news");
  await expect(page.getByText(item.title)).toBeVisible();
  await expect(page.getByText(/Sentiment tạm gián đoạn/)).toBeVisible();

  recovered = true;
  await page.reload();
  await expect(page.getByText("POSITIVE", { exact: true })).toBeVisible();
  await expect(page.getByText(/Sentiment tạm gián đoạn/)).toHaveCount(0);
});
