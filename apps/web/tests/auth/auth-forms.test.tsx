import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthForm } from "@/src/components/auth/AuthForm";

const replace = vi.fn();
const signUp = vi.fn();
const signIn = vi.fn();
const requestPasswordReset = vi.fn();
const updatePassword = vi.fn();

vi.mock("next/navigation", () => ({ useRouter: () => ({ replace, refresh: vi.fn() }) }));
vi.mock("@/src/foundation/auth/supabase-auth-adapter", () => ({
  createSupabaseAuthClient: () => ({ signUp, signIn, requestPasswordReset, updatePassword })
}));

describe("auth forms", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    signUp.mockResolvedValue({ ok: true, next: "verify-email" });
    signIn.mockResolvedValue({ ok: true, next: "authenticated" });
    requestPasswordReset.mockResolvedValue({ ok: true, next: "login" });
    updatePassword.mockResolvedValue({ ok: true, next: "login" });
  });

  it("blocks mismatched registration passwords before calling the adapter", async () => {
    const user = userEvent.setup();
    render(<AuthForm mode="register" />);
    await user.type(screen.getByLabelText("Email address"), "new@example.com");
    await user.type(screen.getByLabelText("Password"), "password-1");
    await user.type(screen.getByLabelText("Confirm password"), "password-2");
    await user.click(screen.getByRole("button", { name: "Create your account" }));
    expect(screen.getByText("Passwords do not match.")).toBeInTheDocument();
    expect(signUp).not.toHaveBeenCalled();
  });

  it("uses the safe internal destination after login", async () => {
    const user = userEvent.setup();
    render(<AuthForm mode="login" next="https://attacker.example" />);
    await user.type(screen.getByLabelText("Email address"), "user@example.com");
    await user.type(screen.getByLabelText("Password"), "password-1");
    await user.click(screen.getByRole("button", { name: "Welcome back" }));
    expect(replace).toHaveBeenCalledWith("/market");
  });

  it("returns a neutral recovery message", async () => {
    const user = userEvent.setup();
    render(<AuthForm mode="forgot" />);
    await user.type(screen.getByLabelText("Email address"), "unknown@example.com");
    await user.click(screen.getByRole("button", { name: "Reset your password" }));
    expect(screen.getByText(/If an account can receive email/)).toBeInTheDocument();
  });
});
