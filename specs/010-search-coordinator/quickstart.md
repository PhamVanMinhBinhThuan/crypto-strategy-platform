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
