export type AuthSession = Readonly<{
  userId: string;
  email: string;
  accessToken: string;
  expiresAt: number;
}>;
export type AuthState = "loading" | "unauthenticated" | "authenticated" | "refreshing";
export type AuthResult =
  { ok: true; next: "verify-email" | "login" | "authenticated" } | { ok: false; message: string };

export interface AuthClient {
  session(): Promise<AuthSession | null>;
  signUp(email: string, password: string, redirectTo: string): Promise<AuthResult>;
  signIn(email: string, password: string): Promise<AuthResult>;
  requestPasswordReset(email: string, redirectTo: string): Promise<AuthResult>;
  updatePassword(password: string): Promise<AuthResult>;
  signOut(): Promise<void>;
  subscribe(listener: (session: AuthSession | null) => void): () => void;
}
