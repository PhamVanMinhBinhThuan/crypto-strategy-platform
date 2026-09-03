import { globSync, readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";

describe("browser secret safety", () => {
  it("contains no privileged credential assignment or raw internal error rendering", () => {
    const files = globSync(["app/**/*.{ts,tsx}", "src/**/*.{ts,tsx}"], { cwd: process.cwd() });
    const source = files.map((file) => readFileSync(file, "utf8")).join("\n");
    expect(source).not.toMatch(/(?:SERVICE_ROLE_KEY|DATABASE_URL|PGPASSWORD)\s*[:=]\s*["']/);
    expect(source).not.toMatch(/\.stack\b|error\.message\s*}/);
  });
});
