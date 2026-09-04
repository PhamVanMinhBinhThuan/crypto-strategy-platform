# Feature Specification: F-014 — End-to-End Demo and Hardening

**Feature Branch**: `feature/014-end-to-end-demo-hardening`

**Created**: 2026-09-04

**Status**: Implemented — controlled verification complete; LIVE/release blockers remain documented

**Input**: User description: "Tích hợp và harden toàn bộ Crypto Strategy Lab thành một luồng demo end-to-end có thể kiểm chứng, dựa trên F-003 đến F-013 và rubric trong File Danh Gia Copy.xlsx."

## Dependencies and Readiness Gates

- F-014 tích hợp các capability đã phát hành từ F-003 đến F-013; nó không được thay thế business contract của feature sở hữu capability.
- F-012 tiếp tục sở hữu Market, Strategy và News UI; F-013 tiếp tục sở hữu Experiment, Backtest Result và Leaderboard UI. F-014 chỉ nối luồng, sửa lỗi tích hợp và harden các trạng thái dùng chung.
- Luồng demo chính phải dùng application boundary thật và dữ liệu bền vững. Chế độ fixture chỉ là phương án dự phòng được ghi nhãn rõ, không được trình bày như bằng chứng tích hợp production.
- Những tiêu chí chưa có implementation hoặc bằng chứng thật phải được ghi là gap; không được tạo log, benchmark, trạng thái Verified hoặc kết quả demo giả.
- F-014 không được tự nhận điểm cho chức năng nâng cao chỉ vì dự án sử dụng công nghệ phức tạp. Mỗi tuyên bố nâng cao phải có code, test hoặc phép đo và giải thích giá trị kiến trúc.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trình diễn luồng nghiên cứu Strategy hoàn chỉnh (Priority: P1)

Là người dùng đã đăng nhập, tôi muốn đi từ dữ liệu thị trường đến Strategy, Search, Backtest, Evaluation, Leaderboard và chi tiết giao dịch trong một hành trình liền mạch để chứng minh các capability của hệ thống hoạt động cùng nhau.

**Why this priority**: Đây là luồng giá trị chính và là tiêu chí demo end-to-end quan trọng nhất trong rubric.

**Independent Test**: Khởi động hệ thống từ hướng dẫn sạch, đăng nhập bằng tài khoản demo, chọn dữ liệu và Strategy, bắt đầu một Search hữu hạn, quan sát tiến trình đến khi có Leaderboard, mở kết quả đứng hạng và kiểm tra metrics, trades cùng provenance mà không sửa database thủ công.

**Acceptance Scenarios**:

1. **Given** hệ thống và các dependency bắt buộc sẵn sàng, **When** người dùng mở Market Dashboard, **Then** bốn chart của cùng cặp giao dịch hiển thị dữ liệu và có thể đổi timeframe độc lập mà không làm thay đổi sai các chart khác.
2. **Given** catalog Strategy đã tải, **When** người dùng mở Strategy Composer, **Then** ít nhất Moving Average, RSI, Bollinger Bands và Support/Resistance có thể được nhận diện qua cùng contract tín hiệu chuẩn và dùng để tạo cấu hình hợp lệ.
3. **Given** một Dataset đóng băng và Strategy hợp lệ, **When** người dùng bắt đầu Search với stop condition hữu hạn, **Then** yêu cầu được chấp nhận một lần, trạng thái Experiment/Job có thể quan sát và candidate lần lượt đi qua Backtest, Evaluation và Ranking.
4. **Given** Search có candidate đủ điều kiện, **When** kết quả mới tốt hơn xuất hiện, **Then** Leaderboard cập nhật từ dữ liệu authoritative và giữ thứ tự Top-K xác định.
5. **Given** một Leaderboard entry, **When** người dùng mở kết quả, **Then** hệ thống hiển thị bốn metrics bắt buộc, danh sách Trade, Entry/Exit, tín hiệu liên quan và provenance của kết quả đó.
6. **Given** luồng chính hoàn tất, **When** người dùng mở News, **Then** News liên quan và Sentiment đã lưu được hiển thị mà không yêu cầu chạy lại Search hoặc Backtest.

---

### User Story 2 - Demo vẫn kiểm soát được khi dependency lỗi (Priority: P1)

Là người trình bày, tôi muốn các lỗi thường gặp được cô lập và có đường phục hồi rõ ràng để một dependency hỏng không biến toàn bộ buổi demo thành màn hình lỗi không giải thích được.

**Why this priority**: Reliability và failure isolation là bằng chứng kiến trúc bắt buộc, không chỉ là trải nghiệm bổ sung.

