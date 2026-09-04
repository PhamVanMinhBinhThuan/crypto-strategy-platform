# F014 Main Flow Evidence

## EV-US1-001: Live startup và browser journey lần 1

- Criterion/requirement: US1, FR-003–FR-012, SC-001–SC-003; Market → Strategy → Search → Backtest → Evaluation → Leaderboard → Result/Trades → News/Sentiment.
- Status: BLOCKED
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation F014 T001–T030 chưa commit)
- Captured at: `2026-09-04T05:14:44Z`
- Environment/profile: LIVE startup attempt; macOS arm64, Java 21.0.12.1, Node 22.23.2, PostgreSQL remote, Redis 8.6.3, API `8080`, Worker `8081`
- Non-secret configuration: `PLATFORM_MARKET_DATA_PROVIDER=binance`, Search start/reproduce enabled; Sentiment chưa start; không bật fixture
- Command/action: source server-only `.env.local`; chạy `./gradlew :apps:api:bootRun --no-daemon` và `./gradlew :apps:worker:bootRun --no-daemon`; kiểm tra bốn actuator liveness/readiness endpoints.
- Expected result: toàn bộ dependency ready, đăng nhập Web và hoàn thành live browser journey trong tối đa 10 phút, có screenshot/result/correlation IDs.
- Observed result: PostgreSQL query pass; Redis `PING` pass; API và Worker start thành công; cả bốn liveness/readiness response đều `HTTP 200 {"status":"UP"}`. Redis stream group `backtest-workers` tồn tại và không có pending message. Browser journey chưa thể bắt đầu vì local chưa có `NEXT_PUBLIC_SUPABASE_URL`/`NEXT_PUBLIC_SUPABASE_ANON_KEY` và chưa có authenticated development session; Sentiment service cũng chưa được start bằng model bundle/token.
- Artifact links: `docs/evidence/f014/main-flow.md` (record này); automated boundary evidence tại `apps/web/tests/e2e/f014-research-flow.spec.ts`, `apps/web/tests/e2e/f014-market-demo.spec.ts`, `apps/web/tests/e2e/f014-news-demo.spec.ts` và `apps/api/src/test/java/com/cryptostrategy/platform/api/experiment/F014ResearchFlowIntegrationTest.java` chỉ là test evidence, không thay thế live screenshots.
- Limitations: không có screenshot, timing browser, experiment/result ID hoặc correlation ID thật; record này không được nâng thành `VERIFIED` và T029 vẫn mở.
- Owner/reviewer: project team / pending reviewer

## Việc cần có để chạy lại EV-US1-002

1. Inject local browser-safe `NEXT_PUBLIC_SUPABASE_URL` và `NEXT_PUBLIC_SUPABASE_ANON_KEY`; không commit hai giá trị này.
2. Có một development user đăng nhập hợp lệ trong browser.
3. Start Sentiment bằng service token/model bundle ngoài repository, hoặc ghi rõ degraded nếu chỉ kiểm tra failure scenario.
4. Dùng Node 22 qua `PATH=/opt/homebrew/opt/node@22/bin:$PATH` vì Homebrew Node mặc định trên máy đang hỏng dynamic link.
5. Chụp ít nhất: bốn Market chart; catalog bốn Strategy; published personal/composite Strategy và conflict rule; Random Search/progress terminal; Top-K → authoritative Result; bốn metric cards cùng một hàng Trade Entry/Exit; News có `ANALYZED` và degraded status nếu kiểm tra isolation.
6. Ghi duration, experiment ID, candidate/result ID, leaderboard revision và correlation ID đã redact vào record mới; ảnh tải lên Drive phải liên kết từng dòng rubric tương ứng.
