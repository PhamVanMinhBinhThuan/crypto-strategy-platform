import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { globSync } from "node:fs";
describe("foundation boundaries", () => {
  it("does not contain privileged credentials in source", () => {
    const files = globSync("{app,src}/**/*.{ts,tsx}");
    const source = files.map((file) => readFileSync(file, "utf8")).join("\n");
    expect(source).not.toMatch(/service[_-]?role(?:_key)?\s*[:=]\s*["'][^"']{10,}/i);
    expect(source).not.toMatch(/PGPASSWORD\s*[:=]\s*['\"]/);
  });
});
