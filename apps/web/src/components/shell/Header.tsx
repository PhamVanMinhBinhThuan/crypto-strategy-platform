import { AccountMenu } from "./AccountMenu";
export function Header() {
  return (
    <header className="header">
      <span>
        <b>Crypto Strategy Lab</b> · <span>Authenticated workspace</span>
      </span>
      <AccountMenu />
    </header>
  );
}
