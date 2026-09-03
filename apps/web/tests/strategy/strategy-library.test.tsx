import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { StrategyCatalog } from "@/src/features/strategy/components/StrategyCatalog";
describe("Strategy catalog states", () => {
  it("keeps system success visible when private library fails", () => {
    render(
      <StrategyCatalog
        system={[
          {
            strategyId: "ma",
            strategyVersionId: "v1",
            version: "1",
            contractVersion: "1",
            displayName: "Moving Average",
            description: "",
            category: "TREND",
            supportedSignals: ["BUY"],
            requiredLookback: 2,
            parameters: [],
            constraints: [],
            descriptorFingerprint: "fp"
          }
        ]}
        owned={[]}
        loadingSystem={false}
        loadingOwned={false}
        ownedError="Không tải được thư viện riêng."
        onSelectSystem={vi.fn()}
        onSelectOwned={vi.fn()}
      />
    );
    expect(screen.getByText("Moving Average")).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent("thư viện riêng");
  });
});
