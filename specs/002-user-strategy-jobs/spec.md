# Feature Specification: User-owned Strategies and Durable Jobs

**Feature Branch**: `db-setup-v2`

**Created**: 2026-08-28

**Status**: Draft

**Input**: User description: "Cập nhật database cho Job, User và Strategy riêng của từng user."

## User Scenarios & Testing

### User Story 1 - Quản lý Strategy riêng tư (Priority: P1)

Là một user đã đăng nhập, tôi muốn lưu Strategy của mình với tên, cấu hình và nhiều phiên bản để có thể sửa đổi Strategy cho các lần chạy sau mà không làm thay đổi lịch sử Experiment cũ.

**Why this priority**: Đây là yêu cầu phân quyền cốt lõi: mỗi user phải chỉ quản lý Strategy thuộc sở hữu của mình trong khi vẫn tái sử dụng được catalog Strategy plugin chung của hệ thống.

**Independent Test**: Tạo hai user, cho mỗi user tạo Strategy có cùng tên và cấu hình, sau đó xác nhận mỗi user chỉ có thể xem, sửa hoặc archive Strategy của chính mình.

**Acceptance Scenarios**:

1. **Given** user A và user B đã đăng nhập, **When** cả hai lưu Strategy cùng tên, **Then** hệ thống tạo hai Strategy độc lập với owner khác nhau.
2. **Given** một Strategy thuộc user A, **When** user B dùng ID đó để đọc, sửa, archive hoặc chạy Experiment, **Then** yêu cầu bị từ chối mà không làm lộ nội dung Strategy.
3. **Given** user A thay đổi tham số Strategy, **When** user lưu thay đổi, **Then** một version mới được tạo và version cũ vẫn giữ nguyên.
4. **Given** một Strategy version đã được Experiment tham chiếu, **When** user archive Strategy, **Then** bằng chứng tái lập của Experiment vẫn còn đọc được.

---

### User Story 2 - Theo dõi Job và các lần thực thi (Priority: P2)

Là user khởi chạy Search hoặc Backtest, tôi muốn mỗi công việc có một Job ID, trạng thái và tiến trình bền vững để có thể theo dõi, dừng và hiểu các lần retry mà không nhận kết quả trùng.

**Why this priority**: Job là identity nghiệp vụ của tác vụ dài; nếu chỉ có Execution Attempt thì user không phân biệt được một công việc với các lần Worker thử chạy lại công việc đó.

**Independent Test**: Tạo một Experiment và Job, ghi nhiều Execution Attempt cho cùng Job, sau đó xác nhận Job vẫn là một công việc duy nhất và chỉ một kết quả nghiệp vụ được chấp nhận.

**Acceptance Scenarios**:

1. **Given** user bắt đầu Search hoặc Backtest hợp lệ, **When** yêu cầu được chấp nhận, **Then** hệ thống lưu một Job thuộc Experiment của user và trả Job ID để theo dõi.
2. **Given** Worker retry một Job thất bại tạm thời, **When** lần chạy mới bắt đầu, **Then** hệ thống tạo Execution Attempt mới dưới cùng Job thay vì tạo Job mới.
3. **Given** một Job đã hoàn thành, **When** message cũ được giao lại, **Then** hệ thống không tạo thêm kết quả nghiệp vụ trùng.
4. **Given** user A gửi Job ID thuộc Experiment của user B, **When** user A yêu cầu xem hoặc dừng Job, **Then** yêu cầu bị từ chối.

---

### User Story 3 - Giữ hồ sơ User tách khỏi thông tin đăng nhập (Priority: P3)

Là người vận hành hệ thống, tôi muốn hồ sơ ứng dụng liên kết đúng với tài khoản xác thực mà không lưu lại password hoặc refresh token trong business schema để giảm rủi ro bảo mật và tránh hai nguồn dữ liệu đăng nhập.

