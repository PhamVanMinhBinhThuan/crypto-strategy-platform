import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AsyncStatus } from "@/src/features/shared/AsyncStatus";
import { NewsFilters } from "@/src/features/news/components/NewsFilters";
describe("F-012 accessibility", () => {
  it("announces bounded async status", () => {
    render(<AsyncStatus message="Đã tải dữ liệu mới" />);
    expect(screen.getByRole("status")).toHaveAttribute("aria-live", "polite");
  });
  it("labels every News filter control", () => {
    const { container } = render(<NewsFilters selected={[]} onChange={() => {}} />);
    container.querySelectorAll("input").forEach((input) => expect(input.labels?.length).toBe(1));
  });
});
