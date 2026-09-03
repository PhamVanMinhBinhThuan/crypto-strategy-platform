import { describe, it, expect, vi, beforeEach } from "vitest";
import { createSupabaseAuthClient } from "@/src/foundation/auth/supabase-auth-adapter";

const mockSignUp = vi.fn();
const mockSignInWithPassword = vi.fn();
const mockResetPasswordForEmail = vi.fn();
const mockUpdateUser = vi.fn();
const mockSignOut = vi.fn();
const mockGetSession = vi.fn();
const mockOnAuthStateChange = vi.fn();
const mockRefreshSession = vi.fn();

vi.mock("@/src/foundation/auth/supabase-browser", () => ({
  createSupabaseBrowserClient: () => ({
    auth: {
      signUp: mockSignUp,
      signInWithPassword: mockSignInWithPassword,
      resetPasswordForEmail: mockResetPasswordForEmail,
      updateUser: mockUpdateUser,
      signOut: mockSignOut,
      getSession: mockGetSession,
      onAuthStateChange: mockOnAuthStateChange,
      refreshSession: mockRefreshSession
    }
  })
}));

describe("SupabaseAuthAdapter", () => {
  let adapter: ReturnType<typeof createSupabaseAuthClient>;

  beforeEach(() => {
    vi.clearAllMocks();
    adapter = createSupabaseAuthClient();
  });

  it("handles successful sign up", async () => {
    mockSignUp.mockResolvedValueOnce({ data: {}, error: null });
    const result = await adapter.signUp("test@example.com", "password", "http://localhost");
    expect(result).toEqual({ ok: true, next: "verify-email" });
  });

  it("maps refresh success and failure to the public session contract", async () => {
    mockRefreshSession.mockResolvedValueOnce({
      data: {
        session: {
          user: { id: "u", email: "u@example.test" },
          access_token: "token",
          expires_at: 42
        }
      },
      error: null
    });
    await expect(adapter.refreshSession?.()).resolves.toEqual({
      userId: "u",
      email: "u@example.test",
      accessToken: "token",
      expiresAt: 42
    });
    mockRefreshSession.mockResolvedValueOnce({
      data: { session: null },
      error: { message: "internal" }
    });
    await expect(adapter.refreshSession?.()).resolves.toBeNull();
  });

  it("handles failed sign up securely", async () => {
    mockSignUp.mockResolvedValueOnce({ error: { message: "Some internal error" } });
    const result = await adapter.signUp("test@example.com", "password", "http://localhost");
    expect(result).toEqual({
      ok: false,
      message: "We could not complete that request. Check your details and try again."
    });
  });

  it("handles successful sign in", async () => {
    mockSignInWithPassword.mockResolvedValueOnce({ data: {}, error: null });
    const result = await adapter.signIn("test@example.com", "password");
    expect(result).toEqual({ ok: true, next: "authenticated" });
  });

  it("handles failed sign in securely", async () => {
    mockSignInWithPassword.mockResolvedValueOnce({ error: { message: "Invalid credentials" } });
    const result = await adapter.signIn("test@example.com", "password");
    expect(result).toEqual({
      ok: false,
      message: "We could not complete that request. Check your details and try again."
    });
  });

  it("returns neutral outcome on password reset request regardless of error", async () => {
    mockResetPasswordForEmail.mockResolvedValueOnce({ error: null });
    const result = await adapter.requestPasswordReset("test@example.com", "http://localhost");
    expect(result).toEqual({ ok: true, next: "login" });
  });

  it("handles successful password update and signs out", async () => {
    mockUpdateUser.mockResolvedValueOnce({ error: null });
    const result = await adapter.updatePassword("newpassword");
    expect(result).toEqual({ ok: true, next: "login" });
    expect(mockSignOut).toHaveBeenCalled();
  });

  it("handles failed password update", async () => {
    mockUpdateUser.mockResolvedValueOnce({ error: { message: "Too weak" } });
    const result = await adapter.updatePassword("newpassword");
    expect(result).toEqual({
      ok: false,
      message: "We could not complete that request. Check your details and try again."
    });
    expect(mockSignOut).not.toHaveBeenCalled();
  });
});
