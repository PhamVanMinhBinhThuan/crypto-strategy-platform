# Feature Specification: Web Foundation and Authentication

**Feature Branch**: `feature/011-web-foundation-auth`

**Created**: 2026-09-03

**Status**: Draft

**Input**: User description: "Xây nền tảng Web dùng chung cho F-012/F-013, gồm giao diện đẹp và responsive, đăng ký, đăng nhập, quên/đặt lại mật khẩu, đăng xuất, session, protected routes, API/WebSocket client và mock adapter; chưa triển khai màn hình nghiệp vụ."

## Clarifications

### Session 2026-09-03

- Q: Sau khi đăng ký, người dùng có bắt buộc xác minh email trước khi được đăng nhập không? → A: Bắt buộc xác minh email; MVP dùng email mặc định của Supabase và chưa cấu hình SMTP riêng.
- Q: Form đăng ký chỉ cần email và mật khẩu, hay cần thêm tên hiển thị của người dùng? → A: Chỉ email, mật khẩu và xác nhận mật khẩu; không tạo User Profile.
- Q: Sau khi đặt lại mật khẩu thành công, người dùng nên được đăng nhập tự động hay quay về trang Login? → A: Kết thúc recovery session và quay về Login để đăng nhập bằng mật khẩu mới.
- Q: F-011 chỉ dùng giao diện tối giống các sketch hiện có, hay phải hỗ trợ cả giao diện sáng? → A: MVP chỉ dùng dark theme và không có theme switcher.
- Q: Nội dung giao diện của F-011 nên sử dụng ngôn ngữ nào? → A: Chỉ dùng tiếng Anh; chưa triển khai đa ngôn ngữ trong MVP.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Truy cập tài khoản an toàn (Priority: P1)

Là người dùng mới hoặc đã có tài khoản, tôi muốn đăng ký, đăng nhập và khôi phục mật khẩu
qua giao diện rõ ràng để có thể truy cập Crypto Strategy Lab mà không cần hiểu cơ chế xác thực.

**Why this priority**: Mọi dữ liệu Strategy, Experiment và Result riêng đều yêu cầu danh tính
đã xác thực; không có luồng này thì các màn hình tiếp theo không thể sử dụng an toàn.

**Independent Test**: Dùng một trình duyệt chưa có session để hoàn thành đăng ký, xác minh email
nếu được yêu cầu, đăng nhập và yêu cầu đặt lại mật khẩu; xác nhận mỗi bước có trạng thái và thông
báo phù hợp mà không lộ thông tin nhạy cảm.

**Acceptance Scenarios**:

1. **Given** người dùng chưa có tài khoản, **When** gửi email và mật khẩu hợp lệ,
   **Then** hệ thống tạo yêu cầu đăng ký và hướng dẫn bước tiếp theo mà không đăng nhập sai danh tính.
2. **Given** tài khoản hợp lệ, **When** đăng nhập đúng, **Then** người dùng được đưa tới trang mặc
   định đã bảo vệ và session được nhận diện trên lần tải trang tiếp theo.
3. **Given** thông tin đăng nhập sai, **When** đăng nhập, **Then** giao diện trả thông báo an toàn,
   không tiết lộ email có tồn tại hay không và cho phép thử lại.
4. **Given** người dùng quên mật khẩu, **When** gửi email khôi phục, **Then** giao diện luôn trả phản
   hồi trung tính và cho phép hoàn tất đặt lại mật khẩu bằng luồng hợp lệ.

---

### User Story 2 - Session và đăng xuất nhất quán (Priority: P1)

Là người dùng đã đăng nhập, tôi muốn session được duy trì và làm mới an toàn; khi đăng xuất hoặc
session không còn hợp lệ, tôi không muốn dữ liệu riêng tiếp tục hiển thị.

**Why this priority**: Session sai hoặc dữ liệu còn sót sau logout là rủi ro bảo mật trực tiếp.

**Independent Test**: Đăng nhập, tải lại trang, mở một trang được bảo vệ, làm session hết hạn và
đăng xuất; xác nhận session hợp lệ được phục hồi, session không thể làm mới chuyển về Login và
dữ liệu riêng bị xóa khỏi trạng thái hiển thị.

