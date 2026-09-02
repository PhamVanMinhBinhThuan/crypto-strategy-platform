# Checklist hoàn tất implementation F-009

## Scope và boundary

- [x] Public controller chỉ gọi published application ports.
- [x] Public identity dùng UUID hoặc typed domain ID tại Java boundary.
- [x] Exact decimal và UTC representation có contract tests.
- [x] Foreign-owner resources trả ownership-safe 404.
- [x] Start/reproduce Experiment không giả lập thành công khi Search Coordinator chưa ready.

## Security và reliability

- [x] JWT, one-time WebSocket ticket, origin và expiry được kiểm thử.
- [x] Internal News audit dùng credential riêng và browser JWT bị từ chối.
- [x] Token, SQL, path, provider payload và exception detail không rò ra response/log.
- [x] Idempotency replay/conflict và atomic standalone Backtest graph được kiểm thử.
- [x] Realtime queue bounded, coalescing, terminal retention và snapshot recovery được kiểm thử.

## Contract và release

- [x] OpenAPI, error catalog, examples và WebSocket event catalog khớp DTO hiện tại.
- [x] Job state fixtures bao phủ queued/running/retry/cancelled/failed/succeeded.
- [x] Full Gradle suite và Python contract suite được chạy.
- [x] ADR standalone Backtest và architecture evidence được review.
- [ ] PostgreSQL/Supabase integration chạy với schema thật (thiếu database environment).
- [ ] Redis runtime recovery smoke chạy với service thật (thiếu Redis environment).
- [ ] Experiment start/reproduce được mở gate (chờ Search Coordinator runtime boundary).
