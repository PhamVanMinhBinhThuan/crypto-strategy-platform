import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Sidebar } from "@/src/components/shell/Sidebar";

vi.mock("next/navigation", () => ({ usePathname: () => "/strategies" }));
vi.mock("@/src/components/shell/AccountMenu", () => ({
  AccountMenu: () => <button aria-label="Sign out">Sign out</button>
}));

describe("application navigation", () => {
  it("renders all downstream routes and marks the active route", () => {
    render(<Sidebar />);
    expect(screen.getByRole("navigation", { name: "Primary" })).toBeInTheDocument();
    expect(screen.getAllByRole("link")).toHaveLength(6);
    expect(screen.getByRole("link", { name: /Strategy Composer/ })).toHaveClass("active");
    expect(screen.getByRole("link", { name: /Market Dashboard/ })).not.toHaveClass("active");
    expect(
      screen.getByRole("button", { name: "Sign out" }).closest(".sidebar-footer")
    ).not.toBeNull();
  });

  it("lets the user collapse and expand the sidebar", async () => {
    const user = userEvent.setup();
    const { container } = render(<Sidebar />);

    await user.click(screen.getByRole("button", { name: "Collapse sidebar" }));
    expect(container.querySelector(".sidebar")).toHaveClass("is-collapsed");
    expect(screen.getByRole("button", { name: "Expand sidebar" })).toHaveAttribute(
      "aria-expanded",
      "false"
    );

    await user.click(screen.getByRole("button", { name: "Expand sidebar" }));
    expect(container.querySelector(".sidebar")).not.toHaveClass("is-collapsed");
  });
});
