# Feature Specification: Java Backend Foundation

**Feature Branch**: `feature/002-java-backend-foundation`

**Created**: 2026-08-27

**Status**: Planned — ready for implementation; ADR merge gate open

**Input**: User description: "Thiết lập nền tảng backend dùng chung theo roadmap và
ADR hiện có để nhóm bốn người có thể phát triển các capability song song mà không phá
module boundary."

## Clarifications

### Session 2026-08-28

- Q: F-002 nên tạo sẵn phạm vi module nào để ba feature tiếp theo có thể bắt đầu mà
  không phải sửa lại cấu trúc build? → A: Tạo sẵn toàn bộ module trong Architecture
  dưới dạng skeleton buildable, chỉ có public boundary/package skeleton và chưa có
  business implementation.
- Q: F-002 nên chứng minh JWT authentication bằng cách nào mà chưa mở rộng business
  API? → A: Dùng protected controller fixture chỉ tồn tại trong integration test; không
  thêm public endpoint hoặc thay đổi OpenAPI trong feature này.
- Q: F-002 có cần kết nối thật tới Supabase development để kiểm tra readiness không?
  → A: Có; dùng kiểm tra kết nối read-only, không truy vấn hoặc thay đổi business table.
- Q: Trong F-002, `apps/worker` cần đạt mức nào khi chưa có job business? → A: Worker
  phải là application chạy được, có health và ở trạng thái idle; chưa kết nối queue hoặc
  consume job.
- Q: F-002 cần thiết lập observability nền tảng ở mức nào? → A: Chuẩn hóa structured
  log và correlation ID xuyên request/response; chưa triển khai tracing backend, metrics
  dashboard hoặc hạ tầng observability riêng.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Build và kiểm thử backend thống nhất (Priority: P1)

Là thành viên phát triển, tôi muốn lấy source mới, chạy một quy trình duy nhất và nhận
được kết quả build/test của toàn backend để bắt đầu làm việc mà không phải tự ghép cấu
hình cho từng module.

**Why this priority**: Mọi feature Market, Strategy, Experiment và Worker đều phụ thuộc
nền tảng build chung; thiếu nó khiến bốn thành viên tạo cấu trúc không tương thích.

**Independent Test**: Trên một máy chỉ có các prerequisite đã công bố, checkout source,
chạy command chuẩn và xác nhận toàn bộ module foundation được build/test thành công.

**Acceptance Scenarios**:

1. **Given** source checkout sạch, **When** thành viên chạy command build chuẩn,
   **Then** toàn bộ backend foundation được build và test không cần chỉnh file local.
2. **Given** một module foundation có test thất bại, **When** chạy command chuẩn,
   **Then** quy trình trả trạng thái thất bại và chỉ rõ module/test gây lỗi.
3. **Given** một thành viên mới, **When** làm theo hướng dẫn repository,
   **Then** người đó khởi động được API foundation và chạy test trong một buổi thiết lập.
4. **Given** một public boundary dùng identity, decimal hoặc timestamp, **When** kiểm tra
   canonical convention, **Then** identity dùng UUID, decimal không mất precision và
   timestamp biểu diễn một UTC instant không mơ hồ timezone.

---

### User Story 2 - Bảo vệ ranh giới module (Priority: P1)

Là nhóm phát triển, chúng tôi muốn dependency direction và public boundary được kiểm
tra tự động để mỗi người có thể thay đổi capability mình sở hữu mà không import
implementation hoặc persistence mapping của capability khác.

**Why this priority**: Đây là điều kiện để Modular Monolith có ý nghĩa thực tế và để
bốn feature tiếp theo được phát triển song song.

**Independent Test**: Thêm một dependency hợp lệ và một dependency bị cấm vào fixture;
xác nhận trường hợp hợp lệ pass và trường hợp bị cấm làm architecture test fail.

**Acceptance Scenarios**:

