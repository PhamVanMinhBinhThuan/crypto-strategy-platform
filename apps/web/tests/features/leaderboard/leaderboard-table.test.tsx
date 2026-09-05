import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { LeaderboardTable } from "@/src/features/leaderboard/components/LeaderboardTable";
import { mapLeaderboard } from "@/src/features/leaderboard/mappers/leaderboard-mapper";
import {
  emptyLeaderboard,
  leaderboardPage
} from "@/src/features/leaderboard/fixtures/leaderboard-fixtures";
describe("Leaderboard table", () => {
  it("renders composite identity and the four authoritative released metrics", () => {
    render(<LeaderboardTable snapshot={mapLeaderboard(leaderboardPage)} />);
    const headers = screen.getAllByRole("columnheader").map((x) => x.textContent);
    expect(headers.slice(0, 7)).toEqual([
      "Rank",
      "Candidate",
      "Score",
      "Total Return",
      "Win Rate",
      "Maximum Drawdown",
      "Trades"
    ]);
    expect(screen.queryByText(/Sharpe/)).not.toBeInTheDocument();
    expect(screen.getByText("ma-crossover + rsi")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Candidate detail" })).toHaveAttribute(
      "href",
      "/search/experiment-013?candidateId=candidate-013"
    );
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
