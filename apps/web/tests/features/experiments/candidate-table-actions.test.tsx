import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ViewDetailsAction } from "@/src/features/experiments/components/CandidateTableParts";

describe("Candidate table actions", () => {
  it("opens the candidate Backtest result when one exists", () => {
    render(
      <ViewDetailsAction
        experimentId="experiment-013"
        candidateId="candidate-013"
        backtestResultId="result-013"
        candidateNumber={1}
        view="RESULTS"
      />
    );

    expect(screen.getByRole("link", { name: "View details for Candidate #1" })).toHaveAttribute(
      "href",
      "/backtests?resultId=result-013&returnTo=%2Fsearch%2Fexperiment-013%3Fview%3Dresults"
    );
  });

  it("keeps the diagnostic candidate detail for candidates without a Backtest result", () => {
    render(
      <ViewDetailsAction
        experimentId="experiment-013"
        candidateId="candidate-013"
        backtestResultId={null}
        candidateNumber={1}
        view="FAILED"
      />
    );

    expect(screen.getByRole("link", { name: "View details for Candidate #1" })).toHaveAttribute(
      "href",
      "/search/experiment-013?view=failed&candidateId=candidate-013"
    );
  });

  it("keeps failed candidates in the diagnostic popup even if Backtest evidence exists", () => {
    render(
      <ViewDetailsAction
        experimentId="experiment-013"
        candidateId="candidate-013"
        backtestResultId="result-013"
        failed
        candidateNumber={1}
        view="ALL"
      />
    );

    expect(screen.getByRole("link", { name: "View details for Candidate #1" })).toHaveAttribute(
      "href",
      "/search/experiment-013?view=all&candidateId=candidate-013"
    );
  });
});
