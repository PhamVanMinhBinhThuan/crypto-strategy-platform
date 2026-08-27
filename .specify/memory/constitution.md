<!--
Báo cáo tác động đồng bộ
- Thay đổi phiên bản: template → 1.0.0
- Nguyên tắc mới: quản trị theo ADR; ownership rõ ràng; khả năng tái lập;
  contract có version; phân phối an toàn và kiểm chứng được
- Phần mới: Ràng buộc kiến trúc và dữ liệu; Quy trình và quality gate
- Phần bị loại bỏ: placeholder của template
- Nội dung trì hoãn: không có
-->

# Constitution của Crypto Strategy Platform

## Các nguyên tắc cốt lõi

### I. Quản trị theo ADR

Mọi quyết định kiến trúc quan trọng MUST được ghi trong ADR trước khi feature
specification, data model, contract, migration hoặc implementation phụ thuộc được
coi là đã phê duyệt. Artifact phụ thuộc MUST tuân theo ADR đang có hiệu lực và
MUST NOT sửa ADR chỉ để làm implementation có vẻ nhất quán.

ADR bổ sung hướng dẫn tương thích MUST khai báo Extends. Quyết định làm mất hiệu
lực ADR trước MUST khai báo Supersedes, liệt kê mọi ADR bị thay thế, nêu tác động
migration và được review rõ ràng. Lịch sử ADR MUST luôn đọc được.

### II. Ownership module và dữ liệu rõ ràng

Mỗi khái niệm nghiệp vụ và bản ghi bền vững MUST có đúng một capability sở hữu.
Module MUST cộng tác qua API, port hoặc message có version đã công bố và MUST NOT
import implementation, repository hoặc table mapping nội bộ của module khác.
Foreign key vật lý không cấp quyền ghi dữ liệu chéo module.

Hệ thống MUST giữ dependency direction trong ADR-0002. Model của provider,
transport, framework và persistence MUST NOT rò rỉ vào domain contract. Code dùng
chung chỉ chứa khái niệm liên capability thực sự ổn định; module common tổng quát
MUST NOT trở thành nơi chứa business model không có owner.

### III. Khả năng tái lập và bằng chứng bất biến

Mọi Experiment hoặc Backtest Result hiển thị cho user MUST truy được về Dataset
membership và checksum, Strategy hoặc Composite version và parameters, execution
assumptions, search seed/configuration, evaluation/ranking version, sentiment
provenance liên quan và software version.

Dataset snapshot, Strategy/Composite snapshot, Experiment Manifest, Candidate
Definition, Result đã chấp nhận, Trade, Evaluation và Leaderboard revision MUST
bất biến sau khi được chấp nhận. Retry MUST tạo Attempt, không tạo business outcome
trùng. Reproduction MUST tạo run mới liên kết bản gốc và MUST NOT overwrite bằng
chứng gốc. Retention MUST NOT xóa dữ liệu tái lập đang được tham chiếu.

### IV. Contract có version và cô lập provider

Boundary HTTP, WebSocket, queue, provider, Strategy, Generator và Sentiment công
khai MUST dùng contract rõ ràng, có version và canonical domain value. Breaking
change MUST qua quá trình chuyển contract/version đã duyệt và được phản ánh trong
specification liên quan; phải có ADR nếu ý nghĩa kiến trúc thay đổi.

Biểu diễn external provider MUST được chuẩn hóa tại adapter và MUST NOT thoát vào
domain logic hoặc contract hướng user. Strategy logic MUST deterministic với frozen
input và MUST NOT gọi trực tiếp network, database, clock hoặc model service.

### V. Phân phối an toàn, tin cậy và kiểm chứng được

Business data MUST được truy cập qua application boundary đã cấp quyền. Secret và
privileged credential MUST NOT xuất hiện trong browser bundle, source control, log
hoặc public error response. Thao tác trên dữ liệu có owner MUST xác thực identity
và kiểm tra ownership; identifier do client gửi một mình MUST NOT cấp quyền.

