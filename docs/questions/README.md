# 10 câu hỏi bảo vệ kiến trúc — Crypto Strategy Lab

Tài liệu này trả lời 10 câu hỏi tại slide 39 (trang PDF 77) của [KienTrucDoAn_slide.pdf](../KienTrucDoAn_slide.pdf), dựa trên yêu cầu trong [Crypto Strategy Lab – Đồ án cuối kỳ.pdf](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf), tài liệu kiến trúc và source code hiện tại.

## Phần 1 — 10 câu bắt buộc (Slide 39 Checklist)

| # | Câu hỏi | Trạng thái câu trả lời |
| --- | --- | --- |
| 1 | [Architectural drivers là gì?](1_architectural_drivers/answer.md) | Thiết kế đầy đủ; một số QA còn chờ benchmark/demo |
| 2 | [C4 Context và Container của nhóm?](2_c4_context_container/answer.md) | Đã có thiết kế và phần lớn container backend |
| 3 | [Boundary của Market / Strategy / Experiment / News?](3_module_boundaries/answer.md) | Đã có module, public contract và architecture test |
| 4 | [Thêm strategy mới sửa ở đâu?](4_add_new_strategy/answer.md) | Plugin/Registry đã implement; hiện có MA Crossover |
| 5 | [Đổi search algorithm sửa ở đâu?](5_replace_search_algorithm/answer.md) | Contract/Registry là boundary; xem evidence trong source hiện tại |
| 6 | [Provider mới có làm frontend đổi?](6_replace_market_provider/answer.md) | Không, nếu adapter giữ canonical contract |
| 7 | [100.000 backtests scale thế nào?](7_scale_100000_backtests/answer.md) | Cơ chế đã có; benchmark mục tiêu còn Planned |
| 8 | [Service lỗi có lan failure không?](8_failure_isolation/answer.md) | Có isolation theo boundary; E2E demo còn cần đo |
| 9 | [Duplicate/retry/event order xử lý thế nào?](9_retry_duplicate_event_order/answer.md) | Idempotency/recovery/versioning đã có; exactly-once không được giả định |
| 10 | [Leaderboard result truy được provenance thế nào?](10_leaderboard_provenance/answer.md) | Frozen graph và reproduction workflow đã implement |

## Phần 2 — 13 câu bổ sung (từ nội dung slide và đề án)

Các câu này xuất hiện trong slide nhưng không nằm trong checklist 10 câu — thường được hỏi thêm trong vấn đáp.

### Nhóm A — Kiến trúc & Quyết định thiết kế (ATAM/ADD)

| # | Câu hỏi | Trạng thái |
| --- | --- | --- |
| 11 | [Nhóm dùng ADD như thế nào để ra quyết định kiến trúc?](11_add_method/answer.md) | Đã soạn; ví dụ: Scalability → Queue/Worker |
| 12 | [Trade-off quan trọng nhất của kiến trúc nhóm là gì?](12_trade_off/answer.md) | Đã soạn; bảng trade-off đầy đủ |
| 13 | [ATAM scenario nào nguy hiểm nhất và nhóm giải quyết ra sao?](13_atam_scenarios/answer.md) | Đã soạn; 5 scenario + điểm yếu thành thật |

### Nhóm B — Transaction / Consistency / Event

| # | Câu hỏi | Trạng thái |
| --- | --- | --- |
| 14 | [Một kết quả Backtest "hoàn tất" nghĩa là gì về mặt transaction?](14_transaction_boundary/answer.md) | Đã soạn; Outbox pattern + Worker crash |
| 15 | [Nhóm chọn Event-Driven cho phần nào? Tại sao không dùng Direct Call?](15_event_driven/answer.md) | Đã soạn; Event Catalog + driver |
| 16 | [CQRS có được dùng không? Ở đâu và tại sao?](16_cqrs/answer.md) | Đã soạn; Leaderboard projection + không dùng full CQRS |

### Nhóm C — Clean Architecture / DDD

| # | Câu hỏi | Trạng thái |
| --- | --- | --- |
| 17 | [Strategy có được gọi trực tiếp DB hay Binance không?](17_clean_architecture_dependency/answer.md) | Đã soạn; Dependency Rule + Port pattern |
| 18 | [Bounded Context của nhóm là gì? Signal vs Trade khác nhau thế nào?](18_bounded_context/answer.md) | Đã soạn; 4 BC + phân tích semantic |
| 19 | [Composite Strategy hoạt động thế nào? Ai quyết định khi 3 strategy "cãi nhau"?](19_composite_strategy/answer.md) | Đã soạn; Majority Vote + Weighted Vote |

### Nhóm D — MLOps / Sentiment

| # | Câu hỏi | Trạng thái |
| --- | --- | --- |
| 20 | [Kết quả Sentiment truy vết về model nào, version nào?](20_mlops_sentiment_versioning/answer.md) | Đã soạn; SentimentResult metadata + monitor |
| 21 | [Tại sao Sentiment là service Python riêng? Down thì cả hệ thống down không?](21_sentiment_service_isolation/answer.md) | Đã soạn; isolation boundary + circuit breaker |

### Nhóm E — Deployment / Scale

| # | Câu hỏi | Trạng thái |
| --- | --- | --- |
| 22 | [Modular Monolith vs Microservices: nhóm chọn gì và vì sao?](22_modular_monolith_vs_microservices/answer.md) | Đã soạn; driver-based decision |
| 23 | [Để Worker scale stateless, điều kiện bắt buộc là gì?](23_worker_stateless_idempotent/answer.md) | Đã soạn; stateless + idempotent + consumer group |

## Cách sử dụng

- Đọc mục **Trả lời ngắn** để thuyết trình trong khoảng 30–60 giây.
- Dùng Mermaid trong từng câu để giải thích trực quan.
- Mở các link **Bằng chứng trong project** khi giảng viên hỏi sâu.
- `Implemented` nghĩa là có source/test trong repository; `Planned` nghĩa là chưa có phép đo thật. Tài liệu không coi số liệu mục tiêu là kết quả đã đạt.


