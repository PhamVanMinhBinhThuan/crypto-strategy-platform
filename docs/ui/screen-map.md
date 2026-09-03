# Shared UI Screen Map

This map prevents multiple frontend features from creating competing versions of the same screen.

| UI ID | Screen | Route | Feature owner | Primary prototype page | Primary screenshot |
|---|---|---|---|---|---|
| UI-01 | Market Dashboard | `/market` | F-012 | `prototype/pages/MarketDashboardPage.tsx` | `screens/Market Dashboard.png` |
| UI-02 | Strategy Composer | `/strategies` | F-012 | `prototype/pages/StrategyComposerPage.tsx` | `screens/Stragegy Composer.png` |
| UI-03 | Backtest Results | `/backtests` | F-013 | `prototype/pages/BacktestResultsPage.tsx` | `screens/Backtest Results.png` |
| UI-04 | Search & Leaderboard | `/search` | F-013 | `prototype/pages/SearchLeaderboardPage.tsx` | `screens/Search & Leaderboard.png` |
| UI-05 | News Sentiment | `/news` | F-012 | `prototype/pages/NewsSentimentPage.tsx` | `screens/New Sentiment.png` |

## Ownership rules

- F-011 owns the production shell, authentication/session, HTTP/realtime clients and shared asynchronous states.
- F-012 owns Market, Strategy and News business UI.
- F-013 owns Backtest Result and Search/Leaderboard business UI.
- F-014 integrates and hardens the already implemented screens; it should not create another visual system.
- A later feature may extend a screen only after explicitly declaring ownership of the new interaction and preserving the existing shared foundation.

## Navigation intent

```text
Market Dashboard
   -> Strategy Composer
      -> Backtest Results
      -> Search & Leaderboard

Search & Leaderboard
   -> Backtest Result detail for an authoritative result identity

Market Dashboard
   -> News Sentiment
```

Exact navigation and identifiers must follow the released production contracts rather than prototype-only state.
