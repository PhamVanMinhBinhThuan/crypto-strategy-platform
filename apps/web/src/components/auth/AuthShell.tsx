import Link from "next/link";
export function AuthShell({
  title,
  subtitle,
  children
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <main className="auth-page">
      <div className="auth-glow" />
      <section className="auth-panel">
        <Link href="/login" className="brand">
          <span className="brand-mark">↗</span>
          <span>
            Crypto Strategy Lab<small>Strategy research workspace</small>
          </span>
        </Link>
        <div className="auth-heading">
          <p className="eyebrow">Secure workspace</p>
          <h1>{title}</h1>
          <p>{subtitle}</p>
        </div>
        {children}
        <p className="legal">
          For research and education only. Backtests do not guarantee future returns.
        </p>
      </section>
      <aside className="auth-visual" aria-hidden="true">
        <div className="grid-orb" />
        <p>DESIGN · TEST · COMPARE</p>
        <h2>
          Turn market data into
          <br />
          <em>reproducible evidence.</em>
        </h2>
        <div className="signal-card">
          <span>System status</span>
          <strong>
            <i /> Operational
          </strong>
        </div>
      </aside>
    </main>
  );
}