1. **Given** module chỉ dùng public contract được phép, **When** chạy architecture test,
   **Then** test thành công.
2. **Given** capability import implementation, repository hoặc table mapping nội bộ của
   module khác, **When** chạy architecture test, **Then** test thất bại với rule rõ ràng.
3. **Given** domain/Strategy code phụ thuộc framework, provider hoặc persistence,
   **When** chạy architecture test, **Then** dependency bị từ chối.
4. **Given** hai capability tạo dependency vòng, **When** chạy architecture test,
   **Then** cycle fixture làm test thất bại với các module liên quan được chỉ rõ.

---

### User Story 3 - Khởi động và quan sát API foundation (Priority: P1)

Là thành viên tích hợp, tôi muốn khởi động API với cấu hình môi trường an toàn và kiểm
tra trạng thái sống/sẵn sàng để biết application foundation hoạt động trước khi thêm
business endpoint.

**Why this priority**: Capability owner cần một composition root ổn định để tích hợp;
người vận hành cần phân biệt process đang sống với dependency chưa sẵn sàng.

**Independent Test**: Khởi động API bằng development configuration, gọi các endpoint
trạng thái và mô phỏng thiếu cấu hình bắt buộc để xác nhận phản hồi/failure rõ ràng.

**Acceptance Scenarios**:

1. **Given** cấu hình hợp lệ, **When** API khởi động, **Then** trạng thái sống và sẵn
   sàng được báo thành công mà không làm thay đổi business data.
2. **Given** một dependency bắt buộc không sẵn sàng, **When** kiểm tra readiness,
   **Then** trạng thái degraded/not-ready được báo mà không làm process crash âm thầm.
3. **Given** API hoặc Worker thiếu cấu hình bắt buộc, **When** khởi động, **Then** runtime
   tương ứng fail-fast với thông báo tên cấu hình thiếu nhưng không hiển thị giá trị secret.
4. **Given** cấu hình shared development hợp lệ, **When** chạy integration verification,
   **Then** readiness của cả API và Worker xác nhận kết nối bằng thao tác chỉ đọc và không
   thay đổi business data.
5. **Given** chưa có queue và job handler, **When** Worker foundation khởi động,
   **Then** Worker báo health thành công, ở trạng thái idle và không thử consume job.
6. **Given** validation failure hoặc unexpected failure trong test fixture, **When** API
   mapping lỗi, **Then** response dùng safe error envelope, có correlation ID và không lộ
   stack trace hoặc secret.

---

### User Story 4 - Xác thực identity tại application boundary (Priority: P2)

Là capability owner, tôi muốn API foundation xác thực access token và cung cấp một
authenticated user identity chuẩn hóa để feature Experiment sau kiểm tra ownership mà
không tự triển khai authentication lại.

**Why this priority**: Ownership authorization phụ thuộc identity tin cậy, nhưng business
endpoint và authorization policy cụ thể chưa thuộc foundation.

**Independent Test**: Gửi request vào protected controller fixture chỉ tồn tại trong
integration test với trường hợp không token, token sai, token hết hạn và token hợp lệ;
xác nhận chỉ token hợp lệ tạo được authenticated user context.

**Acceptance Scenarios**:

1. **Given** request không có token tới protected fixture endpoint, **When** xử lý,
   **Then** request bị từ chối theo error contract và không chạy handler được bảo vệ.
2. **Given** token sai chữ ký, sai issuer/audience hoặc hết hạn, **When** xử lý,
   **Then** request bị từ chối mà không lộ chi tiết xác thực nhạy cảm.
3. **Given** token hợp lệ, **When** xử lý, **Then** boundary cung cấp đúng user ID chuẩn
   hóa cho application layer.

### Edge Cases

