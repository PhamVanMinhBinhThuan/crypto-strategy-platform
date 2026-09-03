<!--
Báo cáo tác động đồng bộ
- Thay đổi phiên bản: 1.0.0 → 1.1.0
- Nguyên tắc sửa đổi:
  - I. Quản trị theo ADR → I. Đặc tả trước, ADR khi có quyết định kiến trúc
  - V. Phân phối an toàn, tin cậy và kiểm chứng được →
    V. An toàn, tin cậy, quan sát và kiểm chứng được
- Phần được mở rộng: target stack Next.js; phạm vi Spec Kit; evidence governance;
  quy tắc phê duyệt amendment
- Phần bị loại bỏ: không có
- Nội dung trì hoãn: đồng bộ architecture view đang ghi `React/Next.js` thành `Next.js`
-->

# Constitution của Crypto Strategy Lab

## Các nguyên tắc cốt lõi

### I. Đặc tả trước, ADR khi có quyết định kiến trúc

Mọi feature MUST bắt đầu từ business outcome, acceptance criteria và quality scenario
liên quan trong specification. Plan MUST kiểm tra các ADR hiện có và xác định
quyết định nào cần được ghi mới hoặc thay thế. ADR MUST được tạo khi
quyết định có tác động dài hạn, xuyên module, thay đổi contract, deployment,
data ownership hoặc mang trade-off kiến trúc đáng kể.

ADR `Proposed` hỗ trợ thảo luận và planning nhưng chưa có hiệu lực ràng buộc.
ADR liên quan MUST chuyển sang `Accepted` trước khi implementation phụ thuộc được
merge. Artifact phụ thuộc MUST tuân theo ADR `Accepted`. Nếu evidence cho thấy quyết
định không còn phù hợp, nhóm MUST tạo amendment hoặc ADR thay thế thay vì
sửa lịch sử âm thầm.

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

### V. An toàn, tin cậy, quan sát và kiểm chứng được

Business data MUST được truy cập qua application boundary đã cấp quyền. Secret và
privileged credential MUST NOT xuất hiện trong browser bundle, source control, log
hoặc public error response. Thao tác trên dữ liệu có owner MUST xác thực identity
và kiểm tra ownership; identifier do client gửi một mình MUST NOT cấp quyền.

Durable business state MUST tồn tại khi cache hoặc queue bị mất. Luồng bất đồng bộ
MUST có idempotency, durable publication state, bounded retry và duplicate-safe
consumption. Tác vụ dài MUST công bố status, progress và lỗi có cấu trúc. Luồng
xuyên boundary MUST truy vết được bằng correlation identifier phù hợp, bao gồm
`correlationId`, `experimentId`, `candidateId` và `jobId` khi các identity này tồn tại.

Mỗi acceptance scenario và quality scenario bị ảnh hưởng MUST có evidence tương
xứng. Evidence MUST được tự động hóa khi khả thi và có thể là unit test,
contract test, integration test, architecture test, resilience test, benchmark hoặc
review có thể xem lại. Evidence MUST gắn với commit, môi trường và cấu hình
kiểm chứng. Trạng thái MUST giữ là `Planned` cho tới khi có kết quả thật có thể
xem lại; chỉ khi đó mới được chuyển sang `Verified`. Benchmark, log và kết quả
demo MUST NOT được tạo giả.

## Ràng buộc kiến trúc và dữ liệu

- Java backend giữ Modular Monolith trừ khi ADR mới Supersedes ADR-0001.
- Target stack của MVP gồm Next.js cho `apps/web`; Java 21 và Spring Boot 3 cho
  `apps/api`/`apps/worker`; Python và FastAPI cho `apps/sentiment`; PostgreSQL/Supabase
  cho durable storage; Redis Streams/Redis cho queue và cache. `apps/web` MUST dùng
  Next.js làm application framework và MUST NOT tạo thêm một React SPA độc lập khi
  chưa có architectural driver và ADR.
- Feature tạo hoặc sửa browser UI trong `apps/web` MUST tham khảo shared UI reference
  dưới `docs/ui/`. Tối thiểu workflow MUST đọc `docs/ui/README.md`,
  `docs/ui/spec-kit-reference.md`, `docs/ui/screen-map.md`,
  `docs/ui/design-system.md` và `docs/ui/interaction-states.md`. Nếu tồn tại mapping
  cho feature hiện tại, workflow MUST đọc `docs/ui/features/<FEATURE-ID>.md` và
  SHOULD kiểm tra screenshot hoặc prototype liên quan trong `docs/ui/screens/`
  và `docs/ui/prototype/`.