**Acceptance Scenarios**:

1. **Given** session còn hợp lệ, **When** tải lại hoặc mở trực tiếp một route được bảo vệ,
   **Then** người dùng vẫn ở đúng route mà không phải đăng nhập lại.
2. **Given** session sắp hoặc đã hết hạn nhưng còn có thể làm mới, **When** người dùng tiếp tục,
   **Then** session được làm mới mà không yêu cầu nhập lại mật khẩu.
3. **Given** session không thể làm mới, **When** truy cập nội dung được bảo vệ,
   **Then** người dùng được chuyển về Login và đích ban đầu được giữ để quay lại sau đăng nhập.
4. **Given** người dùng chọn Logout, **When** thao tác hoàn tất, **Then** session và dữ liệu riêng
   trong client bị xóa và nút Back không làm nội dung riêng xuất hiện lại.

---

### User Story 3 - Application shell đẹp và dễ sử dụng (Priority: P2)

Là người dùng, tôi muốn một giao diện nhất quán trên desktop và mobile, có điều hướng rõ ràng cùng
trạng thái loading, empty, error và degraded dễ hiểu.

**Why this priority**: Foundation trực quan và nhất quán giúp F-012/F-013 không tạo ra các màn hình
rời rạc hoặc hành vi phản hồi khác nhau.

**Independent Test**: Kiểm tra Login và application shell ở chiều rộng mobile/desktop, sử dụng
chuột và bàn phím, rồi kích hoạt từng trạng thái dùng chung để xác nhận nội dung vẫn đọc và thao tác được.

**Acceptance Scenarios**:

1. **Given** người dùng đã đăng nhập, **When** mở application shell, **Then** thương hiệu, navigation,
   trạng thái tài khoản và Logout hiển thị nhất quán với các sketch đã duyệt.
2. **Given** màn hình nhỏ, **When** mở navigation, **Then** nội dung không tràn ngang và các thao tác
   chính vẫn truy cập được.
3. **Given** một tác vụ đang tải, rỗng, lỗi hoặc degraded, **When** màn hình dùng trạng thái nền tảng,
   **Then** người dùng hiểu tình trạng và thấy hành động tiếp theo phù hợp.

---

### User Story 4 - Nền tảng dùng chung cho các nhóm UI (Priority: P2)

Là thành viên thực hiện F-012 hoặc F-013, tôi muốn dùng chung contract về route, session,
Backend request, realtime connection và trạng thái UI để phát triển song song mà không tạo nền
tảng cạnh tranh hoặc gắn component vào dữ liệu giả.

**Why this priority**: Đây là điều kiện để hai feature UI tiếp theo làm song song với ít conflict.

**Independent Test**: Tạo một route mẫu bằng adapter dữ liệu giả, sau đó thay bằng adapter public
Backend tương thích; xác nhận route, component và trạng thái người dùng không phải đổi contract.

**Acceptance Scenarios**:

1. **Given** foundation đã công bố, **When** F-012 và F-013 thêm route riêng,
   **Then** cả hai tái sử dụng cùng application shell, session và transport boundary.
2. **Given** Backend capability chưa sẵn sàng, **When** nhóm phát triển bằng fixture,
   **Then** fixture được nhận diện rõ là dữ liệu giả và không đi vào production mặc định.
3. **Given** fixture được thay bằng public Backend, **When** tích hợp,
   **Then** component không truy cập trực tiếp business table hoặc service nội bộ.

### Edge Cases

- Email sai định dạng, mật khẩu không đạt policy, hai mật khẩu đặt lại không khớp hoặc form bị gửi lặp.
- Email đăng ký đã tồn tại hoặc email khôi phục không tồn tại nhưng phản hồi không được tiết lộ account existence.
- Link xác minh/đặt lại mật khẩu hết hạn, đã dùng, bị sửa hoặc mở trên trình duyệt khác.
- Người dùng mở route được bảo vệ trước khi client xác định xong session.
- Nhiều tab cùng logout, refresh session hoặc thay đổi trạng thái authentication.
- Backend, identity provider hoặc mạng tạm lỗi; không được hiển thị stack trace, token hay credential.
- WebSocket ticket hết hạn, connection mất hoặc JWT hết hạn trong khi realtime đang hoạt động.
- Redirect đích chứa URL ngoài hệ thống hoặc vòng lặp Login-to-Login.
- Fixture vô tình được chọn ở môi trường production.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST cung cấp các luồng đăng ký, đăng nhập, quên mật khẩu, đặt lại mật khẩu
  và đăng xuất bằng email/password.
