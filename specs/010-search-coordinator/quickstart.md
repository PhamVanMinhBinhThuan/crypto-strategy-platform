# Quickstart kiểm chứng F-010 Search Coordinator

## Điều kiện

- Branch `010-search-coordinator`, Java 21 và Gradle wrapper.
- PostgreSQL/Supabase test instance với migration F-001 đến F-010.
- Redis test instance cô lập; không dùng production credentials.
- F-003 Dataset và F-004 Strategy fixtures bất biến.

## Static và unit gates

```bash
git diff --check
JAVA_HOME=/path/to/jdk-21 ./gradlew test
```

Kỳ vọng: pure Search determinism/registry tests, Experiment transaction tests, Worker consumer và
recovery tests, API idempotency/ownership tests cùng architecture suite đều pass.

## Database integration

```bash
DATABASE_URL=... DATABASE_USERNAME=... DATABASE_PASSWORD=... \
  JAVA_HOME=/path/to/jdk-21 ./gradlew :modules:persistence:experimentIntegrationTest
```

Kiểm tra tối thiểu:

1. Migration mới áp dụng sau F-009 và migration cũ không đổi checksum.
2. Start command tạo atomic Experiment + Manifest + SEARCH Job + Search Run + Outbox + receipt.
3. Inject lỗi trước commit không để partial graph hoặc receipt mồ côi.
4. Hai concurrent allocators cùng version chỉ tạo một generation index/Candidate/Backtest Job.
5. Stop/allocate race không commit Candidate sau durable stop.
6. Reproduction initialization copy exact ordered Candidate sequence và không sửa source.

## Redis và Worker integration

Khởi chạy Worker với stream prefix test và group Coordinator riêng, sau đó:

1. Publish một `SEARCH_REQUEST` hợp lệ; xác nhận SEARCH Job được claim và bounded window được fill.
2. Chạy finite Experiment tới Candidate Evaluated, Leaderboard revision và terminal lifecycle.
3. Phát cùng request/completion 100 lần; xác nhận không business outcome trùng.
4. Phát completion stale/out-of-order; xác nhận progress không giảm/vượt authoritative totals.
5. Kill Worker ở từng boundary allocation/outbox/ACK, restart và xác nhận run tự tiếp tục.
6. Xóa test streams/cache nhưng giữ DB, chạy reconciler và xác nhận publication/dispatch được repair.
7. Gửi malformed/unsupported event; xác nhận bounded retry/dead-letter và không lộ payload nhạy cảm.

## Generator replaceability proof

Chạy cùng frozen fixture với `random-search` và một fixture generator conforming:

- cùng seed/state cho cùng ordered outputs qua 100 runs;
- registry từ chối duplicate/unsupported version;
- Candidate ngoài search space/duplicate/no-progress bị chặn;
- không sửa Backtest, Evaluation, Leaderboard business code hoặc public contract.

## Public API gate removal

Sau khi Start conditions trong `contracts/public-readiness-contract.md` pass:

1. `POST /api/v1/experiments` trả `202`, `Location`, Experiment/Job ID và `QUEUED`.
2. Replay cùng key/body 100 lần trả cùng outcome; body khác trả idempotency conflict.
3. User B start bằng private input của User A nhận ownership-safe inaccessible.
4. WebSocket chỉ báo progress/lifecycle; disconnect vẫn reconcile đúng qua REST snapshot.

Sau đó chỉ gỡ Reproduce gate khi US3 conditions pass:

1. `POST /api/v1/experiments/{id}/reproductions` trả `202`, tạo run mới và source không đổi.
2. Response hoàn tất trước execution/verification; durable verification bắt đầu ở `PENDING`.
3. Terminal notification lặp hoặc restart vẫn tạo đúng một `MATCHED|MISMATCHED|FAILED` outcome.
4. User B reproduce source của User A nhận ownership-safe inaccessible.

## Evidence record

Ghi commit, timestamp, environment, migration set, stream prefix, worker concurrency, generator
version/seed, command và result. Không chuyển architecture evidence sang `Verified` chỉ từ unit test
hoặc số liệu minh họa.

### Evidence US2 — stop/recovery (2026-09-03)

- Baseline: branch `010-search-coordinator`, commit artifact `93eb912d551bac9d0aa5803dc839c06297568e92`,
  working tree implementation F-010 chưa commit; Java Temurin 21.0.12.
- PostgreSQL fixture cô lập: container `crypto-f010-postgres`, database `crypto_f010`, cổng `54322`,
  migration F-010 đã áp dụng. Redis fixture cô lập: container `crypto-f010-redis`, cổng `6379`,
  restart thành công và `redis-cli PING` trả `PONG`; không sử dụng credential production.
- Worker recovery suite:
  `./gradlew --init-script .specify/gradle-f010-isolation.init.gradle :apps:worker:test --tests "*SearchCrashRecoveryTest" --tests "*SearchReconciliationTest" --tests "*SearchFailurePolicyTest"`
  — `BUILD SUCCESSFUL`.
- PostgreSQL stop/deadline suite:
  `./gradlew --init-script .specify/gradle-f010-isolation.init.gradle :modules:persistence:experimentIntegrationTest --tests "*SearchStopRaceIntegrationTest" --tests "*SearchDeadlineIntegrationTest" --rerun-tasks`
  — `BUILD SUCCESSFUL`.
- Evidence xác nhận reclaim/restart, queue-loss repair từ durable truth, bounded retry/dead-letter
  có redaction, stop-vs-allocation fence, deadline đóng băng qua restart và quy tắc phân xử
  completion/deadline. Đây là evidence US2 có phạm vi; chưa phải bằng chứng finite end-to-end của T043
  hoặc điều kiện tự động mở public Start gate.