**Why this priority**: User ownership cần identity ổn định, nhưng thông tin xác thực phải do hệ thống authentication chuyên trách quản lý.

**Independent Test**: Tạo tài khoản xác thực và hồ sơ liên kết, sau đó xác nhận business schema chỉ lưu user ID cùng dữ liệu hồ sơ, không có password hay refresh token.

**Acceptance Scenarios**:

1. **Given** một tài khoản xác thực hợp lệ, **When** hồ sơ ứng dụng được tạo, **Then** hồ sơ tham chiếu đúng identity của tài khoản đó.
2. **Given** schema business được kiểm tra, **When** liệt kê cột của bảng user profile, **Then** không có password, password hash hoặc refresh token.

### Edge Cases

- Hai user được phép đặt Strategy trùng tên; cùng một user không được có hai Strategy đang hoạt động trùng tên không phân biệt chữ hoa/thường.
- Strategy đã archive không nhận version mới nhưng version cũ vẫn có thể phục vụ reproduction của Experiment.
- Strategy version đơn chỉ tham chiếu một plugin version; Strategy version kết hợp phải có ít nhất một component hợp lệ trước khi được sử dụng.
- Job có thể chưa gắn Candidate đối với Search tổng; Backtest Job gắn Candidate phải thuộc cùng Experiment.
- Retry tạo Attempt mới với số thứ tự tăng dần; cùng một Job không được có hai Attempt cùng số.
- Xóa cache hoặc queue không được làm mất Job, Strategy, version hay quan hệ sở hữu đã lưu bền vững.
- Bản ghi Attempt cũ đã tồn tại trước migration phải được liên kết với Job hợp lệ thay vì trở thành orphan.

## Requirements

### Functional Requirements

- **FR-001**: Hệ thống MUST tiếp tục dùng identity hiện có làm nguồn xác thực duy nhất và MUST NOT lưu password, password hash hoặc refresh token trong business schema.
- **FR-002**: Mỗi Strategy do user lưu MUST có đúng một owner và mặc định là riêng tư trong MVP.
- **FR-003**: Hệ thống MUST giữ catalog Strategy plugin dùng chung tách biệt với cấu hình Strategy do user sở hữu.
- **FR-004**: User MUST có thể tạo, xem, đổi tên và archive Strategy của mình; archive MUST NOT xóa các version đã được Experiment tham chiếu.
- **FR-005**: Mỗi thay đổi cấu hình Strategy MUST tạo một version bất biến mới với số version tăng trong phạm vi Strategy đó.
- **FR-006**: Một user-owned Strategy version MUST biểu diễn Strategy đơn hoặc Strategy kết hợp và MUST tham chiếu các plugin version hợp lệ cùng tham số đã chốt.
- **FR-007**: Mỗi Strategy version MUST có fingerprint ổn định để nhận diện chính xác nội dung cấu hình.
- **FR-008**: Experiment Manifest MAY tham chiếu user-owned Strategy version nguồn nhưng MUST tiếp tục đóng băng snapshot cần thiết để tái lập độc lập với thay đổi sau này.
- **FR-009**: Hệ thống MUST từ chối mọi thao tác đọc, sửa, archive hoặc sử dụng Strategy khi authenticated user không phải owner.
- **FR-010**: Mỗi tác vụ Search hoặc Backtest dài MUST có một Job bền vững với Job ID, loại, trạng thái, tiến trình, lỗi có cấu trúc và các mốc thời gian cần thiết.
- **FR-011**: Mỗi Job MUST thuộc đúng một Experiment; Candidate là tùy chọn đối với Search Job và nếu có MUST thuộc cùng Experiment.
- **FR-012**: Mỗi lần Worker thử xử lý MUST tạo một Execution Attempt thuộc Job, với attempt number duy nhất và tăng dần trong Job.
- **FR-013**: Retry MUST giữ nguyên Job ID; duplicate delivery MUST NOT tạo Result nghiệp vụ thứ hai cho cùng Candidate và configuration.
- **FR-014**: Quyền xem hoặc dừng Job MUST được suy ra từ owner của Experiment; client-supplied Job ID một mình MUST NOT cấp quyền.
- **FR-015**: PostgreSQL-compatible storage MUST là source of truth cho User Strategy, version, Job và Attempt; queue/cache mất dữ liệu MUST có thể phục hồi từ trạng thái bền vững.
- **FR-016**: Schema change MUST được phát hành bằng forward migration mới và MUST NOT sửa migration baseline đã áp dụng.
- **FR-017**: Direct browser roles MUST NOT được cấp quyền đọc hoặc ghi business tables mới; authorization được thực thi tại application boundary.
- **FR-018**: Migration MUST bảo toàn và liên kết mọi Execution Attempt có sẵn với một Job hợp lệ.

