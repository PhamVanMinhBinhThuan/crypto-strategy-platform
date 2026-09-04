import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SessionProvider, useSession } from "@/src/foundation/auth/SessionProvider";
import type { AuthClient, AuthSession } from "@/src/foundation/auth/contracts";
import { recoverAuthentication } from "@/src/foundation/auth/session-lifecycle";

const authenticated: AuthSession = {
  userId: "user-1",
  email: "trader@example.com",
  accessToken: "public-session-token",
  expiresAt: 2_000_000_000
};

function Probe() {
  const { state, session } = useSession();
  return <output>{`${state}:${session?.email ?? "none"}`}</output>;
}

function client(initial: AuthSession | null) {
  let listener: (session: AuthSession | null) => void = () => undefined;
  const value: AuthClient = {
    session: vi.fn().mockResolvedValue(initial),
    signUp: vi.fn(),
    signIn: vi.fn(),
    requestPasswordReset: vi.fn(),
    updatePassword: vi.fn(),
    signOut: vi.fn().mockResolvedValue(undefined),
    subscribe: vi.fn((next) => {
      listener = next;
      return vi.fn();
    })
  };
  return { value, emit: (session: AuthSession | null) => listener(session) };
}

describe("SessionProvider", () => {
  it("returns a refreshed session through the shared recovery boundary", async () => {
    const auth = client(null);
    auth.value.refreshSession = vi.fn().mockResolvedValue(authenticated);
    await expect(recoverAuthentication(auth.value)).resolves.toEqual(authenticated);
    expect(auth.value.refreshSession).toHaveBeenCalledTimes(1);
    expect(auth.value.signOut).not.toHaveBeenCalled();
  });
  it("bootstraps one session and reacts to cross-tab/auth expiry events", async () => {
    const auth = client(authenticated);
    render(
      <SessionProvider client={auth.value}>
        <Probe />
      </SessionProvider>
    );
    expect(screen.getByText("loading:none")).toBeInTheDocument();
    await screen.findByText("authenticated:trader@example.com");
    auth.emit(null);
    await waitFor(() => expect(screen.getByText("unauthenticated:none")).toBeInTheDocument());
    expect(auth.value.session).toHaveBeenCalledTimes(1);
    expect(auth.value.subscribe).toHaveBeenCalledTimes(1);
  });
});