### Evidence US1 — finite Search (2026-09-03)

- Cùng baseline và PostgreSQL fixture cô lập ở trên; generator `random-search` phiên bản `1.0.0`,
  seed `42`, một Candidate và cửa sổ in-flight bằng `1`.
- Lệnh:
  `./gradlew --init-script .specify/gradle-f010-isolation.init.gradle :modules:search:test :modules:persistence:experimentIntegrationTest --tests "*SearchAllocationConcurrencyIntegrationTest" --tests "*FiniteSearchExperimentIntegrationTest" --tests "*SearchDeadlineIntegrationTest"`
  — `BUILD SUCCESSFUL`.
- `FiniteSearchExperimentIntegrationTest` chạy generator qua public Search boundary, commit atomically
  Candidate + BACKTEST Job + Outbox + coordination decision, ghi Result/Evaluation/Leaderboard fixture
  authoritative, rồi reconcile SEARCH Job/Run đến `COMPLETED`. Test chạy trong transaction rollback;
  không sửa evidence bất biến và không để dữ liệu fixture tồn dư.
- Trong lúc kiểm chứng đã sửa tương thích round-trip generator state qua PostgreSQL `jsonb` (khoảng trắng
  do materialization không làm thay đổi fingerprint/canonical semantics).

### Evidence US3 — async Reproduce (2026-09-03)

- PostgreSQL command:
  `./gradlew --init-script .specify/gradle-f010-isolation.init.gradle :modules:persistence:experimentIntegrationTest --tests "*SearchReproductionIntegrationTest"`
  — `BUILD SUCCESSFUL`.
- Unit/public commands:
  `./gradlew --init-script .specify/gradle-f010-isolation.init.gradle :modules:experiment-execution:test --tests "*SearchReproductionValidationTest" --tests "*SearchReproductionVerificationTest" :apps:api:test --tests "*ReproduceExperimentIntegrationTest"`
  — các suite mục tiêu `BUILD SUCCESSFUL`.
- Evidence kiểm tra source đúng owner và terminal; Manifest/Candidate sequence được copy sang graph mới
  mà source không đổi; replay trả cùng identity, hash khác conflict, lỗi giữa transaction rollback toàn graph;
  verification tồn tại ở `PENDING` trước response và chỉ claim khi reproduction terminal.
- Comparator đối chiếu ordered Trade semantics, exact canonical tuple Total Return/Win Rate/Maximum
  Drawdown/Number of Trades cùng Result/Evaluation/Leaderboard fingerprints. PostgreSQL fence chuyển
  `PENDING -> RUNNING -> MATCHED`; trigger lặp sau terminal không tạo outcome thứ hai. Mismatch report
  chỉ chứa khóa bounded an toàn, không chứa payload/stack/SQL/path.


## Evidence hardening trước merge (2026-09-03)

- Commit triển khai cố định: `d6a9de33c6385fc24bd912c8de5ec810574e2320` trên branch
  `010-search-coordinator`. Toàn bộ lệnh dưới đây được chạy khi `HEAD` đúng commit này.
- Môi trường: Windows, Temurin `21.0.12.1`; Docker Server `29.1.3`; PostgreSQL Bitnami
  `18.3.0` tại `localhost:55432`, database sạch `crypto_f010_evidence`; Redis
  `redis:7-alpine` tại `localhost:6379`. Chỉ dùng credential fixture cục bộ.
- Database được drop/create lại, tạo stub `auth.users` tương thích Supabase local và áp dụng tuần tự
  toàn bộ migration `20260827000100` đến `20260903000100` với `ON_ERROR_STOP=1`; không sửa migration
  F-008 đã áp dụng. Contract checksum F-008 băm nội dung canonical LF để độc lập line ending Windows.
- Canonical Java gate: `gradlew.bat --no-daemon test` — `BUILD SUCCESSFUL`, 70 tasks.
- Architecture gate: `gradlew.bat --no-daemon :architecture-tests:test` — `BUILD SUCCESSFUL`,
  37 tasks; không nới dependency, typed-ID, scope hay ADR rule.
- PostgreSQL gate chạy thật, bỏ qua cache:
  `gradlew.bat --no-daemon --rerun-tasks :modules:persistence:experimentIntegrationTest` —
  `BUILD SUCCESSFUL` trong 36 giây, 28/28 tasks executed. Regression reproduction kiểm tra source
  thiếu Manifest/Search Run/Candidate đều fail và rollback toàn graph; mọi immutable copy yêu cầu
  affected-row count đúng bằng một.
- Redis được restart bằng `docker restart crypto-f010-redis`; `redis-cli PING` trả `PONG`.
  Worker recovery gate:
  `gradlew.bat --no-daemon --rerun-tasks :apps:worker:test --tests "*SearchCrashRecoveryTest"`
  `--tests "*SearchReconciliationTest" --tests "*SearchFailurePolicyTest"`
  `--tests "*SearchRequestConsumerTest"` — `BUILD SUCCESSFUL` trong 1 phút 28 giây,
  42/42 tasks executed.
- Public boundary regression xác nhận `Idempotency-Key` chứa dấu nháy, backslash, newline và control
  character không trở thành correlation data. Correlation ID được sinh phía server và bounded;
  event envelope dùng serializer với `messageType`/`messageVersion`. API/Worker dùng UTC `Clock`
  được inject; Coordinator production luôn reload durable state và trusted reconciliation.
- File init-script cô lập F-010 đã bị xóa; các gate trên là canonical Gradle wrapper commands.