**Independent Test**: Trong một phiên demo có Market và technical Backtest đang hoạt động, lần lượt gây lỗi Sentiment, ngắt kết nối Market, dừng Worker hoặc làm gián đoạn queue; quan sát trạng thái, ảnh hưởng được giới hạn và quá trình phục hồi theo runbook.

**Acceptance Scenarios**:

1. **Given** Market và technical Backtest đang hoạt động, **When** Sentiment không phản hồi, **Then** News/Sentiment hiển thị degraded hoặc trạng thái xử lý phù hợp trong khi Market chart và technical Backtest tiếp tục dùng được.
2. **Given** chart đang nhận dữ liệu, **When** kết nối nguồn Market bị ngắt tạm thời, **Then** người dùng thấy trạng thái freshness chính xác và hệ thống có thể kết nối lại, lấy bù, sắp xếp và loại dữ liệu trùng.
3. **Given** một Job đang xử lý, **When** Worker dừng ngoài ý muốn, **Then** trạng thái bền vững không bị mất và Job có thể được reclaim hoặc retry mà không tạo business result trùng.
4. **Given** một command đã được ghi bền vững, **When** hệ thống phân phối message bị gián đoạn, **Then** command không mất và có thể được phát lại sau khi dependency phục hồi.
5. **Given** dependency live không thể phục hồi trong thời gian demo, **When** người trình bày kích hoạt phương án dự phòng, **Then** giao diện ghi rõ đây là fixture/demo fallback và không tuyên bố đó là live integration evidence.

---

### User Story 3 - Truy vết và tái lập kết quả (Priority: P1)

Là giảng viên hoặc reviewer, tôi muốn lần ngược một dòng Leaderboard về toàn bộ đầu vào và chạy kiểm chứng lại để đánh giá kết quả có minh bạch và tái lập được hay không.

**Why this priority**: Provenance và reproducibility là yêu cầu kiến trúc cốt lõi, giúp phân biệt kết quả khoa học với một con số không có nguồn gốc.

**Independent Test**: Chọn một Leaderboard entry, truy về Evaluation, Backtest Result, Trades, successful Attempt, Candidate và frozen Manifest; sau đó tạo một reproduction run và kiểm tra báo cáo so sánh mà không ghi đè evidence gốc.

**Acceptance Scenarios**:

1. **Given** một Leaderboard entry, **When** reviewer truy vết kết quả, **Then** có thể xác định Dataset/checksum/timeframe, Strategy/version/parameters, Candidate, Attempt, assumptions và các phiên bản Evaluation/Ranking liên quan.
2. **Given** một Experiment đã hoàn tất và artifact còn đầy đủ, **When** người dùng yêu cầu reproduce, **Then** hệ thống tạo run mới liên kết run gốc và giữ nguyên evidence ban đầu.
3. **Given** reproduction hoàn tất, **When** hệ thống so sánh evidence, **Then** trạng thái `MATCHED` chỉ được công bố khi trades theo thứ tự, metrics và fingerprint bắt buộc đều khớp; mọi sai khác phải được báo `MISMATCHED` có thể điều tra.

---

### User Story 4 - Chuẩn bị bộ demo và minh chứng có thể lặp lại (Priority: P2)

Là thành viên nhóm, tôi muốn có một runbook và evidence map duy nhất để bất kỳ thành viên nào cũng có thể cài đặt, chạy demo, thực hiện scenario lỗi và dẫn reviewer đến đúng minh chứng.

**Why this priority**: Một implementation tốt nhưng không cài được hoặc không tìm thấy bằng chứng sẽ không đáp ứng tiêu chí hồ sơ và demo.

**Independent Test**: Một thành viên không viết feature làm theo hướng dẫn trên checkout sạch, hoàn tất demo chính và ít nhất hai scenario kiến trúc, rồi tìm được evidence tương ứng từ rubric mà không cần hướng dẫn miệng ngoài tài liệu.

**Acceptance Scenarios**:

1. **Given** checkout sạch và các prerequisite đã công bố, **When** thành viên làm theo hướng dẫn cài đặt, **Then** có thể khởi động các thành phần cần thiết bằng cấu hình không chứa secret trong repository.
2. **Given** checklist demo, **When** một bước được đánh dấu hoàn tất, **Then** bước đó có đường dẫn đến code, test, ảnh, log hoặc measurement thật cùng commit và môi trường kiểm chứng.
3. **Given** rubric đánh giá, **When** reviewer xem từng tiêu chí bắt buộc, **Then** tiêu chí có trạng thái trung thực: Verified, Partially Verified, Planned hoặc Blocked, kèm gap/remediation nếu chưa đạt.
4. **Given** một người trình bày khác, **When** chạy theo kịch bản, **Then** họ có thể hoàn thành luồng chính và phương án fallback mà không cần sửa source hoặc database thủ công.