- Build được chạy khi máy không có dependency service đang hoạt động.
- Hai module vô tình dùng cùng package namespace hoặc tạo dependency vòng.
- Cấu hình development chứa placeholder nhưng bị hiểu nhầm là credential thật.
- API nhận malformed Authorization header, token thiếu claim bắt buộc hoặc clock skew nhỏ.
- Dependency trạng thái chậm/timeout trong khi liveness vẫn phải phản hồi.
- Worker foundation được khởi động dù chưa có job handler business nào.
- Một module mới được thêm nhưng không tự động tham gia build/architecture test.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Repository MUST cung cấp một command chuẩn để build và chạy test toàn bộ
  backend foundation từ source checkout sạch.
- **FR-002**: Foundation MUST có các composition boundary cho public API và background
  Worker, nhưng MUST NOT triển khai business flow của Market, Strategy, Experiment,
  Backtest, Evaluation, News hoặc Sentiment trong feature này.
- **FR-003**: Foundation MUST khai báo skeleton buildable cho `domain`, `contracts`,
  `market-data`, `strategy-core`, `strategies`, `combination`, `backtesting`,
  `evaluation`, `experiment`, `search`, `leaderboard`, `news` và `persistence` để
  feature sau bổ sung implementation mà không đổi layout gốc. Skeleton chỉ chứa public
  boundary/package cần để chứng minh build và dependency rule, chưa chứa business logic.
- **FR-004**: Capability module MUST chỉ công bố contract/application boundary cần thiết;
  implementation, repository và persistence mapping nội bộ MUST không trở thành shared API.
- **FR-005**: Hệ thống MUST có automated architecture rules cho dependency direction
  của ADR-0001 và ADR-0002, bao gồm test chứng minh rule có thể bắt dependency bị cấm.
- **FR-006**: Domain và Strategy boundary MUST không phụ thuộc application framework,
  provider client, transport model hoặc persistence implementation.
- **FR-007**: Foundation MUST cung cấp canonical representation/convention cho public
  identity dưới dạng UUID, exact decimal không dùng binary floating-point và UTC instant
  không mơ hồ timezone mà không tạo business entity không có owner.
- **FR-008**: API foundation MUST có error envelope/correlation identity nhất quán cho
  validation, authentication và unexpected failure; public error MUST không lộ secret
  hoặc internal stack trace.
- **FR-009**: API foundation MUST cung cấp liveness và readiness riêng biệt; health check
  MUST không ghi hoặc sửa business data.
- **FR-010**: Cấu hình MUST lấy từ environment/runtime configuration; repository MUST
  chỉ chứa example/placeholder không bí mật và MUST fail-fast khi thiếu giá trị bắt buộc.
- **FR-011**: API foundation MUST xác thực Bearer access token theo identity authority
  đã chốt và tạo authenticated user context chứa canonical user ID. Verification MUST
  dùng test-only controller fixture; feature MUST NOT thêm public `/me` hoặc business
  endpoint chỉ để kiểm tra authentication.
- **FR-012**: Authentication foundation MUST từ chối token thiếu, malformed, hết hạn,
  sai chữ ký hoặc sai issuer/audience và MUST có automated evidence cho mỗi nhóm lỗi.
- **FR-013**: Browser/client role MUST không nhận privileged database credential hoặc
  đường truy cập trực tiếp business table từ foundation.
- **FR-014**: Worker foundation MUST tái sử dụng public module API/contract và MUST không
  sao chép business logic từ API composition boundary. Worker MUST khởi động được, cung
  cấp health state và ở trạng thái idle; feature MUST NOT kết nối queue hoặc consume job.
- **FR-015**: Foundation MUST có hướng dẫn prerequisite, build, test, run, configuration
  và cách thêm module mới mà không phá dependency rule.
- **FR-016**: Mỗi acceptance scenario và quality scenario bị ảnh hưởng MUST có evidence
  tương xứng, được tự động hóa khi khả thi; review evidence có thể xem lại được chấp nhận
  cho outcome cần đánh giá trực tiếp của thành viên. Test không cần database/queue/provider
  thật MUST chạy được khi các dependency đó tắt.
