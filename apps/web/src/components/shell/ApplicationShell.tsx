"use client";

import { Sidebar } from "./Sidebar";
import { Header } from "./Header";
import { FixtureModeBadge } from "../states/FixtureModeBadge";
import { useClients } from "@/src/foundation/composition/client-provider";
export function ApplicationShell({ children }: { children: React.ReactNode }) {
  const { fixtures } = useClients();
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="app-main">
        <Header />
        <main className="content">{children}</main>
      </div>
      {fixtures && <FixtureModeBadge />}
    </div>
  );
}
