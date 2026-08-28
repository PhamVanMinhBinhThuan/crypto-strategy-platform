# Hướng dẫn review Java Backend Foundation

## Mục đích

Review chéo xác nhận F-002 cung cấp đúng extension point để F-003, F-004 và F-005
phát triển độc lập mà không phá module boundary. Review không yêu cầu Supabase credential
và không được âm thầm nới dependency hoặc sửa ADR.

## Hai lượt review

1. **T024 — Boundary review**: kiểm tra dependency matrix và public/internal package có
   đủ cho feature sắp triển khai hay không.
2. **T046 — Final review**: sau khi mọi góp ý đã xử lý, chạy lại build tại merge-candidate
   commit và xác nhận phạm vi mình phụ trách vẫn sử dụng được.

Nếu review bắt đầu khi F-002 đã ở merge-candidate commit và sau đó không còn thay đổi
code/boundary, một phiên review có thể xác nhận đồng thời T024 và T046; vẫn ghi đủ hai
dòng evidence với cùng commit. Nếu có sửa đổi sau review, T046 phải chạy lại trên commit mới.

## Các bước chung cho mỗi reviewer

1. Checkout/pull nhánh `feature/002-java-backend-foundation` và ghi lại commit:

   ```bash
   git rev-parse HEAD
   ```

2. Dùng JDK 21 và chạy build offline:

   ```bash
   java -version
   ./gradlew clean check
   ```

3. Đọc:

   - `specs/002-java-backend-foundation/contracts/module-boundaries.md`;
   - `docs/adr/0002-module-boundaries.md`;
   - các dòng project/package thuộc phạm vi review trong Gradle và source tree.

4. Trả lời checklist theo phạm vi bên dưới. Nếu cần dependency ngoài matrix, ghi
   `CHANGES_REQUIRED` và nêu cạnh dependency cần thêm; không tự sửa ADR-0002 hoặc nới
   ArchUnit rule.
5. Gửi Luật: tên reviewer, commit, `APPROVED` hoặc `CHANGES_REQUIRED`, ghi chú ngắn và
   kết quả `./gradlew clean check`. Luật ghi kết quả vào `verification-evidence.md`.

## Checklist theo thành viên

### Nghi Văn — Market/Data cho F-003

- `market-data` có thể định nghĩa Asset/Pair/Candle/Timeframe và provider/persistence
  port qua public package mà chỉ phụ thuộc `domain`.
- PostgreSQL/Binance adapter không bị đưa vào pure domain package.
- `persistence`, `apps/api` và `apps/worker` có thể sử dụng public port mà không import
  package `internal` hoặc tạo dependency ngược.

### Văn Minh — Strategy và Job–Execution Attempt cho F-004/F-005

- `strategy-core`, `strategies`, `combination` và `backtesting` có đủ dependency đã cho
  phép để giữ Strategy deterministic, không phụ thuộc Spring/database/network.
- Public boundary có vị trí phù hợp cho Job lifecycle command/query và
  `Candidate → Job → Execution Attempt`; queue message vẫn thuộc `contracts`.
- Worker có thể mapping integration message thành command nội bộ mà không truy cập
  package `internal` của capability.
- Nếu Job cần một cạnh dependency chưa có trong matrix, ghi rõ cạnh đó để review
  ADR-0002 trước F-005; không nới rule ngay trong F-002.

### Tiến — Experiment persistence/Outbox/Worker cho F-005/F-007

- `experiment` có thể công bố input/output port mà không phụ thuộc persistence adapter.
- `persistence` có thể implement public output port cho Experiment, Job, Attempt,
  Idempotency và Outbox mà capability không import ngược adapter.
- `apps/worker` có thể gọi public capability API và `contracts`; retry/recovery không
  buộc domain phụ thuộc Redis hoặc Spring.
- Contract Job–Attempt do Văn Minh xác nhận có thể được persist và xử lý transactionally
  mà không tạo dependency cycle.

## Mẫu kết quả gửi Luật

```text
Reviewer: <tên>
Review: T024 Boundary | T046 Final
Commit: <git rev-parse HEAD>
Scope: <Market/Data | Strategy + Job/Attempt | Persistence/Outbox/Worker>
Build: PASS | FAIL
Decision: APPROVED | CHANGES_REQUIRED
Notes: <không có hoặc mô tả thay đổi cần thiết>
```

T024/T046 chỉ hoàn thành khi cả ba reviewer có kết quả theo mẫu và không còn
`CHANGES_REQUIRED` chưa xử lý.
