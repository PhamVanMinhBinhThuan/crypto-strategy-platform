# Research: F-014 End-to-End Demo and Hardening

## R1 — Phạm vi

**Decision**: Integration-first, chỉ remediation gap bắt buộc; không thêm trading thật hoặc feature nâng cao.
**Rationale**: Capability đã có từ F001–F013; rủi ro chính là tích hợp và evidence, không phải thiếu một kiến trúc mới.

**Alternatives considered**: Mở rộng thêm feature bonus; loại vì làm loãng các gate bắt buộc.

## R2 — Live và fallback

**Decision**: Live profile là demo chính; fixture bật chủ động, tách riêng và luôn có nhãn `DEMO/FIXTURE`.
**Rationale**: Đáp ứng bằng chứng Binance thật nhưng vẫn có đường diễn tập xác định khi provider lỗi.

**Alternatives considered**: Chỉ dùng fixture hoặc không có fallback; cả hai đều không đáp ứng đồng thời tính trung thực và khả năng diễn tập.

## R3 — Bốn Strategy

**Decision**: Giữ MA; thêm RSI, Bollinger Bands và Support/Resistance trong `modules/strategies` qua contract/registry hiện tại.
**Rationale**: Baseline chỉ đăng ký MA; ADR plugin đã accepted nên không đổi engine hoặc cho user chạy source tùy ý.

**Alternatives considered**: Hard-code trong engine hoặc upload code người dùng; loại vì phá extensibility và security boundary.

## R4 — Frontend boundary

**Decision**: UI dùng public API/WebSocket F-011/F-013; Search, Backtest, Evaluation, Ranking và provenance do backend xử lý.
**Rationale**: Bảo toàn authorization và không biến prototype simulation thành logic production.

**Alternatives considered**: Tính toán trên browser; loại vì tạo hai nguồn sự thật và bỏ qua application authorization.

## R5 — Môi trường

**Decision**: Chuẩn hóa startup order, health check và profile bằng config/runbook hiện có; chỉ thêm script nhỏ nếu cần.
**Rationale**: Không có driver cho deployment platform hoặc service mới.

**Alternatives considered**: Thêm Docker/Kubernetes orchestration mới; hoãn vì vượt phạm vi hardening hiện tại.

## R6 — Evidence

**Decision**: Mỗi tiêu chí có Evidence Record trạng thái `PLANNED`, `BLOCKED`, `PARTIAL`, `VERIFIED`, gắn commit/môi trường/artifact thật.
**Rationale**: Test skip hoặc số liệu dự kiến không phải pass.

**Alternatives considered**: Checklist pass/fail đơn giản; loại vì không biểu diễn được blocked/partial và provenance.

## R7 — Failure scenarios

**Decision**: Tối thiểu chạy Sentiment down nhưng luồng kỹ thuật tiếp tục, và Worker/queue interruption được recover không tạo outcome trùng.
**Rationale**: Bao phủ isolation cùng durability/idempotency.

**Alternatives considered**: Chỉ unit test; loại vì không chứng minh recovery xuyên boundary.

## R8 — Performance

**Decision**: Dùng harness hiện có, workload cố định, ba lần chạy, lưu từng kết quả và median; không gọi đây là production SLA.
**Rationale**: Kết quả trung thực và lặp lại được.

**Alternatives considered**: Một lần chạy hoặc chỉ công bố lần tốt nhất; loại vì dễ gây sai lệch.

## R9 — Security

**Decision**: Scan tracked source, config, committed evidence và browser artifact; kết hợp secret scan với redaction/browser-boundary tests.
**Rationale**: Secret có thể rò ở repository, bundle hoặc log; credential thật luôn cấp ngoài repo.

**Alternatives considered**: Chỉ scan source; loại vì bỏ sót bundle và demo artifact.

## R10 — Persistence

**Decision**: Không dự kiến schema change; nếu audit chứng minh cần, chỉ dùng forward migration của đúng owner.
**Rationale**: Không sửa migration đã áp dụng và không nhân đôi model provenance hiện có.

**Alternatives considered**: Tạo schema evidence riêng ngay; loại vì chưa có gap persistence được chứng minh.