- **FR-002**: Phản hồi đăng nhập/khôi phục MUST không tiết lộ tài khoản hoặc email có tồn tại hay không.
- **FR-003**: Mật khẩu MUST tuân thủ policy của identity provider và MUST không được lưu trong
  application state lâu hơn thời gian cần để gửi form.
- **FR-003a**: Form đăng ký MUST chỉ thu thập email, mật khẩu và xác nhận mật khẩu; F-011 MUST
  không tạo User Profile hoặc yêu cầu thay đổi database để lưu tên người dùng.
- **FR-004**: Luồng đăng ký MUST yêu cầu xác minh email trước khi đăng nhập và MUST hiển thị rõ
  trạng thái chờ xác minh, xác minh thành công, link hết hạn hoặc link không hợp lệ.
- **FR-005**: Link xác minh và đặt lại mật khẩu MUST được kiểm tra trước khi cho phép thay đổi session
  hoặc credential và MUST có trạng thái rõ cho link hết hạn/không hợp lệ.
- **FR-005a**: Sau khi đặt lại mật khẩu thành công, hệ thống MUST kết thúc recovery session và
  chuyển người dùng về Login để đăng nhập bằng mật khẩu mới; MUST NOT tự động mở session nghiệp vụ.
- **FR-006**: Mọi route nghiệp vụ MUST yêu cầu session hợp lệ; route authentication MUST truy cập
  được khi chưa đăng nhập.
- **FR-007**: Điều hướng từ route được bảo vệ tới Login MUST chỉ giữ redirect nội bộ an toàn.
- **FR-008**: Session hợp lệ MUST tồn tại qua reload; hệ thống MUST thử refresh session trước khi
  yêu cầu đăng nhập lại.
- **FR-009**: Logout MUST xóa session, dữ liệu riêng và realtime subscription phía client trước khi
  chuyển về Login.
- **FR-010**: Public Backend request MUST gửi access token hiện tại theo contract F-009 và MUST
  không đưa refresh token vào request nghiệp vụ.
- **FR-011**: Realtime connection MUST xin one-time ticket bằng session hiện tại, reconnect/resubscribe
  theo contract F-009 và MUST không đặt access/refresh token dài hạn trong WebSocket URL/message.
- **FR-012**: Browser MUST không đọc/ghi business table trực tiếp và MUST không gọi trực tiếp Binance
  hoặc Sentiment Service nội bộ.
- **FR-013**: Foundation MUST công bố một application shell dùng chung gồm navigation, account menu
  và vùng nội dung cho các route F-012/F-013.
- **FR-014**: Foundation MUST cung cấp trạng thái dùng chung cho loading, empty, validation error,
  authentication error, unavailable và degraded.
- **FR-015**: Login, Register, Forgot Password, Reset Password và application shell MUST sử dụng
  được trên desktop/mobile và bằng bàn phím.
- **FR-015a**: F-011 MUST dùng một dark theme nhất quán với các sketch đã duyệt; theme sáng và
  chức năng chuyển theme nằm ngoài phạm vi MVP.
- **FR-015b**: Mọi nhãn, hướng dẫn và thông báo hướng người dùng của F-011 MUST dùng tiếng Anh
  nhất quán; hệ thống đa ngôn ngữ nằm ngoài phạm vi MVP.
- **FR-016**: Tất cả form MUST ngăn gửi lặp trong lúc xử lý và MUST giữ lỗi gắn với trường hoặc
  hành động phù hợp.
- **FR-017**: Foundation MUST định nghĩa contract dùng chung cho route, auth/session state, public
  request/result, error mapping và realtime lifecycle để F-012/F-013 không tạo client cạnh tranh.
