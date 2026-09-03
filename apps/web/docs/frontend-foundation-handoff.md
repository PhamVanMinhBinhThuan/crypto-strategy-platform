# Frontend Foundation Handoff

F-012 and F-013 must reuse `src/foundation` and the protected application shell. `/market`,
`/strategies`, and `/news` belong to F-012; `/backtests` and combined `/search` + leaderboard
belong to F-013. Do not create another auth, API, WebSocket client, or app shell. Components depend
on the published interfaces so fixtures can be replaced by real F-009 adapters without UI rewrites.