---

### User Story 5 - Kiểm tra chất lượng trước khi nộp (Priority: P2)

Là nhóm phát triển, tôi muốn có quality gate cuối cùng cho bảo mật, hiệu năng, accessibility và regression để tránh một thay đổi tích hợp phá capability đã hoàn thành.

**Why this priority**: F-014 là cổng phát hành cuối; các lỗi cross-feature thường chỉ xuất hiện khi toàn hệ thống chạy chung.

**Independent Test**: Chạy bộ kiểm tra được tài liệu hóa trên commit ứng viên phát hành và xác nhận mọi gate bắt buộc có kết quả thật, có thể xem lại.

**Acceptance Scenarios**:

1. **Given** commit ứng viên, **When** chạy secret/security scan, **Then** không có privileged credential, token thật hoặc secret đã biết trong source, browser artifact, log mẫu và public error evidence.
2. **Given** luồng demo chuẩn, **When** chạy performance smoke theo cấu hình cố định, **Then** thời gian từng giai đoạn và tổng thời gian được ghi lại, không có timeout không kiểm soát hoặc duplicate result.
3. **Given** viewport từ 360px đến desktop và người dùng chỉ dùng bàn phím, **When** đi qua các hành động demo chính, **Then** nội dung quan trọng, trạng thái và hành động vẫn đọc và sử dụng được.
4. **Given** toàn bộ test suite bắt buộc, **When** chạy trên commit ứng viên, **Then** không có regression chưa được chấp thuận và các test bị skip/gated được liệt kê thay vì tính là pass.

### Edge Cases

- Nguồn Market không truy cập được ngay trước hoặc trong buổi demo.
- Dataset không đủ dữ liệu cho lookback của Strategy đã chọn.
- Một hoặc nhiều Strategy bắt buộc chưa có trong catalog hoặc có parameter schema không hợp lệ.
- Search Space bị cạn trước khi đạt số candidate tối đa.
- Experiment bị stop khi vẫn còn Job đang chạy hoặc chờ retry.
- Realtime event bị trùng, đến sai thứ tự hoặc bị mất trong lúc reconnect.
- Candidate không đủ số Trade để vào Leaderboard dù Backtest đã thành công.
- Leaderboard rỗng hoặc entry trỏ tới result không thể truy cập.
- Reproduction thiếu Dataset/Strategy version hoặc checksum không còn khớp.
- News Provider hoạt động nhưng Sentiment không sẵn sàng, và trường hợp ngược lại.
- Một thành phần dùng chung không sẵn sàng khiến live E2E không thể tiếp tục.
- Evidence được tạo từ commit hoặc cấu hình khác với commit ứng viên phát hành.

## Requirements *(mandatory)*

### Functional Requirements

#### End-to-End Integration

- **FR-001**: Hệ thống MUST cung cấp một hành trình demo liên tục từ Market Data, Dataset, Strategy, Search, Backtest, Evaluation, Leaderboard đến Result/Trade detail và News/Sentiment.
- **FR-002**: Hành trình chính MUST sử dụng authentication, authorization và public application boundary hiện có; browser MUST NOT truy cập trực tiếp business storage hoặc tự thực thi Search, Backtest, Evaluation hay Ranking.
- **FR-003**: Market Dashboard MUST hỗ trợ và trong demo MUST hiển thị đồng thời bốn chart, đồng thời cho phép mỗi chart chọn timeframe độc lập.
- **FR-004**: Catalog demo MUST có ít nhất bốn Strategy đơn: Moving Average, RSI, Bollinger Bands và Support/Resistance, cùng sử dụng tín hiệu chuẩn và cơ chế cấu hình thống nhất.
- **FR-005**: Người dùng MUST có thể tạo và sử dụng Strategy cá nhân từ Strategy hệ thống đã phát hành, đồng thời kết hợp nhiều Strategy bằng quy tắc xử lý xung đột được công bố; các khả năng này MUST NOT đồng nghĩa với upload hoặc chạy source code tùy ý.
- **FR-006**: Search demo MUST có ít nhất Random Search, Search Space hợp lệ và ít nhất một stop condition hữu hạn.
- **FR-007**: Mỗi candidate MUST đi qua cùng pipeline Backtest, Evaluation và Ranking; browser MUST NOT tạo candidate hoặc tính score thay backend.
- **FR-008**: Backtest Result MUST hiển thị Entry/Exit hoặc Buy/Sell evidence, Trade detail và đúng bốn metrics bắt buộc: Return, Win Rate, Maximum Drawdown và Number of Trades.
- **FR-009**: Leaderboard MUST hiển thị Top-K xác định, cập nhật khi có revision mới và cho phép mở authoritative Result của entry đã chọn.
- **FR-010**: News MUST đi qua collect, normalize, store và sentiment analysis; giao diện chỉ hiển thị dữ liệu đã được public contract cung cấp.

