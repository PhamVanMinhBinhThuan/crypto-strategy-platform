# Architecture Evidence Matrix

**Status**: Planned
**Last Updated**: 2026-08-12

Tài liệu này không tuyên bố hệ thống đã đạt các ngưỡng. Repository chưa có implementation để tạo evidence thực nghiệm; mỗi dòng chỉ định chính xác bằng chứng phải thu thập trước khi ADR liên quan được chuyển sang `Accepted`.

| Proof | Quyết định/QA | Cách kiểm chứng | Expected measure | Artifact phải lưu | Status |
| --- | --- | --- | --- | --- | --- |
| AP-01 Strategy extension | ADR-0005 / QA-01 | Thêm MACD plugin và review diff | Không sửa Backtester, Evaluator, Leaderboard, UI | PR diff, contract test report | Planned |
| AP-02 Search replacement | ADR-0011 / QA-02 | Thêm Domain-guided generator | Không sửa downstream pipeline | PR diff, generator tests | Planned |
| AP-03 Provider replacement | ADR-0003 / QA-03 | Chạy cùng contract tests với fixture adapter | Public contract và consumer không đổi | Contract test report | Planned |
| AP-04 Realtime recovery | ADR-0003, ADR-0004 / QA-04 | Ngắt upstream, ghi timeline reconnect/backfill | ≤30 giây, 0 missing/duplicate closed Candle | Test log, candle reconciliation report | Planned |
| AP-05 Worker scale | ADR-0006 / QA-05 | Benchmark cùng workload với 1 và 3 workers | ≥2× throughput, 0 duplicate Result | Benchmark CSV/report | Planned |
| AP-06 Failure isolation | ADR-0008 / QA-06 | Dừng Sentiment Service khi chart chạy | Chart không gián đoạn, degraded ≤5 giây | Screen recording, logs | Planned |
| AP-07 Reproduction | ADR-0009 / QA-07 | Replay immutable manifest | Trades, 4 metrics và fingerprint khớp | Manifest, result comparison | Planned |
| AP-08 Observability | ADR-0006 / QA-08 | Trace một candidate thành công và một job lỗi | IDs xuyên suốt, UI update ≤5 giây | Log/metric snapshot | Planned |
| AP-09 Realtime latency | ADR-0004 / QA-09 | Đo bốn chart trong demo workload | p95 ≤1 giây, chart độc lập | Latency report, UI test | Planned |
| AP-10 Async search | ADR-0006 / QA-10 | Start Search 1.000 candidates | REST ≤2 giây, bounded queue | API timing, queue metrics | Planned |

## Evidence lifecycle

1. Thu thập artifact từ test/demo có commit hoặc release version rõ ràng.
2. Ghi ngày, môi trường, cấu hình và người thực hiện.
3. Đổi `Status` của proof thành `Verified` chỉ khi expected measure đạt.
4. Nếu không đạt, giữ ADR `Proposed`, ghi mismatch và điều chỉnh decision hoặc measure qua review.
5. Không dùng số liệu minh họa, screenshot dựng hoặc kết quả từ workload khác làm evidence.
