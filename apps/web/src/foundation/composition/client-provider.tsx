"use client";
import { createContext, useContext, useMemo } from "react";
import type { ApiClient } from "../http/contracts";
import type { RealtimeClient } from "../realtime/contracts";
import { createApiClient } from "../http/api-client";
import { createRealtimeClient } from "../realtime/realtime-client";
import { useSession } from "../auth/SessionProvider";
import { getPublicEnvironment } from "../config/environment";
import { MockApiClient } from "../testing/mock-api-client";
import { MockRealtimeClient } from "../testing/mock-realtime-client";
const Context = createContext<{
  api: ApiClient;
  realtime: RealtimeClient;
  fixtures: boolean;
} | null>(null);
export function ClientProvider({ children }: { children: React.ReactNode }) {
  const { client } = useSession();
  const value = useMemo(() => {
    const env = getPublicEnvironment();
    if (env.fixturesEnabled)
      return { api: new MockApiClient(), realtime: new MockRealtimeClient(), fixtures: true };
    const api = createApiClient(env.apiBaseUrl, client);
    return {
      api,
      realtime: createRealtimeClient(env.websocketUrl, api, () => {}),
      fixtures: false
    };
  }, [client]);
  return <Context.Provider value={value}>{children}</Context.Provider>;
}
export function useClients() {
  const value = useContext(Context);
  if (!value) throw new Error("useClients must be inside ClientProvider");
  return value;
}
