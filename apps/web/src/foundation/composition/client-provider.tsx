"use client";
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ApiClient } from "../http/contracts";
import type { RealtimeClient } from "../realtime/contracts";
import { createApiClient } from "../http/api-client";
import { createRealtimeClient } from "../realtime/realtime-client";
import { useSession } from "../auth/SessionProvider";
import { getPublicEnvironment } from "../config/environment";
import { recoverAuthentication } from "../auth/session-lifecycle";
const Context = createContext<{
  api: ApiClient;
  realtime: RealtimeClient;
  fixtures: boolean;
} | null>(null);
export function ClientProvider({ children }: { children: React.ReactNode }) {
  const { client } = useSession();
  const env = getPublicEnvironment();
  const [developmentValue, setDevelopmentValue] = useState<React.ContextType<typeof Context>>();
  const productionValue = useMemo(() => {
    const recover = () => recoverAuthentication(client);
    if (env.fixturesEnabled) return null;
    const api = createApiClient(env.apiBaseUrl, client, fetch, recover);
    return {
      api,
      realtime: createRealtimeClient(env.websocketUrl, api, recover),
      fixtures: false
    };
  }, [client, env.apiBaseUrl, env.fixturesEnabled, env.websocketUrl]);
  useEffect(() => {
    if (!env.fixturesEnabled) return;
    let active = true;
    void import("./development-clients").then(({ createFixtureClients }) => {
      if (active) setDevelopmentValue(createFixtureClients());
    });
    return () => {
      active = false;
    };
  }, [env.fixturesEnabled]);
  const value = developmentValue ?? productionValue;
  if (!value) return <p role="status">Loading deterministic fixture adapters…</p>;
  return <Context.Provider value={value}>{children}</Context.Provider>;
}
export function useClients() {
  const value = useContext(Context);
  if (!value) throw new Error("useClients must be inside ClientProvider");
  return value;
}
