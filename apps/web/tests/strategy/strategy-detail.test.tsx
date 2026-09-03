import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { StrategyDetail } from "@/src/features/strategy/components/StrategyDetail";
import { StrategyActions } from "@/src/features/strategy/components/StrategyActions";
const owned = {
  userStrategyId: "s1",
  kind: "SINGLE",
  name: "Private MA",
  description: "",
  status: "ACTIVE",
  archivedAt: null,
  createdAt: "2026-09-03T00:00:00Z",
  updatedAt: "2026-09-03T00:00:00Z",
  latestVersion: {
    userStrategyVersionId: "v1",
    userStrategyId: "s1",
    versionNo: 1,
    kind: "SINGLE",
    source: {
      type: "SINGLE",
      strategy: { strategyId: "ma", strategyVersionId: "sv1", version: "1", parameters: {} }
    },
    status: "PUBLISHED",
    fingerprint: "fp",
    publishedAt: "2026-09-03T00:00:00Z",
    createdAt: "2026-09-03T00:00:00Z"
  }
} as const;
describe("Strategy detail", () => {
  it("marks published versions immutable", () => {
    render(<StrategyDetail owned={owned} />);
    expect(screen.getByRole("note")).toHaveTextContent("bất biến");
  });
  it("requires explicit archive confirmation", async () => {
    const archive = vi.fn(),
      confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    render(
      <StrategyActions
        canPublish={false}
        archived={false}
        pending={false}
        onPublish={vi.fn()}
        onArchive={archive}
        onNewVersion={vi.fn()}
      />
    );
    await userEvent.click(screen.getByRole("button", { name: "Archive" }));
    expect(archive).not.toHaveBeenCalled();
    confirm.mockReturnValue(true);
    await userEvent.click(screen.getByRole("button", { name: "Archive" }));
    expect(archive).toHaveBeenCalledOnce();
    confirm.mockRestore();
  });
});
