# F016 Sentiment Strategy — Verification Record

## Phạm vi và môi trường

- Captured at: `2026-09-15T03:54:18Z`
- Branch: `feature/016-sentiment-strategy`
- Base commit: `28009857e3972e368278030fa5040f50342cab08`
- Working tree: `DIRTY` — thay đổi F016 chưa được commit theo yêu cầu của người dùng.
- Implementation patch SHA-256: `1b6e9741ce29b802e8025d9037935114ce074ae3bd116cebdbcdb492018f0792`
- OS: macOS `26.6.2` (`25G83`), Apple Silicon `arm64`
- Verification toolchain: Temurin Java `21.0.12.1`, Node `22.23.2`, npm `10.9.8`, Gradle `8.14.5`
- Shell mặc định tại thời điểm ghi record đang trỏ tới Java 25 và Node 24 Homebrew bị thiếu
  `libsimdjson.27.dylib`; các lệnh kiểm tra bên dưới được chạy bằng toolchain Java 21/Node 22 của dự án.
- External services lúc chốt record: Redis `6379` đang nghe; Web `3000`, Sentiment `8000` và API `8080`
  không chạy; Docker daemon không kết nối được.
- Subsequent LIVE attempt (`2026-09-15`, cùng branch/base SHA): Docker Desktop đã được bật; Web
  `3000`, Sentiment `8000`, API `8080`, Worker `8081` đều trả health `200` và Redis trả `PONG`.
  Sentiment trả `READY`, model `multichannel-english-1.0.0`. Database được cấu hình là PostgreSQL
  từ xa của nhóm; ban đầu bảng `news.sentiment_snapshot` và cột F016 Result chưa có.
- Shared migration deployment (`2026-09-15T07:25:05Z`): sau khi người dùng phê duyệt rõ ràng, áp đúng
  `20260909000100_f016_sentiment_snapshot.sql` và
  `20260911000100_f016_result_decision_evidence.sql` bằng `psql -v ON_ERROR_STOP=1 -X -q -f` trên
  database `postgres`, PostgreSQL `17.6`, role `postgres`. Cả hai lệnh thành công. Đã kiểm tra lại
  bảng snapshot/observation, trigger validate/count/immutability, cột JSONB Result và array check
  constraint; tất cả có mặt. Không sửa/xóa dữ liệu đã có. Sau migration Result cũ vẫn đếm được
  `1208`; snapshot mới `0` vì chưa Start một run Sentiment sau deployment.
- Input readiness: BTC có `101` analyzed Sentiment Results và dữ liệu mới nhất lúc kiểm tra được
  published `2026-09-15T06:12:01Z`; active model `multichannel-english-1.0.0` có `131` Results.
  API/Worker readiness và Sentiment model readiness vẫn HTTP `200` sau migration.

Digest phía trên bao phủ working patch và file untracked của implementation, nhưng loại trừ chính file
`verification.md` và `tasks.md` để record không tự làm thay đổi digest của nó.

## EV-016-06 — Tính xác định và chống look-ahead

- Status: `VERIFIED`
- Profile: `AUTOMATED`
- Command:

```bash
./gradlew \
  :modules:strategies:test --tests '*SentimentPolarityStrategyTest' \
  :modules:combination:test --tests '*SentimentCompositeStrategyTest' \
  :apps:worker:test --tests '*NewsFrozenSupplementalInputResolverTest' \
  :apps:api:test --tests '*StartExperimentIntegrationTest'
```

- Observed: `BUILD SUCCESSFUL in 1m28s`; 52 tasks, 4 executed và 48 up-to-date.
- Coverage: BUY/HOLD/SELL, biên threshold, loại News tương lai/hết hạn, evidence ổn định, Majority/Weighted
  tie trả HOLD, frozen replay không gọi live Sentiment, technical execution không phụ thuộc Sentiment và
  snapshot preflight dừng trước khi tạo durable Search graph.
- Limitations: reproduction `MATCHED` trên hạ tầng LIVE chưa được quay trong lần này.

## EV-016-07 — Cô lập lỗi Sentiment

- Status: `PARTIAL`
- Profile: `AUTOMATED`
- Automated evidence:
  - `NewsFrozenSupplementalInputResolverTest` chứng minh frozen replay không cần live Sentiment.
  - Cùng test suite chứng minh technical-only execution không đọc Sentiment store.
  - Browser test `f014-failure-recovery.spec.ts` xác nhận News vẫn đọc được khi Sentiment degraded và có
    đường retry về trạng thái authoritative.
- LIVE limitation: stack và schema đã sẵn sàng, nhưng chưa chạy/quay một Result Sentiment bằng phiên
  Supabase Auth của người dùng. `401 AUTHENTICATION_REQUIRED` khi gọi status endpoint chưa đăng nhập
  là rào bảo vệ đúng contract, không phải proof của outage.

