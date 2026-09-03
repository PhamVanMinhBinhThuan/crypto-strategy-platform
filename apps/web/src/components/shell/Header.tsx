import { AccountMenu } from "./AccountMenu";
export function Header() {
  return (
    <header className="header">
      <span>
        <b>BTC/USDT</b> · <span style={{ color: "var(--green)" }}>● Live status</span>
      </span>
      <AccountMenu />
    </header>
  );
}
