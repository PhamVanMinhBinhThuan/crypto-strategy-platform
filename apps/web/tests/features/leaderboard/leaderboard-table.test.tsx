import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { LeaderboardTable } from "@/src/features/leaderboard/components/LeaderboardTable";
import { mapLeaderboard } from "@/src/features/leaderboard/mappers/leaderboard-mapper";
import {
  emptyLeaderboard,
  leaderboardPage
} from "@/src/features/leaderboard/fixtures/leaderboard-fixtures";
describe("Leaderboard table", () => {
  it("renders exactly the six released data columns and result action", () => {
    render(<LeaderboardTable snapshot={mapLeaderboard(leaderboardPage)} />);
    const headers = screen.getAllByRole("columnheader").map((x) => x.textContent);
    expect(headers.slice(0, 6)).toEqual([
      "Rank",
      "Evaluation Result ID",
      "Backtest Result ID",
      "Score",
      "Maximum Drawdown",
      "Evaluation Fingerprint"
    ]);
    expect(screen.queryByText(/Sharpe|Win Rate|Trades|Total Return/)).not.toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: "View Backtest" })[0]).toHaveAttribute(
      "href",
      "/backtests?resultId=result-013"
    );
    expect(screen.getByRole("region")).toHaveClass("table-scroll");
  });
  it("renders the released empty state", () => {
    render(<LeaderboardTable snapshot={mapLeaderboard(emptyLeaderboard)} />);
    expect(screen.getByText(/No strategy candidates evaluated yet/)).toBeInTheDocument();
  });
});