## EV-016-08 — Browser, responsive và keyboard

- Status: `VERIFIED`
- Profile: `AUTOMATED` / `CONTROLLED_FIXTURE`
- Commands và kết quả:

```bash
cd apps/web
npx playwright test --project=desktop --project=mobile
# 62 passed, 90 skipped, 0 failed (2.3m)

npx playwright test tests/e2e/f012-performance.spec.ts \
  --project=performance-desktop --project=performance-mobile
# 2 passed, 0 failed (1.0m)
```

- Observed: F016 finite domains và Composite Search pool đạt; các viewport 360/768/1024/1440 đạt;
  desktop keyboard flow đạt; desktop/mobile application shell đạt.
- Skip disclosure: phần lớn 90 case là suite F013 chỉ bật khi có fixture/credential profile riêng. Case
  keyboard của F014/F016 bị skip trên project touch-emulated mobile vì semantics bàn phím được kiểm tra ở
  desktop; responsive mobile vẫn chạy và đạt.

## EV-016-09 — Java và Web quality gates

- Status: `VERIFIED`
- Profile: `AUTOMATED`
- Java command: `./gradlew clean check`
- Java observed: `BUILD SUCCESSFUL in 30s`; 102 tasks, 65 executed, 24 from cache, 13 up-to-date.
- Java limitation: hai integration test phụ thuộc external Redis/profile là
  `F014RecoveryScenarioTest` và `RealtimeRedisRecoveryIntegrationTest` được skip theo cấu hình; unit,
  module, contract và architecture gates còn lại đạt.
- Web command: `cd apps/web && npm run check`
- Web observed: format, lint và typecheck đạt; Vitest `107 files / 309 tests` đạt; Next.js `16.3.4`
  production build đạt với 14 pages.
- Isolated PostgreSQL gate (sau khi bật Docker): chạy `postgres:16-alpine` tại
  `127.0.0.1:55416/f016_verify`, tạo `auth.users` stub và role `anon`/`authenticated`, áp lần lượt
  toàn bộ 15 migration từ `supabase/migrations/` thành công. Chạy
  `./gradlew :modules:persistence:newsIntegrationTest --tests '*SentimentSnapshotIntegrationTest'
  --no-daemon` trên riêng DB này: `BUILD SUCCESSFUL in 7s`, 37 tasks (2 executed, 35 up-to-date).
  Test round-trip/idempotence, chống mutation và rollback khi asset không khớp đều đạt; không thay đổi
  Supabase chung.
- Sentiment image gate: trên Docker Linux `aarch64`, extra `ml` trước đây fail vì không có wheel
  `tensorflow-cpu==2.19.0`. Dependency đã được phân nhánh theo kiến trúc, image build đạt và service
  `READY`. File `.env.local` đang trỏ `SENTIMENT_BUNDLE_PATH` tới Windows `D:/...`; lần chạy macOS
  này override biến bằng absolute path của repo, không ghi lại secret file.

## EV-016-10 — Migration trên PostgreSQL nhóm

- Status: `VERIFIED`
- Profile: `LIVE`
- Captured at: `2026-09-15T07:25:05Z`
- Approval: người dùng xác nhận áp hai migration F016 trên database nhóm trong cuộc trò chuyện này.
- Command: `psql -v ON_ERROR_STOP=1 -X -q -f` cho đúng hai file forward-only đã dry-run trên
  PostgreSQL cô lập; không chạy test ghi dữ liệu trên database chung.
- Observed: bảng `news.sentiment_snapshot`/observation, trigger kiểm tra/immutability, cột
  `experiment.backtest_result.strategy_decision_evidence` và array check đều có. `1208` Results cũ
  vẫn đọc được; số row có evidence NULL hoặc không phải JSON array là `0`. Web/API/Worker/Sentiment
  health đều HTTP `200` sau deployment.
- Limitations: record này chứng minh schema deployment, chưa chứng minh một Result Sentiment LIVE vì
  chưa Start run bằng phiên người dùng đã xác thực.

## Phần LIVE còn phải thu

T048 chưa đóng hoàn toàn. Stack đã khởi động, hai migration trên database nhóm đã áp và input News
đủ cho BTC; cần người dùng đăng nhập và quay flow thực tế:

1. News → `Use as Strategy` → Composer.
2. Sentiment Single hoặc Composite → Search → Candidate → Result evidence.
3. Reproduce tới `MATCHED` bằng frozen snapshot.
4. Dừng Sentiment, xác nhận banner `DEGRADED`, Result cũ và technical-only Backtest vẫn hoạt động.

Không có minh chứng LIVE nào được suy diễn từ fixture hoặc automated test trong record này.
