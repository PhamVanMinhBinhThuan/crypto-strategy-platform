# F014 Rubric Evidence Matrix

## Quy tắc sử dụng

- Nguồn: `File Danh Gia Copy.xlsx`, sheet `1_TU_DANH_GIA`, dòng 16–39.
- Có 23 tiêu chí cốt lõi và 1 dòng mở rộng có giá trị. Dòng 24 không tạo nghĩa vụ xây feature mới.
- `VERIFIED` chỉ dùng khi có kết quả chạy thật gắn commit và môi trường. Code hoặc tài liệu có sẵn nhưng chưa kiểm chứng vẫn là `PLANNED`/`PARTIAL`.
- Baseline được chụp trên base commit `50c28d99c02a4ee28ed1109b231daa4397a22fe4` với working tree dirty; xem `docs/evidence/f014/baseline.md`.

## Matrix

| # | Nhóm | Tiêu chí | Điểm | Requirement | Owner | Trạng thái T050 | Evidence hiện có | Gap hoặc remediation còn lại |
|---:|---|---|---:|---|---|---|---|---|
| 1 | Kiến trúc | Khả năng mở rộng Strategy/Plugin | 7 | FR-004, SC-003 | Strategy Core / Strategies | **VERIFIED** | `foundation-gates.md`: 72/72 foundation tests; RSI, Bollinger và S/R dùng chung `StrategyPlugin`/registry; ADR-0005 | Chạy lại trên commit sạch ở T054 để nâng thành release evidence |
| 2 | Kiến trúc | Tách trách nhiệm và giảm coupling | 6 | FR-002, FR-007 | Architecture / capability owners | **PARTIAL** | 32 architecture tests pass; `integration-gap-register.md`; `data-flows.md` | T062 còn phải rà ownership/public dependency trên commit cuối |
| 3 | Kiến trúc | Khả năng thay thế thành phần | 5 | FR-006, FR-010 | Search / Market / Sentiment | **PARTIAL** | Search generator, Market provider và Sentiment đều qua port/adapter; ADR-0003/0008/0010; contract tests pass | Chưa có một demo thay implementation live và đo kết quả tương đương |
| 4 | Kiến trúc | Scalability và Performance | 5 | FR-028, SC-009 | Search / Worker / API | **PARTIAL** | `performance.md`: ba run cùng workload, median in-process speedup 3v1 đạt 2.298×, 0 timeout/duplicate | Chưa phải multi-process Worker/live infrastructure benchmark; T061 còn phải gắn final clean SHA |
| 5 | Kiến trúc | Realtime và Multi-timeframe | 4 | FR-003, FR-012, FR-016, SC-002 | Market Data / Web | **PARTIAL** | `f014-market-demo.spec.ts` pass; recovery/reconcile tests pass; bốn panel giữ selection độc lập | Chưa có Binance/WebSocket LIVE screenshot và reconnect timeline |
| 6 | Kiến trúc | Reliability và Observability | 4 | FR-011–FR-016, SC-005 | API / Worker / Web | **PARTIAL** | `failure-recovery.md`: Redis thật reclaim/dedup VERIFIED; backend/UI Sentiment recovery pass có kiểm soát | External Sentiment stop/restart và full live timeline vẫn BLOCKED |
| 7 | Kiến trúc | Reproducibility và Versioning | 4 | FR-018–FR-021, SC-004, SC-006 | Experiment / Result / Leaderboard | **PARTIAL** | `reproduction.md`: immutable graph/provenance/verdict pass API + controlled browser | Shared DB thiếu F006 nên chưa có live source/target/result/verdict IDs |
| 8 | Chức năng | Market Data và Candlestick | 6 | FR-001, FR-003, FR-012 | Market Data / Web | **PARTIAL** | Four-chart contract test pass; candle schema, volume, pair/timeframe/freshness được render | Chưa kiểm chứng historical + realtime Binance trong authenticated LIVE Web |
| 9 | Chức năng | Strategy Engine có ít nhất 4 strategy | 5 | FR-004, SC-003 | Strategies | **VERIFIED** | MA, RSI, Bollinger Bands, Support/Resistance có registry identity duy nhất; 18 strategy tests + API catalog pass | Chụp catalog LIVE để bổ sung minh chứng trình bày, không phải gap code |
| 10 | Chức năng | Composite Strategy | 5 | FR-005 | Combination / Strategy / Web | **PARTIAL** | `f014-research-flow.spec.ts` tạo/publish composite, dùng `majority-vote@1.0.0`, tie → HOLD | Chưa có published version ID và ảnh từ user/session LIVE |
| 11 | Chức năng | Backtesting Engine | 6 | FR-007, FR-008 | Backtesting / Worker | **PARTIAL** | API research integration + controlled flow sinh authoritative Result/Trades; Entry/Exit/order consistency được test | Shared DB migration chặn full Worker/PostgreSQL live result |
| 12 | Chức năng | Strategy Evaluation | 4 | FR-008 | Evaluation | **PARTIAL** | Result contract/UI test bốn metrics Return, Win Rate, MDD, Number of Trades | Chưa có một Result ID LIVE truy từ Leaderboard |
| 13 | Chức năng | Strategy Search và Stop Condition | 5 | FR-006, FR-007 | Search / Experiment | **PARTIAL** | Random Search, seed, bounded space/maximumCandidates và terminal progress pass integration/controlled E2E | Chưa chạy pipeline live đến terminal trên DB đúng migration |
| 14 | Chức năng | Leaderboard Top-K | 3 | FR-009 | Leaderboard / Web | **PARTIAL** | Realtime payload/revision validation, stable ordering và Result link pass automated tests | Thiếu revision ID/fingerprint từ live terminal Experiment |
| 15 | Chức năng | Visualization Strategy và Trade | 3 | FR-008 | Backtesting / Web | **PARTIAL** | `TradeHistory` hiển thị Entry/Exit, side, exit reason, execution rows và highlight từ dữ liệu thật của Result contract | Contract chưa lưu indicator-level reasoning/zone; không được browser tự dựng; thiếu ảnh LIVE |
| 16 | Chức năng | News Collector | 2 | FR-010 | News / Worker | **PARTIAL** | Provider abstraction, normalize/dedup/state contract và News controlled E2E pass; degraded News vẫn đọc được | Chưa có News ID từ provider/storage LIVE |
| 17 | Chức năng | Sentiment Analysis | 1 | FR-010, FR-011 | Sentiment / News | **PARTIAL** | 10 Python tests; Java state/retry integration; UI label/confidence/polarity/disclaimer controlled pass | External TensorFlow process chưa READY; chưa có stored inference result LIVE |
| 18 | Hồ sơ | Source code và README | 4 | FR-022, FR-026 | Repository / Documentation | **VERIFIED** | Root `README.md` có install/run và F014 entry point; runbook/checklist/dry-run liên kết được | Cần cập nhật commit SHA sau commit cuối, nhưng bộ hướng dẫn hiện đã dùng được |
| 19 | Hồ sơ | Architecture Document | 6 | FR-026 | Architecture | **PARTIAL** | Context/container/module/data model/data flow có sẵn; T048 đã đồng bộ main flow, source of truth và failure boundary | T062 còn rà consistency và link hỏng trên commit cuối |
| 20 | Hồ sơ | Architectural Decision Records | 3 | FR-026 | Architecture | **VERIFIED** | `docs/adr/README.md` lập index 16 ADR; ADR-0005/0006/0007/0008/0009/0016 phủ plugin, worker/Redis, sentiment, reproduction và Search | T062 chỉ còn final consistency review |
| 21 | Hồ sơ | Video/demo và link minh chứng | 2 | FR-023–FR-025 | Demo / Documentation | **BLOCKED** | Runbook, checklist và danh sách 10 ảnh đã sẵn sàng | Chưa có video/Drive link; nhóm phải quay phiên LIVE sau khi unblock database/auth/Sentiment |
| 22 | Demo | Demo end-to-end | 3 | FR-001, SC-001 | Tất cả capability owners | **BLOCKED** | `runbook-dry-run.md`: controlled fallback 8/8 pass; API/Worker health từng pass | Chưa chạy full LIVE journey ≤10 phút; không dùng controlled suite thay thế |
| 23 | Demo | Scenario kiến trúc/xử lý lỗi | 2 | FR-011–FR-021, SC-005–SC-006 | Architecture / API / Worker | **PARTIAL** | Redis/Worker reclaim dùng Redis thật; realtime và Sentiment failure/recovery pass backend + controlled browser | Cần external Sentiment stop/restart và ảnh/timeline live cho ít nhất hai scenario trình bày |
| 24 | Mở rộng | Phần nâng cao có mục tiêu kiến trúc rõ | 5 | FR-024 | Nhóm theo capability | **NO_CLAIM** | `advanced-evidence.md` ghi rõ ML, Redis/Worker Pool và Loop Engineering là ứng viên | Chưa có demo + measurement vượt yêu cầu cốt lõi; không tự khai điểm |