#### Reliability and Recovery

- **FR-011**: Sentiment failure MUST được cô lập khỏi Market Data và technical Backtest, đồng thời trạng thái degraded MUST được trình bày rõ.
- **FR-012**: Market disconnect MUST có trạng thái freshness trung thực và đường reconnect/backfill không tạo Candle trùng.
- **FR-013**: Worker interruption MUST không làm mất Job bền vững và MUST không tạo business outcome trùng khi retry hoặc reclaim.
- **FR-014**: Queue/cache interruption MUST không xóa durable Experiment, Job, Result, News, Sentiment hoặc publication state đã được chấp nhận.
- **FR-015**: Retry MUST hữu hạn; lỗi không retry được MUST có trạng thái terminal có thể quan sát và không chặn công việc hợp lệ khác.
- **FR-016**: Realtime recovery MUST giữ snapshot bền vững gần nhất, đánh dấu freshness phù hợp và reconcile lại từ nguồn authoritative sau reconnect.
- **FR-017**: Demo fallback MUST được tách khỏi production composition, chỉ bật chủ động trong môi trường cho phép và hiển thị nhãn fixture/demo rõ ràng.

#### Provenance and Reproduction

- **FR-018**: Mọi Result được demo MUST truy được về Experiment, Candidate, successful Attempt, Dataset version/checksum, Strategy version/parameters và execution assumptions.
- **FR-019**: Manifest, Candidate, accepted Result, Trade, Evaluation và Leaderboard revision MUST giữ bất biến sau khi được chấp nhận.
- **FR-020**: Reproduction MUST tạo run mới liên kết evidence gốc và MUST NOT ghi đè run gốc.
- **FR-021**: Báo cáo reproduction MUST phân biệt `MATCHED` và `MISMATCHED` dựa trên so sánh deterministic của evidence bắt buộc.

#### Demo, Evidence and Release Readiness

- **FR-022**: Repository MUST có một runbook thống nhất cho prerequisite, cấu hình an toàn, startup order, seed/setup dữ liệu demo, luồng chính, scenario lỗi, recovery và cleanup.
- **FR-023**: Repository MUST có demo checklist ánh xạ mỗi bước tới kết quả mong đợi, người phụ trách và evidence có thể xem lại.
- **FR-024**: Cả 24 dòng chấm điểm trong rubric MUST có trạng thái trung thực và đường dẫn bằng chứng hoặc gap/remediation cụ thể; dòng mở rộng có giá trị MAY ghi không tuyên bố điểm nếu hệ thống chưa có phần vượt yêu cầu được chứng minh.
- **FR-025**: Evidence MUST ghi commit, ngày, môi trường, cấu hình không nhạy cảm, lệnh hoặc thao tác và kết quả thật; test bị skip MUST NOT được tính là pass.
- **FR-026**: README và tài liệu kiến trúc MUST dẫn người đọc tới luồng chạy, sơ đồ chính, ADR và evidence mà không tạo tài liệu mâu thuẫn.
- **FR-027**: Hệ thống MUST vượt qua secret/security scan và MUST không đưa privileged credential vào source, browser artifact, public error hoặc committed demo log.
- **FR-028**: Performance smoke MUST dùng workload/cấu hình cố định, ghi thời gian từng giai đoạn, throughput hoặc queue progress liên quan, và lưu kết quả thật thay vì số liệu giả định.
- **FR-029**: Demo chính MUST có kịch bản accessibility/responsive cho viewport 360px đến desktop, keyboard navigation và trạng thái không phụ thuộc riêng vào màu sắc.
- **FR-030**: Release checklist MUST liệt kê tất cả dependency, gate, known limitation và rollback/fallback action còn tồn tại.

### Key Entities

