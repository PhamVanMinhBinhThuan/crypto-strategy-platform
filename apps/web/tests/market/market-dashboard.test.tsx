import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { MarketConnectionStatus } from "@/src/features/market/components/MarketConnectionStatus";
describe("Market dashboard states", () => {
  it("does not claim live unless transport and provider are connected", () => {
    const { rerender } = render(
      <MarketConnectionStatus transport="reconnecting" provider="CONNECTED" />
    );
    expect(screen.getByRole("status")).toHaveTextContent("Dữ liệu lưu gần nhất");
    rerender(<MarketConnectionStatus transport="connected" provider="CONNECTED" />);
    expect(screen.getByRole("status")).toHaveTextContent("Live");
  });

  it("shows the authoritative freshness timestamp without claiming transport liveness", () => {
    render(
      <MarketConnectionStatus
        transport="reconnecting"
        provider="DISCONNECTED"
        lastDataAt="2026-09-04T02:00:00Z"
      />
    );

    expect(screen.getByRole("status")).toHaveTextContent("Dữ liệu lưu gần nhất");
    expect(screen.getByRole("time")).toHaveAttribute("dateTime", "2026-09-04T02:00:00Z");
  });
});
