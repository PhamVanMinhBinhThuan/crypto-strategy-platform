# 11. Nhóm dùng ADD như thế nào để thiết kế kiến trúc?

## Trả lời ngắn

ADD (Attribute-Driven Design) là cách thiết kế kiến trúc bắt đầu từ yêu cầu quan trọng, sau đó mới chọn giải pháp kỹ thuật. Nhóm không chọn Redis và Worker chỉ vì công nghệ này phổ biến. Nhóm xuất phát từ yêu cầu phải chạy nhiều Backtest mà không làm API bị chặn, rồi mới chọn Queue và Worker làm giải pháp.

Luồng suy nghĩ chính:

```text
Yêu cầu quan trọng
→ tình huống cần giải quyết
→ giải pháp kiến trúc
→ chia trách nhiệm và contract
→ kiểm tra kết quả
→ chưa đạt thì điều chỉnh
```

## Ví dụ: mở rộng số lượng Backtest

Yêu cầu của hệ thống là xử lý nhiều candidate. Nếu API tự chạy toàn bộ Backtest, request có thể timeout và API bị chiếm tài nguyên.

Nhóm áp dụng ADD như sau:

| Bước | Áp dụng trong dự án |
| --- | --- |
| Xác định mục tiêu | Chạy nhiều Backtest mà API vẫn phản hồi nhanh |
| Chọn yêu cầu quan trọng | Tăng từ 1 lên 3 Worker và cải thiện throughput |
| Xác định phần cần thiết kế | API, Job, Queue, Worker và persistence |
| Chọn giải pháp | Xử lý bất đồng bộ bằng Redis Streams và Worker pool |
| Chia trách nhiệm | API tạo Job; Queue chuyển việc; Worker chạy Backtest; PostgreSQL lưu trạng thái |
| Tạo contract | Message chỉ mang `experimentId`, `jobId` và `candidateId` |
| Kiểm tra | Đo throughput, recovery và kiểm tra không tạo kết quả trùng |
| Điều chỉnh | Nếu chưa đạt thì thay concurrency, batch size hoặc retry policy |

Bằng chứng về yêu cầu đo được: [`quality-attributes.md`, QA-05](../../architecture/quality-attributes.md).

## Từ yêu cầu đến quyết định kiến trúc

Yêu cầu:

```text
Chạy nhiều Backtest nhưng không block API.
```

Quyết định:

```text
API lưu Job và trả ID sớm
→ Redis Stream phân phối Job
→ Worker chạy Backtest
→ PostgreSQL lưu trạng thái và kết quả
```

Lý do và trade-off được ghi tại [ADR-0006 — Queue và Worker](../../adr/0006-queue-worker-backtesting.md).

## Contract giữa Queue và Worker

Queue không gửi toàn bộ Dataset hoặc object Java. Message chỉ chứa các ID cần thiết:

```java
public record BacktestJobPayload(
        String experimentId,
        String jobId,
        String candidateId
) {}
```

Worker dùng các ID này để lấy dữ liệu chuẩn rồi chạy nghiệp vụ. Cách này làm message nhỏ và giảm phụ thuộc giữa Queue với Backtester.

Bằng chứng: [`BacktestJobPayload.java`](../../../modules/contracts/src/main/java/com/cryptostrategy/platform/contracts/api/BacktestJobPayload.java) và [`MessageContractSerializationTest.java`](../../../modules/contracts/src/test/java/com/cryptostrategy/platform/contracts/api/MessageContractSerializationTest.java).

## Kiểm tra quyết định

ADD không kết thúc khi vẽ xong sơ đồ. Nhóm phải kiểm tra giải pháp có đáp ứng yêu cầu ban đầu không:

- Worker có nhận và xử lý Job đúng không;
- Worker crash có phục hồi được không;
- message được giao lại có tạo kết quả trùng không;
- tăng số Worker có làm throughput tăng không.

Bằng chứng hiện có: [`BacktestExecutionPipelineIntegrationTest.java`](../../../apps/worker/src/test/java/com/cryptostrategy/platform/worker/integration/BacktestExecutionPipelineIntegrationTest.java), [`StaleAttemptAndRecoveryIntegrationTest.java`](../../../apps/worker/src/test/java/com/cryptostrategy/platform/worker/integration/StaleAttemptAndRecoveryIntegrationTest.java) và [`DualLayerDedupIntegrationTest.java`](../../../apps/worker/src/test/java/com/cryptostrategy/platform/worker/integration/DualLayerDedupIntegrationTest.java).

Mục tiêu benchmark 1 Worker so với 3 Worker vẫn phải được đo trong cùng một môi trường kiểm thử trước khi tuyên bố đạt mức throughput đề ra.

## Vì sao ADD là vòng lặp?

Nếu kết quả đo chưa đạt yêu cầu, nhóm quay lại xem xét quyết định. Ví dụ, nhóm có thể điều chỉnh số Worker, concurrency, batch size hoặc database connection pool rồi đo lại.

Vì vậy ADD không phải quy trình làm một lần. Đây là vòng lặp:

```text
Thiết kế → kiểm tra → phát hiện vấn đề → điều chỉnh → kiểm tra lại
```

## Nhiều driver tạo ra nhiều quyết định

Queue và Worker chỉ là một ví dụ. Nhóm cũng dùng cách suy nghĩ tương tự cho các yêu cầu khác:

- Dễ thêm Strategy → Plugin và Registry — [ADR-0005](../../adr/0005-strategy-plugin-registry.md).
- Dễ thay Market Provider → Port và Adapter — [ADR-0003](../../adr/0003-market-data-adapter.md).
- Sentiment lỗi không ảnh hưởng Backtest → tách Python Service và Circuit Breaker — [ADR-0008](../../adr/0008-sentiment-service-boundary.md).
- Chạy lại Experiment → Frozen Manifest và fingerprint — [ADR-0009](../../adr/0009-reproducible-experiments.md).

## Nguồn đề bài

Slide 32 và slide 65–66 trong [slide kiến trúc](../../KienTrucDoAn_slide.pdf); Syllabus Topic 4 — Attribute-Driven Design.