- Shared UI reference chỉ định presentation, visual hierarchy và interaction intent;
  nó MUST NOT trở thành source of truth cho business behavior. Thứ tự authority là:
  Constitution → ADR `Accepted` → released public contract → F-011 Frontend Foundation
  → specification/planning artifact của feature → shared UI reference dưới `docs/ui/`.
  Khi UI reference mâu thuẫn với public contract, public contract MUST thắng.
- Source dưới `docs/ui/prototype/` là read-only design evidence và MUST NOT được xem
  là production architecture. `apps/web` MUST NOT sao chép prototype-only business
  simulation, Search orchestration, Backtest/Evaluation/Ranking calculation,
  authentication/session infrastructure, HTTP client, WebSocket client hoặc
  application shell. Frontend feature MUST tái sử dụng các boundary đã công bố bởi
  F-011.
- Thay đổi công nghệ ảnh hưởng kiến trúc MUST có driver, trade-off, ADR và
  verification plan. Kafka, Kubernetes, microservice theo từng module, full CQRS hoặc
  Event Sourcing MUST NOT được thêm vào MVP nếu chưa có quy trình này.
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
- MVP MUST NOT đặt lệnh giao dịch tiền thật, quản lý ví hoặc tuyên bố kết quả
  Backtest là cam kết lợi nhuận hay lời khuyên tài chính.
- Migration đã áp dụng trên môi trường dùng chung MUST NOT bị sửa. Schema change
  MUST dùng forward migration đã review và lưu trong repository.

## Quy trình phát triển và quality gate

1. Feature mới hoặc thay đổi contract, schema hay kiến trúc MUST bắt đầu bằng
   specification có business outcome và acceptance criteria kiểm chứng được.
2. Các thay đổi trên MUST đi theo Spec Kit: specify, clarify khi cần, plan, tasks,
   consistency analysis, implementation và verification.
3. Bug nhỏ, typo hoặc cập nhật tài liệu đơn giản MAY bỏ qua full workflow nếu
   Pull Request ghi rõ phạm vi và cách kiểm tra. Feature lớn MUST NOT bị chia nhỏ
   nhằm né specification.
4. Specification mô tả user/business outcome; plan chọn implementation và xác định
   ADR liên quan; tasks mô tả công việc theo dependency order và có thể kiểm
   chứng độc lập.
5. Trước implementation, reviewer MUST xác nhận ADR, architecture, API/event
   contract, data model và feature artifact không mâu thuẫn.
6. Implementation MUST bảo toàn thay đổi của user và MUST có test ánh xạ tới
   acceptance scenario, quality attribute và failure/recovery path.
7. Database change MUST qua static review và non-production dry run trước shared
   deployment. Áp dụng migration remote cần phê duyệt rõ ràng.
8. Pull Request MUST nêu owner, ADR, contract, migration, security boundary và
   evidence bị ảnh hưởng. Vi phạm Constitution chưa giải quyết sẽ chặn merge.

## Quản trị

Constitution này quản lý Spec Kit artifact và implementation. ADR đang có hiệu lực
cung cấp quyết định chi tiết bên dưới. Nếu ADR và Constitution mâu thuẫn, công việc
MUST dừng tới khi được giải quyết bằng Constitution amendment hoặc ADR đúng quy
trình; implementation MUST NOT tự chọn âm thầm.

Amendment MUST được thực hiện qua Pull Request và nêu proposal, lý do, impact
review trên template/spec/plan/task/ADR cùng migration plan khi artifact hiện có bị ảnh
hưởng. Thay đổi MAJOR hoặc MINOR MUST được ít nhất hai thành viên khác review;
thay đổi PATCH MUST được ít nhất một thành viên khác review. Tác giả
MUST NOT tự phê duyệt amendment của mình.

Version tuân theo Semantic Versioning: MAJOR khi loại bỏ hoặc định nghĩa lại nguyên
tắc không tương thích, MINOR khi thêm nguyên tắc hoặc mở rộng governance đáng kể,
PATCH khi làm rõ không đổi ngữ nghĩa. Mỗi amendment MUST cập nhật báo cáo
tác động, version và ngày sửa.

Compliance MUST được review trong planning, consistency analysis, code review và
trước deployment. Complexity vi phạm nguyên tắc cần ADR; nếu chính nguyên tắc thay
đổi thì cần Constitution amendment trước.

**Version**: 1.1.0 | **Ratified**: 2026-08-27 | **Last Amended**: 2026-08-28