## Tóm tắt tại T050

- Trong 23 tiêu chí cốt lõi: `VERIFIED` 4, `PARTIAL` 17, `PLANNED` 0 và `BLOCKED` 2.
- Hai blocker trực tiếp là hồ sơ video/Drive và full live E2E; nguyên nhân runtime cụ thể được ghi trong
  `runbook-dry-run.md`, không bị thay bằng fixture evidence.
- Dòng mở rộng: `NO_CLAIM`. Hệ thống có implementation ML/Redis/Loop đáng trình bày, nhưng chưa đủ
  measurement và live demonstration để tuyên bố điểm nâng cao.
- Đây là trạng thái task/evidence trên working tree. Sau commit cuối, T054/T061 phải chạy lại và gắn
  SHA mới trước khi dùng làm release evidence.

## Minh chứng có thể đưa vào sheet ngay

- Dòng 1 và 9: report 72 foundation tests, sơ đồ/plugin contract và ảnh catalog bốn Strategy. Nếu ảnh
  lấy từ Playwright thì ghi `CONTROLLED/TEST`; sau live rerun thay bằng ảnh LIVE.
- Dòng 18–20: link README, kiến trúc data flow và ADR index có thể nộp dưới dạng hồ sơ repository.
- Dòng 6/23: report Redis reclaim/dedup có dependency thật; không dùng phần Sentiment controlled để
  tuyên bố external recovery.
- Dòng 5, 7–17: có thể dùng ảnh để diễn tập trình bày nhưng hiện chỉ được gắn `CONTROLLED/TEST`.
- Dòng 21–22 chưa có minh chứng đạt; cần quay/chụp từ phiên LIVE sau khi blocker được xử lý.