### Key Entities

- **User Profile**: Hồ sơ ứng dụng liên kết một-một với identity xác thực; không chứa credential hoặc token.
- **Strategy Plugin Version**: Phiên bản implementation Strategy dùng chung do hệ thống quản lý.
- **User Strategy**: Strategy riêng tư do một user sở hữu, có tên, loại và lifecycle active/archived.
- **User Strategy Version**: Snapshot cấu hình bất biến của User Strategy, có version number và fingerprint.
- **User Strategy Component**: Thành phần plugin cùng tham số trong một Strategy kết hợp.
- **Experiment Manifest**: Snapshot đầu vào tái lập; có thể ghi Strategy version nguồn nhưng không phụ thuộc dữ liệu mutable.
- **Job**: Công việc Search hoặc Backtest logic thuộc một Experiment, có trạng thái và progress bền vững.
- **Execution Attempt**: Một lần Worker thử thực thi Job; nhiều Attempt có thể thuộc một Job.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Trong bộ kiểm thử hai-user, 100% thao tác đọc, sửa, archive và sử dụng chéo Strategy bị từ chối.
- **SC-002**: Hai user có thể lưu Strategy trùng tên mà không xung đột, trong khi một user không thể có hai Strategy active trùng tên không phân biệt hoa/thường.
- **SC-003**: 100% thay đổi cấu hình tạo version mới; không kiểm thử nào có thể sửa nội dung version đã được Experiment tham chiếu.
- **SC-004**: Một Job có thể ghi ít nhất ba Attempt retry mà vẫn chỉ có một Job ID và không tạo Result nghiệp vụ trùng.
- **SC-005**: 100% Job và Strategy truy ngược được tới đúng owner thông qua quan hệ dữ liệu đã công bố.
- **SC-006**: Sau khi cache/queue bị xóa trong kịch bản phục hồi, toàn bộ Job, Strategy version và trạng thái bền vững vẫn tồn tại và có thể tái lập công việc cần chạy.
- **SC-007**: Migration nâng cấp hoàn thành mà không để Execution Attempt orphan và không thay đổi nội dung migration baseline.
- **SC-008**: Kiểm tra schema xác nhận không có credential hoặc refresh token trong business tables và direct browser roles không có quyền truy cập các bảng mới.

## Assumptions

- Supabase Auth tiếp tục quản lý password, session và refresh token; feature này không thiết kế lại authentication.
- User Strategy là cấu hình của các plugin Strategy đã đăng ký, không cho user upload hoặc thực thi mã tùy ý.
- MVP chỉ hỗ trợ Strategy riêng tư; chia sẻ Strategy, team, role editor/viewer và marketplace nằm ngoài phạm vi.
- Catalog plugin Strategy hiện tại vẫn là dữ liệu dùng chung của hệ thống.
- Job trong phạm vi feature này gồm Search và Backtest; News/Sentiment giữ lifecycle riêng.
- Application layer sẽ kiểm tra ownership; database duy trì integrity và không cấp direct access cho browser roles.
- Thao tác xóa Strategy ở MVP được biểu diễn bằng archive để bảo toàn provenance.
