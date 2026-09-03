import { describe, expect, it } from "vitest";
import { readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative } from "node:path";
const files = (root: string): string[] =>
  readdirSync(root).flatMap((name) => {
    const p = join(root, name);
    return statSync(p).isDirectory() ? files(p) : /\.(ts|tsx)$/.test(p) ? [p] : [];
  });
describe("F-013 architecture boundaries", () => {
  const forbidden =
    /\bfetch\s*\(|new\s+WebSocket|@supabase|from\s+["'].*prototype|SERVICE_ROLE_KEY|DATABASE_URL|PGPASSWORD|REDIS_(URL|PASSWORD)|ioredis|\bWorker\b|Binance|provider credential|apps\/api|\.java["']/i;
  it("keeps browser feature code behind F-011 public boundaries", () => {
    for (const file of files("src/features")) {
      const source = readFileSync(file, "utf8");
      expect(source, relative(process.cwd(), file)).not.toMatch(forbidden);
    }
  });
  it("keeps production route modules free of fixture selection", () => {
    for (const file of files("app/(protected)")) {
      const source = readFileSync(file, "utf8");
      expect(source).not.toMatch(/fixtures\/|testing\/|scenario/i);
    }
  });
  it("keeps protected routes and production composition free of forbidden capabilities", () => {
    const roots = [...files("app/(protected)"), "src/foundation/composition/client-provider.tsx"];
    for (const file of roots) {
      expect(readFileSync(file, "utf8"), relative(process.cwd(), file)).not.toMatch(forbidden);
    }
    expect(readFileSync("src/foundation/composition/client-provider.tsx", "utf8")).not.toMatch(
      /development-clients.*from|MockApiClient|MockRealtimeClient|scenario/i
    );
  });
});
