import type { AuthClient, AuthResult, AuthSession } from "./contracts";
import { createSupabaseBrowserClient } from "./supabase-browser";

const SAFE_AUTH_ERROR = "We could not complete that request. Check your details and try again.";
const sessionOf = (
  session: {
    user: { id: string; email?: string };
    access_token: string;
    expires_at?: number;
  } | null
): AuthSession | null =>
  session
    ? {
        userId: session.user.id,
        email: session.user.email ?? "",
        accessToken: session.access_token,
        expiresAt: session.expires_at ?? 0
      }
    : null;
export function createSupabaseAuthClient(): AuthClient {
  const client = createSupabaseBrowserClient();
  return {
    async session() {
      return sessionOf((await client.auth.getSession()).data.session);
    },
    async refreshSession() {
      const { data, error } = await client.auth.refreshSession();
      return error ? null : sessionOf(data.session);
    },
    async signUp(email, password, redirectTo) {
      const { error } = await client.auth.signUp({
        email,
        password,
        options: { emailRedirectTo: redirectTo }
      });
      return error ? { ok: false, message: SAFE_AUTH_ERROR } : { ok: true, next: "verify-email" };
    },
    async signIn(email, password) {
      const { error } = await client.auth.signInWithPassword({ email, password });
      return error ? { ok: false, message: SAFE_AUTH_ERROR } : { ok: true, next: "authenticated" };
    },
    async requestPasswordReset(email, redirectTo) {
      await client.auth.resetPasswordForEmail(email, { redirectTo });
      return { ok: true, next: "login" };
    },
    async updatePassword(password) {
      const { error } = await client.auth.updateUser({ password });
      if (error) return { ok: false, message: SAFE_AUTH_ERROR };
      await client.auth.signOut();
      return { ok: true, next: "login" };
    },
    async signOut() {
      await client.auth.signOut();
    },
    subscribe(listener) {
      const { data } = client.auth.onAuthStateChange((_event, session) =>
        listener(sessionOf(session))
      );
      return () => data.subscription.unsubscribe();
    }
  };
}
export const neutralAuthError = (): AuthResult => ({ ok: false, message: SAFE_AUTH_ERROR });
