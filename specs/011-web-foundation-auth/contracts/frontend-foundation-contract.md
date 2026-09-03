# Frontend Foundation Contract

F-011 sở hữu shell, auth/session, HTTP/realtime clients và shared UI states. F-012/F-013 sở hữu
business routes/components và MUST dùng các boundary đã công bố.

- Public routes: `/login`, `/register`, `/forgot-password`, `/reset-password`, `/auth/callback`.
- Protected shells: `/market`, `/strategies`, `/backtests`, `/search`, `/news`; `/search` chứa cả
  Search và Leaderboard do F-013 sở hữu.
- Auth contract: bootstrap/observe/refresh, sign-up/in/out và password recovery.
- HTTP contract: bearer request và normalized data/error/correlation result.
- Realtime contract: one-time ticket, connect, subscribe, reconnect và snapshot recovery hook.
- Test contract: real và fixture adapters có cùng consumer-facing interface.

F-012/F-013 không tạo auth/API/WebSocket singleton khác, không gọi business table/Binance/internal
Sentiment và không đặt business calculation trong foundation. Fixture phải explicit, visible và
tắt mặc định trong production.
