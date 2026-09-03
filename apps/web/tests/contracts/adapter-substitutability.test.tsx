import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { MockApiClient } from "@/src/foundation/testing/mock-api-client";
import { FoundationConsumerProbe } from "@/src/foundation/testing/FoundationConsumerProbe";
describe("adapter contract", () => {
  it("consumes an ApiClient without adapter knowledge", async () => {
    render(
      <FoundationConsumerProbe client={new MockApiClient(new Map([["/probe", { label: "ok" }]]))} />
    );
    await userEvent.click(screen.getByRole("button"));
    expect(screen.getByText("Loaded")).toBeInTheDocument();
  });
});
