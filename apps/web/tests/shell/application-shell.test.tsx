import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Sidebar } from "@/src/components/shell/Sidebar";

vi.mock("next/navigation", () => ({ usePathname: () => "/strategies" }));

describe("application navigation", () => {
  it("renders all downstream routes and marks the active route", () => {
    render(<Sidebar />);
    expect(screen.getByRole("navigation", { name: "Primary" })).toBeInTheDocument();
    expect(screen.getAllByRole("link")).toHaveLength(6);
    expect(screen.getByRole("link", { name: /Strategy Composer/ })).toHaveClass("active");
    expect(screen.getByRole("link", { name: /Market Dashboard/ })).not.toHaveClass("active");
  });
});
