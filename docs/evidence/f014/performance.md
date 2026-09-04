# F014 Performance Evidence

## EV-US5-PERFORMANCE-001: Backtest worker scaling 1 → 3

- Criterion/requirement: T052, T055, FR-028, SC-009; cùng workload chạy ba lần, báo từng kết quả, median, timeout và duplicate.
- Status: PARTIAL
- Commit SHA: `50c28d99c02a4ee28ed1109b231daa4397a22fe4`
- Working tree: dirty (implementation và tài liệu F014 chưa commit).
- Captured at: `2026-09-04T10:15:57Z`
- Environment/profile: `IN_PROCESS_BACKTEST`; macOS Darwin 25.5.0 arm64, Apple M2, 8 logical CPU, 16 GiB RAM, Temurin Java 21.0.12.1.
- Non-secret configuration: 12 candidates/run, 2.000 one-minute candles/candidate, MA Crossover `(5,25)`, batch 256, concurrency 1 so với 3, timeout 60 giây, p95 budget 2 giây/candidate.
- Command/action: `JAVA_HOME=<JDK_21> ./gradlew clean check --no-daemon`; raw output nằm trong XML của `F014DemoPerformanceTest`.
- Expected result: ba lần hoàn tất đủ candidate, không timeout/duplicate, p95 dưới budget và median throughput speedup tối thiểu 2×.
- Observed result: 36/36 candidate ở mỗi profile hoàn tất qua ba lần, 0 timeout, 0 duplicate Result ID, 0 duplicate Candidate ID; median speedup `2.298×`; mọi candidate p95 dưới 29 ms.
- Artifact links: `apps/api/build/test-results/test/TEST-com.cryptostrategy.platform.api.performance.F014DemoPerformanceTest.xml`; `apps/api/src/test/java/com/cryptostrategy/platform/api/performance/F014DemoPerformanceTest.java`.
- Limitations: đây là compute benchmark trong một JVM, không gồm startup, HTTP, Redis, PostgreSQL, network hoặc nhiều process/host; không phải production SLA hay bằng chứng live end-to-end. Kết quả chưa gắn final clean commit nên chỉ chuyển `VERIFIED` sau T061 rerun.
- Owner/reviewer: implementer F014 / pending reviewer.

## Raw results

| Run | Concurrency | Compute (ms) | Total (ms) | Candidate p95 (ms) | Throughput (candidate/s) | Speedup 3v1 | Timeout | Duplicate IDs |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 1 | 135.717 | 135.845 | 21.193 | 88.419 | 1.849× | 0 | 0 |
| 1 | 3 | 73.390 | 73.582 | 28.312 | 163.510 | 1.849× | 0 | 0 |
| 2 | 1 | 113.785 | 113.880 | 17.136 | 105.462 | 2.474× | 0 | 0 |
| 2 | 3 | 45.986 | 46.075 | 14.847 | 260.947 | 2.474× | 0 | 0 |
| 3 | 1 | 76.602 | 76.669 | 10.250 | 156.653 | 2.298× | 0 | 0 |
| 3 | 3 | 33.327 | 33.396 | 9.465 | 360.065 | 2.298× | 0 | 0 |

Median của ba speedup `[1.849, 2.474, 2.298]` là **2.298×**. Harness đổi thứ tự profile ở run chẵn để profile 3 worker không luôn hưởng lợi từ JIT/cache chạy sau; đây vẫn là microbenchmark nên cần giữ đúng giới hạn diễn giải ở trên.
