"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { routes } from "@/src/foundation/navigation/routes";
export function Sidebar() {
  const path = usePathname();
  return (
    <aside className="sidebar">
      <Link href="/market" className="brand">
        <span className="brand-mark">↗</span>
        <span>
          Crypto Strategy Lab<small>Institutional grade</small>
        </span>
      </Link>
      <nav className="nav" aria-label="Primary">
        {routes.map(({ href, label, icon: Icon }) => (
          <Link key={href} href={href} className={path.startsWith(href) ? "active" : ""}>
            <Icon size={17} />
            <span>{label}</span>
          </Link>
        ))}
      </nav>
    </aside>
  );
}
