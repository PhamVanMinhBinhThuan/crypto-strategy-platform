# F014 Performance Evidence

## EV-US5-PERFORMANCE-001: Backtest concurrency 1 → 3

- Criterion/requirement: T052, T055, T061, FR-028, SC-009; cùng workload chạy ba lần, báo từng kết quả, median, timeout và duplicate.
- Status: VERIFIED
- Commit SHA: `0761a54bfcbb93c3b24cb216b19d6cc79e03e21b`
- Working tree: clean detached worktree khi bắt đầu benchmark.
- Captured at: `2026-09-04T12:22:15Z`
- Environment/profile: `IN_PROCESS_BACKTEST`; macOS Darwin 25.5.0 arm64, Apple M2, 8 logical CPU, 16 GiB RAM, Temurin Java 21.0.12.1.
- Non-secret configuration: 12 candidates/run, 2.000 one-minute candles/candidate, MA Crossover `(5,25)`, batch 256, concurrency 1 so với 3, timeout 60 giây, p95 budget 2 giây/candidate.
- Command/action: sau full gate, chạy standalone `JAVA_HOME=<JDK_21> ./gradlew :apps:api:test --tests '*F014DemoPerformanceTest' --rerun-tasks --no-daemon`; raw output nằm trong XML của `F014DemoPerformanceTest`.
- Expected result: ba lần hoàn tất đủ candidate, không timeout/duplicate, p95 dưới budget và median throughput speedup tối thiểu 2×.
- Observed result: 36/36 candidate ở mỗi profile hoàn tất, 0 timeout và 0 duplicate identity; median speedup `2.527×`; mọi candidate p95 dưới 16 ms. Mục tiêu scaling 2× đạt trong standalone profile.
- Artifact links: `apps/api/build/test-results/test/TEST-com.cryptostrategy.platform.api.performance.F014DemoPerformanceTest.xml`; `apps/api/src/test/java/com/cryptostrategy/platform/api/performance/F014DemoPerformanceTest.java`.
- Limitations: compute benchmark trong một JVM, không gồm startup, HTTP, Redis, PostgreSQL, network hoặc nhiều process/host; không phải production SLA. Lần chạy cùng `npm ci` chỉ đạt 1.836×, cho thấy số đo nhạy với host contention; vì vậy chỉ claim target cho standalone profile đã công bố.
- Owner/reviewer: implementer F014 / pending reviewer.

## Raw results

| Run | Concurrency | Compute (ms) | Total (ms) | Candidate p95 (ms) | Throughput (candidate/s) | Speedup 3v1 | Timeout | Duplicate IDs |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 1 | 129.350 | 129.443 | 15.205 | 92.771 | 2.527× | 0 | 0 |
| 1 | 3 | 51.181 | 51.239 | 13.343 | 234.462 | 2.527× | 0 | 0 |
| 2 | 1 | 71.633 | 71.697 | 7.343 | 167.521 | 1.697× | 0 | 0 |
| 2 | 3 | 42.201 | 42.260 | 11.416 | 284.353 | 1.697× | 0 | 0 |
| 3 | 1 | 66.290 | 66.360 | 6.473 | 181.023 | 2.534× | 0 | 0 |
| 3 | 3 | 26.164 | 26.261 | 7.257 | 458.638 | 2.534× | 0 | 0 |

Median standalone của `[2.527, 1.697, 2.534]` là **2.527×**. Harness đổi thứ tự profile ở run chẵn để profile 3 worker không luôn hưởng lợi từ JIT/cache chạy sau.

## Quan sát bị nhiễu vẫn được giữ

Trong full gate chạy đồng thời với `npm ci`, cùng harness cho các speedup `[1.836, 1.445, 1.970]`, median
`1.836×`. Bộ này không dùng làm measurement chính vì có host workload cạnh tranh, nhưng được giữ để
không cherry-pick và để nhắc rằng benchmark phải chạy standalone theo command đã công bố.
