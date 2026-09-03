import { BarChart3, FlaskConical, Gauge, Newspaper, SlidersHorizontal } from "lucide-react";
export const routes = [
  { href: "/market", label: "Market Dashboard", icon: Gauge },
  { href: "/strategies", label: "Strategy Composer", icon: SlidersHorizontal },
  { href: "/backtests", label: "Backtest Results", icon: BarChart3 },
  { href: "/search", label: "Search & Leaderboard", icon: FlaskConical },
  { href: "/news", label: "News Sentiment", icon: Newspaper }
] as const;
