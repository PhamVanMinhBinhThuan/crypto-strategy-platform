import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { LeaderboardControls } from "@/src/features/leaderboard/components/LeaderboardControls";
import { capLeaderboardLimit } from "@/src/features/leaderboard/types/leaderboard";
describe("Leaderboard controls", () => {
  it("offers 10/25/50 presets when configured and starts at 10", () => {
    render(<LeaderboardControls limit={10} configuredTopK={100} onChange={() => {}} />);
    expect(screen.getByLabelText("Top-K")).toHaveValue("10");
    expect(screen.getAllByRole("option").map((x) => x.textContent)).toEqual([
      "Top 10",
      "Top 25",
      "Top 50"
    ]);
  });
  it("caps custom limits by 1-100 and configured Top-K", () => {
    expect(capLeaderboardLimit(-1, 25)).toBe(1);
    expect(capLeaderboardLimit(101, 100)).toBe(100);
    expect(capLeaderboardLimit(50, 25)).toBe(25);
  });
  it("emits accessible custom selection", async () => {
    const change = vi.fn();
    render(<LeaderboardControls limit={10} configuredTopK={25} onChange={change} />);
    await userEvent.setup().clear(screen.getByLabelText("Custom"));
    await userEvent.setup().type(screen.getByLabelText("Custom"), "25");
    expect(change).toHaveBeenLastCalledWith(25);
  });
});
