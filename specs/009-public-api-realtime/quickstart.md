# Quickstart kiểm chứng F-009

## Điều kiện

- Java 21 (`JAVA_HOME` trỏ tới JDK 21), Gradle wrapper và repository ở branch F-009.
- Python 3.11 cùng dependencies trong `apps/sentiment[test]` nếu chạy contract suite.
- Với integration: Supabase/PostgreSQL local, Redis và các biến môi trường theo
  `infra/compose/docker-compose.yml`; không dùng credential production.

## Kiểm tra contract tĩnh

```bash
diff -r specs/008-news-sentiment/contracts/sentiment-v1 modules/contracts/src/main/resources/contracts/sentiment-v1
git diff --check
```

OpenAPI, error catalog và WebSocket contract phải được review cùng một thay đổi; command
và event version không được lệch giữa tài liệu và transport DTO.

## Kiểm tra unit/architecture

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew test
```

Kỳ vọng: toàn bộ module test, API auth/error tests, realtime protocol tests và architecture
tests pass; không có violation về raw identity, UTC hoặc decimal.

## Kiểm tra Python contract

```bash
python3.11 -m venv /tmp/crypto-sentiment-f009
/tmp/crypto-sentiment-f009/bin/python -m pip install -e 'apps/sentiment[test]'
/tmp/crypto-sentiment-f009/bin/python -m pytest
```

## Luồng REST acceptance tối thiểu

1. Gọi public business endpoint không có token và token sai; xác nhận `401` với error
   envelope an toàn.
2. Với User A, tạo một workload bằng idempotency key mới; lặp lại cùng body 100 lần và
   xác nhận chỉ có một Job/Experiment.
3. Gửi body khác với cùng key; xác nhận `409 IDEMPOTENCY_KEY_CONFLICT` và không có outcome mới.
4. Với User B, dùng ID của User A để đọc/stop/reproduce; xác nhận inaccessible response
   không tiết lộ metadata.
5. Đọc Job sau khi worker thất bại; xác nhận HTTP read thành công và payload có terminal
   failure classification.
6. Với Backtest đơn lẻ, xác nhận `backtestId`, `candidateId` và `jobId` trong database là
   ba identity khác nhau; chỉ `backtestId` và `jobId` xuất hiện trong acceptance response.
7. Gây lỗi giữa transaction acceptance và commit; xác nhận không còn partial Experiment,
   Candidate, Job, Outbox hoặc idempotency receipt.

## Luồng WebSocket acceptance tối thiểu

1. Dùng authenticated REST boundary lấy one-time ticket, mở `/ws` đúng origin và gửi
   `SUBSCRIBE_CANDLES`/`SUBSCRIBE_EXPERIMENT`/`SUBSCRIBE_LEADERBOARD`.
2. Xác nhận mỗi subscription chỉ active sau confirmation; subscription sai ownership
   nhận isolated `SUBSCRIPTION_ERROR`.
3. Duy trì bốn Candle subscriptions, gửi duplicate/stale events và xác nhận client giữ
   bản close/newest revision.
4. Ngắt connection giữa snapshot và event, reconnect/resubscribe, đọc REST snapshot và
   xác nhận không mất terminal state hoặc tạo duplicate business effect.
5. Gửi command vượt limit/payload quá lớn/unknown field; xác nhận lỗi đúng scope hoặc
   connection close theo security policy.
6. Cho JWT gốc hết hạn trong khi connection đang mở; xác nhận server dừng private event và
   đóng bằng `4001 REAUTHENTICATION_REQUIRED`. Client refresh session, xin ticket mới,
   reconnect/resubscribe và reconcile REST mà không yêu cầu đăng nhập lại khi refresh token
   còn hợp lệ.

## Integration evidence

Sau khi local services sẵn sàng, chạy các task integration riêng của repository (news,
experiment, backtest/evaluation/leaderboard, API và worker Supabase). Ghi command, commit,
môi trường và kết quả vào evidence F-009; không đánh dấu `Verified` chỉ từ unit tests.

## Kết quả mong đợi

- Auth/ownership/idempotency/error contract pass.
- REST snapshot có pagination deterministic và exact/UTC representation.
- WebSocket không yêu cầu exactly-once nhưng reconcile được từ snapshot.
- News/Sentiment degraded không làm hỏng Market, Strategy hoặc technical Backtest reads.

## Readiness snapshot (cập nhật 2026-09-03)

| Capability | Trạng thái | Evidence |
| --- | --- | --- |
| Auth/error/ownership/idempotency | Ready | API integration và redaction tests |
| Market, Dataset, Strategy | Ready | Contract, ownership và conflict tests |
| Standalone Backtest start/read | Ready | Atomic aggregate + API/result tests |
| Experiment read/stop, Job read/cancel | Ready | Command/read/state tests |
| Experiment start/reproduce | Ready | Published F-010 runtime boundary; public acceptance/replay/ownership và PostgreSQL evidence; vẫn trả `503` nếu tắt feature flag |
| Realtime Candle/workload/leaderboard | Ready trong test local | Protocol, lifecycle, backpressure, recovery và Redis thật với injected connection outage; chưa benchmark production |
| Public News và protected audit | Ready | Public/degraded/security tests; audit giữ route `/internal/news-items/**` |

Redis workload streams mặc định bật trong runtime bằng
`platform.realtime.streams.enabled=true`; Gradle unit test tắt listener để không phụ thuộc
dịch vụ ngoài. Các stream là `progress.events.v1`, `lifecycle.events.v1` và
`candidate.evaluated.v1`.

### Evidence đã chạy

- `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew test`
  — `BUILD SUCCESSFUL` (68 actionable tasks; full repository suite).
- `/tmp/crypto-sentiment-f009/bin/python -m pytest apps/sentiment/tests -q`
  — `10 passed`.
- OpenAPI YAML parse và documentation parity test — pass.

### Evidence bổ sung từ F-010 (2026-09-03)

- Commit triển khai `0a065db` trên branch `010-search-coordinator`; Java Temurin 21.0.12.
- PostgreSQL fixture `crypto_f010` tại cổng `54322` đã chạy các suite finite Search,
  stop/deadline race và durable reproduction; Redis fixture tại cổng `6379` đã được restart,
  trả `PONG` và chạy các suite reclaim/reconciliation/failure policy. Chi tiết lệnh và cấu hình
  không-production nằm trong `specs/010-search-coordinator/quickstart.md`.
- Public Start và Reproduce integration tests xanh; các gate được mở theo evidence độc lập.
- Evidence này đóng phần PostgreSQL/Redis runtime của T074, nhưng không tuyên bố latency production
  hoặc benchmark throughput nhiều Worker ngoài phạm vi các test đã ghi.

### Regression hardening F-009 (2026-09-03)

- Branch `feature/009-public-api-real-time`, baseline `f310b383b77c376bffc18ff88ffa7e65040ef6d9`
  cộng working tree hardening của lần implement này; thay đổi chưa commit.
- Windows, Temurin `21.0.12.1`; Python `3.12.13` trong `.venv/f009` (phù hợp
  `requires-python >=3.11,<3.13`); Redis `redis:7-alpine` tại `localhost:6379`.
- Bốn regression ban đầu tái hiện lỗi listener giữa hai connection, cleanup khi authorization
  thất bại, listener tự đóng trong callback và coalescing sai subscription. Sau sửa, bộ realtime
  kiểm chứng thêm late callback sau resubscribe, close race và overflow buffer activation.
- Redis smoke dùng stream ngẫu nhiên `f009-smoke-<UUID>:*`, đọc/ghi Redis thật và inject
  `RedisConnectionFailureException` ở bước lấy connection của riêng listener. Sau lỗi trên cả
  ba stream, listener nhận lại lifecycle `COMPLETED`. Test xóa các stream của chính nó;
  không restart server hoặc sửa stream của workload khác. Đây là consumer recovery evidence,
  không phải benchmark hoặc bài test mất toàn bộ Redis server.
- Test Redis mặc định skip khi không có `F009_REDIS_SMOKE=true`; bật riêng bằng PowerShell:

```powershell
$env:F009_REDIS_SMOKE = 'true'
.\gradlew.bat --no-daemon --max-workers=2 :apps:api:test --tests '*RealtimeRedisRecoveryIntegrationTest'
```

- Kết quả targeted Redis smoke: `BUILD SUCCESSFUL`; regression trước sửa không nhận
  event terminal sau khi kết nối phục hồi vì listener bị cancel.
- Python: `.venv/f009/Scripts/python.exe -m pytest apps/sentiment/tests -q -p no:cacheprovider`
  — `10 passed`; một deprecation warning từ Starlette/AnyIO, không có failure.
- OpenAPI YAML parse: pass (`3.1.0`, 23 paths). `git diff --check`: pass.

### Gate tổng sau hardening (2026-09-03)

- Cùng baseline/working tree và môi trường nêu trên. PostgreSQL fixture:
  `crypto-f010-postgres-hardening`, database `crypto_f010_evidence`, cổng `55432`;
  dùng schema F-010 đã có, không sửa hoặc áp dụng migration trong lượt này.
- Bật `F009_REDIS_SMOKE=true`, cấu hình `DATABASE_URL`, `DATABASE_USERNAME`,
  `DATABASE_PASSWORD` bằng credential fixture local (không ghi credential vào artifact), rồi chạy:

```powershell
.\gradlew.bat --no-daemon --no-configuration-cache --no-parallel --max-workers=1 --rerun-tasks test :modules:persistence:experimentIntegrationTest
```

- Toàn bộ Java `test` tasks được thực thi lại: **507 tests, 0 failures/errors/skips**,
  gồm **151 API tests** (Redis smoke thực sự chạy) và **32 architecture tests**.
- Lệnh tổng ban đầu fail riêng ở PostgreSQL fixture Outbox dùng message ID cố định từ
  lần chạy trước. Đã sửa fixture dùng ID mới, transaction rollback và `@RepeatedTest(2)`;
  assertion xác nhận không còn Outbox/processed-message do test tạo sau rollback.
- Chạy lại toàn bộ PostgreSQL suite sau sửa (test task thực thi, không lấy kết quả cache):

```powershell
.\gradlew.bat --no-daemon --no-configuration-cache --no-parallel --max-workers=1 :modules:persistence:experimentIntegrationTest
```

- **BUILD SUCCESSFUL**, **29 tests, 0 failures/errors/skips**. Chỉ test fixture thay đổi
  sau lượt 507 Java tests; production code giữ nguyên.
- Gradle configuration cache từng tham chiếu generated Kotlin source không còn tồn tại;
  chạy tuần tự, tắt configuration cache đã vượt qua lỗi này. Kotlin daemon fallback và
  warning serial/unchecked có sẵn trong các module cũ vẫn xuất hiện; không có warning
  biên dịch mới từ các file F-009 được sửa.
- `.specify/extensions.yml` không tồn tại: không có before/after-implement hook cần chạy.

**Merge gate**: ADR-0015 vẫn `Proposed`. Phải có phê duyệt `Accepted` trước merge;
evidence trên branch không tự thay đổi trạng thái ADR. Các con số latency/30 phút demo
production không được suy ra từ unit tests hoặc smoke này.
