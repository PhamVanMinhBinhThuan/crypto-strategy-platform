"use client";
import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { PanelLeftClose, PanelLeftOpen } from "lucide-react";
import { routes } from "@/src/foundation/navigation/routes";
import { useRememberedResource } from "@/src/foundation/navigation/resource-history";
import { AccountMenu } from "./AccountMenu";
export function Sidebar() {
  const path = usePathname();
  const [collapsed, setCollapsed] = useState(false);
  const lastExperiment = useRememberedResource("experiment");
  const lastBacktest = useRememberedResource("backtest");
  const destination = (href: string) =>
    href === "/search" ? lastExperiment : href === "/backtests" ? lastBacktest : href;
  return (
    <aside className={`sidebar${collapsed ? " is-collapsed" : ""}`}>
      <div className="sidebar-header">
        <Link href="/market" className="brand" aria-label="Crypto Strategy Lab home">
          <span className="brand-mark">↗</span>
          <span className="brand-copy">
            Crypto Strategy Lab<small>Institutional grade</small>
          </span>
        </Link>
        <button
          type="button"
          className="sidebar-toggle"
          aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          aria-expanded={!collapsed}
          onClick={() => setCollapsed((value) => !value)}
        >
          {collapsed ? <PanelLeftOpen size={18} /> : <PanelLeftClose size={18} />}
        </button>
      </div>
      <nav className="nav" aria-label="Primary">
        {routes.map(({ href, label, icon: Icon }) => (
          <Link
            key={href}
            href={destination(href)}
            className={path.startsWith(href) ? "active" : ""}
            title={collapsed ? label : undefined}
          >
            <Icon size={17} />
            <span>{label}</span>
          </Link>
        ))}
      </nav>
      <footer className="sidebar-footer">
        <AccountMenu />
      </footer>
    </aside>
  );
}