- **FR-017**: Foundation MUST NOT sửa migration database đã apply hoặc thay đổi business
  schema; mọi nhu cầu schema mới phát hiện trong feature trở thành forward migration
  được review riêng.
- **FR-018**: Development integration verification MUST kết nối thật tới Supabase bằng
  một database health operation chỉ đọc; MUST không query, insert, update hoặc delete
  business table và MUST không ghi connection credential vào source/log/evidence.
- **FR-019**: API và Worker foundation MUST dùng structured log convention; API MUST
  nhận hoặc tạo correlation ID, trả identity đó trong response và giữ nó trong mọi log
  của request. Feature MUST NOT triển khai tracing backend, metrics dashboard hoặc hạ
  tầng observability riêng.

### Key Entities

- **Authenticated User Context**: Identity đã được boundary xác thực, tối thiểu chứa
  canonical user ID và metadata cần thiết để application layer ra quyết định sau này.
- **Error Envelope**: Public failure representation có error code, message an toàn và
  correlation identity.
- **Health State**: Trạng thái sống và sẵn sàng của application/dependency, không chứa
  credential hoặc business data.
- **Module Boundary**: Public contract và dependency rule của một capability; không phải
  business entity hoặc nơi chứa model dùng chung tùy ý.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Một thành viên mới có thể build và chạy toàn bộ test foundation bằng một
  command trong tối đa 10 phút sau khi cài prerequisite đã công bố.
- **SC-002**: 100% module foundation tham gia command build/test chung; không module nào
  cần chạy quy trình thủ công riêng để được kiểm chứng.
- **SC-003**: Architecture test bắt được 100% fixture internal import, forbidden technology
  dependency và dependency cycle đã liệt kê, đồng thời chấp nhận 100% fixture dependency
  hợp lệ.
- **SC-004**: API và Worker foundation đều khởi động và trả liveness trong tối đa 30 giây
  trên máy development mục tiêu khi cấu hình hợp lệ.
- **SC-005**: 100% fixture token thiếu, malformed, hết hạn, sai signature/issuer/audience
  bị từ chối; 100% fixture hợp lệ tạo đúng canonical user ID.
- **SC-006**: Không secret, privileged database credential hoặc internal stack trace xuất
  hiện trong source control, public error fixture hoặc client-facing configuration.
- **SC-007**: Toàn bộ automated test không cần external integration vẫn pass khi database,
  queue và provider bị tắt.
- **SC-008**: Nghi Văn, Văn Minh và Tiến có thể tạo feature module từ cùng foundation mà
  không phải sửa composition/build layout ngoài extension point đã công bố.
- **SC-009**: Shared-development integration verification xác nhận API và Worker kết nối
  thành công; reviewable operation/audit evidence không ghi nhận business-data query hoặc
  mutation từ health operation.
- **SC-010**: 100% request fixture có đúng một correlation ID trong response và mọi log
  fixture của request có cùng identity; log verification không chứa token hoặc secret.

## Assumptions

- Constitution v1.1.0 chốt target stack và cho phép ADR `Proposed` hỗ trợ planning.
  ADR-0011 đã `Accepted`; ADR-0001, ADR-0002, ADR-0006 và ADR-0007 phải được review và
  chuyển sang `Accepted` trước khi implementation phụ thuộc được merge.
- Luật là owner chính của F-002; Nghi Văn, Văn Minh và Tiến review boundary phục vụ
  Market, Strategy và Experiment trước khi merge.
- Database baseline đã apply trên shared development; feature chỉ cần configuration và
  readiness boundary, không tạo hoặc sửa business schema.
- Redis Streams, Binance integration, Strategy implementation, Experiment persistence,
  Backtest, News/Sentiment và user-facing business endpoints thuộc feature sau.
- Worker trong feature này chỉ là composition/build foundation, chưa consume job thật.
- Frontend và high-fidelity prototype không thuộc scope; browser authentication UI được
  triển khai trong Web feature sau.
