import { describe, expect, it } from "vitest";
import { globSync, readFileSync } from "node:fs";
describe("F-012 production safety", () => {
  it("contains no prototype mocks, privileged access or raw error rendering", () => {
    const source = globSync("src/features/**/*.{ts,tsx}")
      .map((file) => readFileSync(file, "utf8"))
      .join("\n");

    expect(source).not.toMatch(
      /docs\/ui\/prototype|NEXT_PUBLIC_ENABLE_FIXTURES\s*=|SUPABASE_SERVICE_ROLE_KEY|DATABASE_URL/
    );
    expect(source).not.toMatch(/\.error\.message/);
  });
});
