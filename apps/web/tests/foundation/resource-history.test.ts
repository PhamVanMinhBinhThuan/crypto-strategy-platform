import { describe, expect, it } from "vitest";
import { safeExperimentReturnUrl } from "@/src/foundation/navigation/resource-history";

describe("experiment return navigation", () => {
  it("returns to the experiment tab without reopening candidate details", () => {
    expect(
      safeExperimentReturnUrl(
        "/search/experiment-013?view=failed&candidateId=candidate-013",
        "experiment-013",
        "candidate-013"
      )
    ).toBe("/search/experiment-013?view=failed");
  });

  it("uses a clean Results URL when no return location was supplied", () => {
    expect(safeExperimentReturnUrl(undefined, "experiment-013", "candidate-013")).toBe(
      "/search/experiment-013?view=results"
    );
  });
});
