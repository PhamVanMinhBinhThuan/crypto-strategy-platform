import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
const read = (path: string) => readFileSync(resolve(process.cwd(), path), "utf8");
describe("F-013 route foundation", () => {
  it("composes both routes inside the single protected F-011 shell", () => {
    const layout = read("app/(protected)/layout.tsx");
    expect(layout).toContain("<ClientProvider>");
    expect(layout).toContain("<ApplicationShell>");
    expect(read("app/(protected)/backtests/page.tsx")).toContain("BacktestResultsView");
    expect(read("app/(protected)/search/page.tsx")).toContain("SearchView");
  });
  it("does not duplicate shell or client ownership in route pages", () => {
    const pages =
      read("app/(protected)/backtests/page.tsx") + read("app/(protected)/search/page.tsx");
    expect(pages).not.toMatch(
      /createApiClient|createRealtimeClient|ApplicationShell|new WebSocket|fetch\s*\(/
    );
  });
});