- **FR-018**: Mock adapter MUST cùng contract với adapter thật, MUST được gắn nhãn rõ trong UI/dev
  tooling và MUST không được bật mặc định trong production.
- **FR-019**: F-011 MUST chỉ tạo route shell/placeholder cho Market, Strategy, Backtest,
  Search & Leaderboard và News; Search và Leaderboard dùng chung route `/search` theo sketch đã
  duyệt. F-011 MUST không triển khai business screen của F-012/F-013.
- **FR-020**: Giao diện MUST không hiển thị token, credential, stack trace, provider payload,
  database model hoặc internal service detail.
- **FR-021**: F-011 MUST không thay đổi public Backend contract của F-009 hoặc business rule của
  các capability backend.

### Key Entities

- **Authenticated Session**: Danh tính hiện tại, trạng thái xác thực và thời hạn cần để quyết định
  route/request; không đại diện cho authorization nghiệp vụ.
- **Safe Redirect**: Route nội bộ mà người dùng được quay lại sau khi đăng nhập; không cho phép URL ngoài hệ thống.
- **Application Shell**: Khung điều hướng, tài khoản và vùng nội dung dùng chung cho F-012/F-013.
- **Client Result State**: Trạng thái chuẩn loading, success, empty, validation, unavailable hoặc degraded.
- **Realtime Connection State**: Trạng thái kết nối/tái kết nối và các logical subscription cần khôi phục.
- **Frontend Fixture**: Dữ liệu giả có schema tương thích contract, chỉ dành cho development/test.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Người dùng mới có thể hoàn thành đăng ký và hiểu bước tiếp theo trong dưới 2 phút.
- **SC-002**: Người dùng có tài khoản hợp lệ có thể đăng nhập và tới trang mặc định trong dưới 10 giây,
  không tính thời gian mạng/provider ngoài quyền kiểm soát.
- **SC-003**: 100% route nghiệp vụ được kiểm tra đều từ chối session không hợp lệ và quay lại đúng
  đích nội bộ sau đăng nhập thành công.
- **SC-004**: Sau Logout, 100% kiểm tra reload/Back navigation không hiển thị lại dữ liệu riêng.
- **SC-005**: Login và application shell không tràn ngang, không che thao tác chính tại chiều rộng
  từ 360px đến 1440px trong bộ viewport acceptance.
- **SC-006**: Toàn bộ thao tác chính của các form authentication và navigation hoàn thành được chỉ
  bằng bàn phím, có focus dễ nhận biết và nhãn có thể đọc bằng công cụ hỗ trợ.
- **SC-007**: F-012 và F-013 có thể thêm route mẫu đồng thời mà không sửa auth/session client,
  application shell hoặc contract transport dùng chung.
- **SC-008**: Chuyển route mẫu từ fixture sang public Backend adapter không yêu cầu đổi component contract.
- **SC-009**: Security verification phát hiện 0 token, password, privileged credential, stack trace
  hoặc internal detail trong browser-visible output và production bundle/configuration.

## Assumptions

- Email/password là phương thức authentication duy nhất của MVP; OAuth/social login ngoài phạm vi.
- MVP bắt buộc xác minh email trước khi đăng nhập, dùng dịch vụ email mặc định của Supabase và
  chưa cấu hình SMTP riêng.
- Password policy và email delivery do identity provider quản lý; application không tự lưu password
  hoặc triển khai mail server.
- Logout áp dụng cho session hiện tại; giao diện quản lý mọi thiết bị/session ngoài phạm vi.
- Các sketch hiện có là visual direction cho dark theme; chúng không tự động mở rộng business scope.
- Nội dung thật của Market/Strategy/News thuộc F-012; Experiment/Result/Leaderboard thuộc F-013.
- Public REST/WebSocket và authorization contract F-009 là nguồn chuẩn cho browser integration.
- F-011 không yêu cầu thay đổi database business schema hoặc migration.
- Account menu của MVP nhận diện người dùng bằng email; tên hiển thị và hồ sơ cá nhân ngoài phạm vi.
