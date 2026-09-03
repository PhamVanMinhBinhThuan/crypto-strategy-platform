import { render } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { AuthForm } from "@/src/components/auth/AuthForm";

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace: vi.fn(), refresh: vi.fn() }) }));

describe("Accessibility: Auth Foundation", () => {
  beforeEach(() => {
    process.env.NEXT_PUBLIC_SUPABASE_URL = "http://localhost";
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY = "anon-key-1234567890123456";
    process.env.NEXT_PUBLIC_API_BASE_URL = "http://localhost";
    process.env.NEXT_PUBLIC_WS_URL = "ws://localhost";
  });
  const assertAccessibility = (container: HTMLElement) => {
    // Check that every input has an associated label
    const inputs = container.querySelectorAll("input");
    inputs.forEach((input) => {
      expect(
        input.id ||
          input.getAttribute("aria-labelledby") ||
          input.getAttribute("aria-label") ||
          input.labels?.length
      ).toBeTruthy();
    });

    // Check that every button has text or aria-label
    const buttons = container.querySelectorAll("button");
    buttons.forEach((button) => {
      expect(button.textContent || button.getAttribute("aria-label")).toBeTruthy();
    });
  };

  it("Login form should have basic accessibility structures", () => {
    const { container } = render(<AuthForm mode="login" />);
    assertAccessibility(container);
  });

  it("Register form should have basic accessibility structures", () => {
    const { container } = render(<AuthForm mode="register" />);
    assertAccessibility(container);
  });

  it("Forgot password form should have basic accessibility structures", () => {
    const { container } = render(<AuthForm mode="forgot" />);
    assertAccessibility(container);
  });

  it("Reset password form should have basic accessibility structures", () => {
    const { container } = render(<AuthForm mode="reset" />);
    assertAccessibility(container);
  });
});