Durable business state MUST tồn tại khi cache hoặc queue bị mất. Luồng bất đồng bộ
MUST có idempotency, durable publication state, bounded retry và duplicate-safe
consumption. Mỗi requirement MUST có automated evidence tương xứng: unit test cho
domain invariant, contract test cho boundary, integration test cho persistence và
messaging, architecture test cho dependency rule.

## Ràng buộc kiến trúc và dữ liệu

- Java backend giữ Modular Monolith trừ khi ADR mới Supersedes ADR-0001.
- PostgreSQL-compatible storage là source of truth; cache và queue MUST có thể
  rebuild hoặc recover.
- Price, quantity, fee, money, rate và metric MUST dùng exact decimal semantics.
  Timestamp MUST biểu diễn instant/timezone rõ ràng và đi qua boundary dưới dạng UTC.
- Canonical identity, ordering, serialization, checksum, fingerprint và tie-break
  MUST deterministic và có version khi thay đổi logic có thể đổi output.
- Browser MUST NOT đọc hoặc ghi business table trực tiếp. Authentication có thể
  dùng boundary của ADR-0011; Java application vẫn thực thi authorization.
- Lỗi News/Sentiment MUST được cô lập khỏi Market Data và technical Backtest.
  Redis mất MUST NOT xóa Experiment, Result, News, Sentiment hoặc Outbox truth.
- Migration đã áp dụng trên môi trường dùng chung MUST NOT bị sửa. Schema change
  MUST dùng forward migration đã review và lưu trong repository.

## Quy trình phát triển và quality gate

1. Quyết định quan trọng bắt đầu bằng ADR hoặc reference tới ADR đang có hiệu lực.
2. Feature đi theo Spec Kit: specify, clarify khi cần, plan, tasks, consistency
   analysis, implementation và verification.
3. Specification mô tả user/business outcome; plan chọn implementation; tasks mô
   tả công việc theo dependency order và có thể kiểm chứng độc lập.
4. Trước implementation, reviewer MUST xác nhận ADR, architecture, API/event
   contract, data model và feature artifact không mâu thuẫn.
5. Implementation MUST bảo toàn thay đổi của user và MUST có test ánh xạ tới
   acceptance scenario, quality attribute và failure/recovery path.
6. Database change MUST qua static review và non-production dry run trước shared
   deployment. Áp dụng migration remote cần phê duyệt rõ ràng.
7. Pull request MUST nêu owner, ADR, contract, migration, security boundary và
   evidence bị ảnh hưởng. Vi phạm Constitution chưa giải quyết sẽ chặn merge.

## Quản trị

Constitution này quản lý Spec Kit artifact và implementation. ADR đang có hiệu lực
cung cấp quyết định chi tiết bên dưới. Nếu ADR và Constitution mâu thuẫn, công việc
MUST dừng tới khi được giải quyết bằng Constitution amendment hoặc ADR đúng quy
trình; implementation MUST NOT tự chọn âm thầm.

Amendment cần proposal, impact review trên template/spec/plan/task/ADR và sự chấp
thuận của user hoặc nhóm. Version theo Semantic Versioning: MAJOR khi loại bỏ hoặc
định nghĩa lại nguyên tắc không tương thích, MINOR khi thêm nguyên tắc hoặc mở rộng
governance đáng kể, PATCH khi làm rõ không đổi ngữ nghĩa. Mỗi amendment MUST cập
nhật báo cáo tác động, version và ngày sửa.

Compliance MUST được review trong planning, consistency analysis, code review và
trước deployment. Complexity vi phạm nguyên tắc cần ADR; nếu chính nguyên tắc thay
đổi thì cần Constitution amendment trước.

**Version**: 1.0.0 | **Ratified**: 2026-08-27 | **Last Amended**: 2026-08-27
