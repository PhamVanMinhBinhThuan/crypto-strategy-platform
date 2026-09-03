import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ExperimentStatus } from "@/src/features/experiments/components/ExperimentStatus";
import { JobProgressList } from "@/src/features/experiments/components/JobProgressList";
import { CandidateDiscoveryTimeline } from "@/src/features/experiments/components/CandidateDiscoveryTimeline";
import {
  candidatePage,
  experimentStates,
  jobStates,
  runningExperiment,
  runningJob
} from "@/src/features/experiments/fixtures/experiment-job-fixtures";
import {
  mapCandidatePage,
  mapExperiment,
  mapJob
} from "@/src/features/experiments/mappers/experiment-job-mappers";
describe("Search progress presentation", () => {
  it("renders lifecycle, raw counts, ratio, exact score and candidate metadata", () => {
    render(
      <>
        <ExperimentStatus experiment={mapExperiment(runningExperiment)} />
        <JobProgressList jobs={[mapJob(runningJob), mapJob(jobStates[2])]} />
        <CandidateDiscoveryTimeline candidates={mapCandidatePage(candidatePage).items} />
      </>
    );
    expect(screen.getAllByText("RUNNING")).toHaveLength(2);
    expect(screen.getAllByRole("progressbar")).toHaveLength(2);
    for (const progress of screen.getAllByRole("progressbar")) {
      expect(progress).toHaveAccessibleName("42 completed, 2 failed, 100 total");
    }
    expect(screen.getAllByText("0.873400000000000001")).toHaveLength(2);
    expect(screen.getByText(/Generation 42/)).toBeInTheDocument();
    expect(screen.getByText(/Retry scheduled/)).toBeInTheDocument();
  });
  it("shows safe terminal failure in text, not color alone", () => {
    render(<ExperimentStatus experiment={mapExperiment(experimentStates[6])} />);
    expect(screen.getByRole("alert")).toHaveTextContent("JOB_EXECUTION_TIMEOUT");
  });
  it("separates ephemeral discovery hints", () => {
    render(<CandidateDiscoveryTimeline candidates={[]} discoveries={["result-013"]} />);
    expect(screen.getByText(/Freshness notification/)).toBeInTheDocument();
  });
});
