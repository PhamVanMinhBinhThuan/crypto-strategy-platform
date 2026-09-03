import type { AuthClient, AuthSession } from "./contracts";
import { clearPrivateClientState } from "./logout";

let sessionPromise: Promise<AuthSession | null> | null = null;
const logoutChannel = typeof window !== "undefined" ? new BroadcastChannel("auth_sync") : null;

if (logoutChannel) {
  logoutChannel.onmessage = (event) => {
    if (event.data === "LOGOUT") {
      // eslint-disable-next-line @next/next/no-location-assign-relative-destination
      window.location.href = "/login?expired=true";
    }
  };
}

export async function resolveSession(client: AuthClient): Promise<AuthSession | null> {
  if (sessionPromise) return sessionPromise;

  sessionPromise = client.session().finally(() => {
    sessionPromise = null;
  });

  return sessionPromise;
}

export async function handleAuthenticationFailure(client: AuthClient): Promise<void> {
  await clearPrivateClientState();
  await client.signOut();
  if (logoutChannel) {
    logoutChannel.postMessage("LOGOUT");
  }
  // eslint-disable-next-line @next/next/no-location-assign-relative-destination
  window.location.href = "/login?expired=true";
}
