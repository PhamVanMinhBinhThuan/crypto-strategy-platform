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

## Readiness snapshot (2026-09-02)

| Capability | Trạng thái | Evidence |
| --- | --- | --- |
| Auth/error/ownership/idempotency | Ready | API integration và redaction tests |
| Market, Dataset, Strategy | Ready | Contract, ownership và conflict tests |
| Standalone Backtest start/read | Ready | Atomic aggregate + API/result tests |
| Experiment read/stop, Job read/cancel | Ready | Command/read/state tests |
| Experiment start/reproduce | Gated | Trả `503 DEPENDENCY_UNAVAILABLE` đến khi Search Coordinator có runtime boundary |
| Realtime Candle/workload/leaderboard | Code-ready | Protocol, lifecycle, backpressure, recovery và Redis consumer tests; production Redis smoke chưa chạy |
| Public News và protected audit | Ready | Public/degraded/security tests; audit giữ route `/internal/news-items/**` |

Redis workload streams mặc định bật trong runtime bằng
`platform.realtime.streams.enabled=true`; Gradle unit test tắt listener để không phụ thuộc
dịch vụ ngoài. Các stream là `work.progress`, `work.lifecycle` và
`work.candidate-evaluated`.

### Evidence đã chạy

- `JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew test`
  — `BUILD SUCCESSFUL` (68 actionable tasks; full repository suite).
- `/tmp/crypto-sentiment-f009/bin/python -m pytest apps/sentiment/tests -q`
  — `10 passed`.
- OpenAPI YAML parse và documentation parity test — pass.

### Evidence chưa thể chạy trong môi trường hiện tại

Tại thời điểm kiểm chứng, `DATABASE_URL`, `SUPABASE_DB_URL`,
`SPRING_DATASOURCE_URL`, `REDIS_URL` và `SPRING_DATA_REDIS_HOST` đều chưa được cấu hình.
Vì vậy T074 vẫn mở: chưa claim PostgreSQL/Supabase/Redis runtime integration hoặc latency
production. Việc này không làm transport giả vờ hoạt động; Experiment start/reproduce giữ
readiness gate và REST snapshot vẫn là nguồn trạng thái authoritative.