- **Demo Scenario**: Một hành trình hoặc failure case có precondition, thao tác, kết quả mong đợi và cleanup rõ ràng.
- **Evidence Record**: Bằng chứng gắn với tiêu chí, commit, môi trường, cấu hình, thao tác và kết quả kiểm chứng thật.
- **Readiness Finding**: Gap hoặc regression được phát hiện, mức độ ảnh hưởng, owner, remediation và trạng thái.
- **Demo Profile**: Bộ cấu hình không nhạy cảm xác định dữ liệu, Strategy, Search Space, stop condition và account role dùng cho một lần demo.
- **Fallback Profile**: Chế độ dự phòng có nhãn rõ, tách khỏi production và không được dùng để khẳng định live integration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Một người dùng đã đăng nhập hoàn thành luồng Market → Strategy → Search → Backtest → Evaluation → Leaderboard → Result/Trades → News/Sentiment trong tối đa 10 phút theo runbook, không sửa source hoặc database thủ công.
- **SC-002**: Cả bốn chart hiển thị đồng thời và mỗi chart đổi timeframe độc lập trong 100% bước kiểm thử demo đã ghi.
- **SC-003**: Catalog demo cung cấp đủ bốn Strategy bắt buộc và mỗi Strategy vượt qua cùng contract/validation gate trước khi được dùng trong Search hoặc Backtest.
- **SC-004**: 100% Leaderboard entry được chọn trong demo truy được về Result, Candidate, successful Attempt, Dataset/checksum/timeframe, Strategy/version/parameters và Trades.
- **SC-005**: Ít nhất hai failure scenario được chạy và lưu evidence thật; một scenario MUST chứng minh Sentiment failure không gián đoạn Market/technical Backtest, scenario còn lại MUST chứng minh recovery hoặc duplicate safety của luồng bất đồng bộ.
- **SC-006**: Reproduction của result demo tạo run mới và cho kết luận `MATCHED` hoặc `MISMATCHED` có evidence; không có artifact gốc bị ghi đè.
- **SC-007**: 100% trong 24 dòng rubric có trạng thái và link evidence hoặc gap/remediation; 23 tiêu chí cốt lõi không được để trống, còn tiêu chí mở rộng chỉ được tự khai khi có implementation và evidence thật.
- **SC-008**: Secret/security scan trên commit ứng viên phát hiện 0 privileged credential hoặc secret thật trong phạm vi kiểm tra.
- **SC-009**: Performance smoke được lặp tối thiểu 3 lần với cùng profile; báo cáo công bố median, từng kết quả và mọi lỗi/timeout thay vì chỉ chọn lần tốt nhất.
- **SC-010**: 100% bước demo chính sử dụng được bằng bàn phím và không gây tràn viewport toàn trang tại 360px, 768px, 1024px và 1440px; bảng rộng chỉ cuộn trong vùng bảng.
- **SC-011**: Một thành viên không triển khai F-014 có thể thực hiện runbook, tìm evidence cho từng bước và hoàn tất cả luồng chính lẫn fallback mà không cần chỉ dẫn ngoài tài liệu.
- **SC-012**: Toàn bộ automated gate bắt buộc trên commit ứng viên phát hành pass; mọi test bị skip, dependency gate hoặc phép đo chưa chạy được đều được liệt kê rõ và không tính là pass.

## Assumptions

- F-014 ưu tiên hoàn thành yêu cầu bắt buộc và bằng chứng kiến trúc trước các mục điểm cộng nâng cao.
- Environment live demo có tài khoản test hợp lệ và quyền truy cập các dependency bên ngoài cần thiết; secret được cung cấp ngoài repository.
- Workload demo chính được giữ nhỏ và hữu hạn để hoàn thành trong buổi trình bày, nhưng vẫn đi qua pipeline production thực tế.
- Khi dependency bên ngoài không ổn định, fallback có thể bảo vệ khả năng trình bày UI và luồng tương tác nhưng không thay thế live integration evidence.
- F-014 được phép sửa defect tích hợp hoặc prerequisite còn thiếu trong F-003–F-013, nhưng thay đổi contract/schema/kiến trúc mới phải quay về owner artifact và quy trình review tương ứng.
- File `File Danh Gia Copy.xlsx` là rubric đầu vào để tạo evidence map; việc tự đánh giá không tự động quyết định điểm của giảng viên.

## Out of Scope

- Giao dịch tiền thật, quản lý ví hoặc lời khuyên đầu tư.
- Cho người dùng upload/chạy Strategy source code, script hoặc thư viện không tin cậy.
- LLM-generated Strategy, Genetic/Bayesian/Evolutionary Search, Long/Short nâng cao, Stop Loss, Take Profit hoặc multiple exchanges nếu chưa có feature riêng và evidence hoàn chỉnh.
- Thay thế kiến trúc hiện tại bằng nền tảng phân tán mới chỉ để phục vụ demo.
- Tạo số liệu benchmark, log, screenshot hoặc trạng thái Verified giả để lấp gap của rubric.
