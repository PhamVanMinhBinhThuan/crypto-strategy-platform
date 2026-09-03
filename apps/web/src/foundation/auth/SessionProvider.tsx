"use client";
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { AuthClient, AuthSession, AuthState } from "./contracts";
import { createSupabaseAuthClient } from "./supabase-auth-adapter";
const Context = createContext<{
  state: AuthState;
  session: AuthSession | null;
  client: AuthClient;
} | null>(null);
export function SessionProvider({
  children,
  client: supplied
}: {
  children: React.ReactNode;
  client?: AuthClient;
}) {
  const client = useMemo(() => supplied ?? createSupabaseAuthClient(), [supplied]);
  const [session, setSession] = useState<AuthSession | null>(null);
  const [state, setState] = useState<AuthState>("loading");
  useEffect(() => {
    let active = true;
    client.session().then((value) => {
      if (active) {
        setSession(value);
        setState(value ? "authenticated" : "unauthenticated");
      }
    });
    const off = client.subscribe((value) => {
      setSession(value);
      setState(value ? "authenticated" : "unauthenticated");
    });
    return () => {
      active = false;
      off();
    };
  }, [client]);
  return <Context.Provider value={{ state, session, client }}>{children}</Context.Provider>;
}
export function useSession() {
  const value = useContext(Context);
  if (!value) throw new Error("useSession must be inside SessionProvider");
  return value;
}
