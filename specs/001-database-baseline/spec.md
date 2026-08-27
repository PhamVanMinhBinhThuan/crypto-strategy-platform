# Feature Specification: Durable Data Baseline

**Feature Branch**: `feature/database-setup`

**Created**: 2026-08-27

**Status**: Draft

**Input**: User description: "Thiết lập nền tảng dữ liệu bền vững cho market,
strategy, experiment, backtest, leaderboard, news, sentiment và reliability;
mỗi Experiment thuộc một user cơ bản và dữ liệu phải tái lập được."

## Clarifications

### Session 2026-08-27

- Q: Feature database baseline có bao gồm application authentication và ownership không?
  → A: Không; feature chỉ triển khai schema, migration, database permissions và
  database tests. Application authentication/authorization và UI login thuộc
  feature sau.
- Q: Khi nào feature database baseline được xem là hoàn thành?
  → A: Sau khi migration được review, được user phê duyệt riêng, áp dụng thành
  công lên shared development environment và vượt qua database verification.
- Q: Feature database baseline bảo vệ dữ liệu bất biến bằng database trigger hay
  application rule? → A: Không dùng trigger chứa business policy; baseline cung
  cấp cấu trúc, constraint và permission, còn persistence feature sau thực
  thi immutability và invariant nhiều bảng trong transaction.
- Q: Feature database baseline có tự động cleanup dữ liệu hết hạn không? → A:
  Không; baseline chỉ cung cấp timestamp và index hỗ trợ cleanup. Background
  maintenance feature sau thực thi retention policy.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Khởi tạo nền tảng dữ liệu nhất quán (Priority: P1)

Là thành viên phát triển, tôi muốn một baseline dữ liệu có thể áp dụng lặp lại
trên môi trường development trống để các module dùng cùng entity, quan hệ và
ràng buộc đã được nhóm chốt.

**Why this priority**: Mọi persistence adapter và feature phía sau đều phụ thuộc
baseline; nếu cấu trúc không nhất quán, implementation sẽ phải sửa lại dây chuyền.

**Independent Test**: Áp dụng baseline trên một môi trường development trống và
xác nhận toàn bộ nhóm dữ liệu, quan hệ, constraint và quyền truy cập cần thiết
được tạo mà không thao tác thủ công.

**Acceptance Scenarios**:

1. **Given** một môi trường dữ liệu trống, **When** baseline được áp dụng,
   **Then** toàn bộ cấu trúc market, strategy, experiment, news và reliability
   được tạo thành công trong một quy trình có kiểm soát.
2. **Given** baseline đã được áp dụng, **When** kiểm tra lịch sử thay đổi,
   **Then** trạng thái môi trường khớp với phiên bản được lưu trong repository.
3. **Given** dữ liệu không thỏa identity, range hoặc lifecycle value đã chốt,
   **When** dữ liệu được ghi, **Then** hệ thống từ chối và không tạo bản ghi một phần.
4. **Given** migration đã qua review và được user phê duyệt triển khai, **When**
   áp dụng lên shared development environment, **Then** migration hoàn tất và verification
   xác nhận schema, constraint, index cùng permission đúng baseline.

---

### User Story 2 - Tạo nền tảng ownership cho Experiment (Priority: P1)

Là thành viên phát triển, tôi muốn mỗi Experiment bắt buộc tham chiếu một user và
mọi dữ liệu con có đường truy ngược ownership rõ ràng để feature authorization
sau này có thể thực thi đúng ranh giới.

**Why this priority**: User ownership là ranh giới bảo mật bắt buộc trước khi hệ
thống lưu Experiment của nhiều người.

**Independent Test**: Tạo hai identity và các Experiment tương ứng; xác nhận
Experiment thiếu owner bị từ chối, bảng con truy ngược đúng owner và idempotency
key giống nhau của hai user không xung đột.

**Acceptance Scenarios**:

1. **Given** một Experiment mới, **When** không có owner hợp lệ, **Then** bản ghi
   bị từ chối.
2. **Given** một Experiment có owner, **When** truy theo Candidate, Result hoặc
   Trade, **Then** có thể xác định duy nhất owner thông qua Experiment.
3. **Given** hai user dùng cùng idempotency key cho cùng loại command, **When**
   cả hai gửi request, **Then** hai request không xung đột với nhau.

---

### User Story 3 - Tái lập kết quả Experiment (Priority: P1)

Là người đánh giá Strategy, tôi muốn mỗi kết quả truy được về đúng Dataset,
Strategy, cấu hình, phiên bản đánh giá và phần mềm để có thể chạy kiểm chứng mà
không làm thay đổi kết quả gốc.

**Why this priority**: Reproducibility là giá trị cốt lõi của Experiment và
Leaderboard, không thể bổ sung đáng tin cậy nếu dữ liệu nền đã bị overwrite.

**Independent Test**: Tạo fixture gồm Dataset, Strategy, Experiment, Result và
Evaluation; xác nhận truy đủ provenance và có thể lưu một reproduction record mới
cùng manifest fingerprint mà không ghi đè identity gốc.

**Acceptance Scenarios**:

1. **Given** một Experiment fixture, **When** truy provenance, **Then** schema cung
   cấp đầy đủ reference tới manifest, Dataset membership, Strategy snapshot,
   Candidate, Result và Evaluation.
2. **Given** cùng frozen input, **When** lưu reproduction fixture, **Then** schema
   cho phép cùng manifest fingerprint nhưng yêu cầu identity/result mới và reference
   về bản gốc.
3. **Given** provider bổ sung hoặc sửa Candle, **When** Dataset mới được freeze,
   **Then** Dataset cũ và Experiment đang tham chiếu vẫn giữ nguyên.

---

### User Story 4 - Chống trùng và phục hồi xử lý nền (Priority: P2)

Là người vận hành, tôi muốn retry hoặc duplicate delivery không tạo Candle,
Result, Sentiment hay Leaderboard entry trùng và sự kiện chưa publish vẫn có thể
được phục hồi.

**Why this priority**: Queue và provider đều có thể gửi lại dữ liệu; thiếu
idempotency sẽ làm sai kết quả và ranking.

**Independent Test**: Chèn lặp cùng Candle, membership, Candidate, message và
sentiment identity; xác nhận constraint từ chối duplicate và outbox chưa publish
có thể được truy vấn để feature Worker sau xử lý.

**Acceptance Scenarios**:

1. **Given** Candle có cùng provider, pair, timeframe và open time, **When** ghi
   lần thứ hai, **Then** không xuất hiện Candle logic thứ hai.
2. **Given** cùng Candidate được retry, **When** nhiều Attempt hoàn thành,
   **Then** Candidate có tối đa một Result thành công được chấp nhận.
3. **Given** một Outbox Event chưa publish, **When** truy vấn recovery state,
   **Then** event được tìm thấy và processed-message identity có thể chống ghi trùng.

### Edge Cases

- Dataset membership chứa cùng Candle ở hai vị trí hoặc Candle sai pair/timeframe.
- Attempt thành công được gắn nhầm sang Candidate khác.
- Leaderboard revision chứa Evaluation của Experiment khác hoặc trùng rank.
- User bị vô hiệu hóa nhưng Experiment cũ vẫn cần giữ để audit/reproduce.
- Strategy/Composite version được tham chiếu nhưng artifact không còn resolve được.
- News content thay đổi nhưng URL giữ nguyên, làm content hash và sentiment input đổi.
- Giá trị decimal vượt precision hoặc timestamp/range không hợp lệ.
- Dịch vụ queue/cache mất toàn bộ dữ liệu tạm thời.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST có một lịch sử thay đổi dữ liệu duy nhất, có thứ tự
  và có thể áp dụng trên môi trường development trống mà không chỉnh tay.
- **FR-002**: Hệ thống MUST tổ chức dữ liệu theo các ownership boundary Market,
  Strategy, Experiment, News và Platform Reliability.
- **FR-003**: Hệ thống MUST dùng identity ổn định và từ chối identity không đúng
  định dạng hoặc bị trùng theo business key đã công bố.
- **FR-004**: Hệ thống MUST chỉ lưu closed Candle bền vững và deduplicate theo
  provider, pair, timeframe và open time.
- **FR-005**: Hệ thống MUST hỗ trợ các timeframe canonical `1m`, `5m`, `15m`,
  `30m`, `1h`, `2h`, `4h`, `1d`; bốn mặc định là `5m`, `15m`, `1h`, `4h`.
- **FR-006**: Hệ thống MUST freeze ordered Dataset membership, count, checksum,
  range và normalization version; Dataset đã được tham chiếu không bị overwrite.
- **FR-007**: Hệ thống MUST lưu Strategy/Composite snapshot bất biến khi lần đầu
  được Experiment hoặc Composite tham chiếu.
- **FR-008**: Hệ thống MUST tách Experiment runtime state khỏi manifest bất biến
  và lưu đầy đủ provenance cần để reproduce.
- **FR-009**: Hệ thống MUST tách Candidate Definition, Execution Attempt,
  Backtest Result, Trade và Evaluation Result; retry không tạo Result trùng.
- **FR-010**: Hệ thống MUST cho phép một Backtest Result có nhiều Evaluation theo
  metric version và giữ các Leaderboard revision bất biến khi Top-K thay đổi.
- **FR-011**: Hệ thống MUST lưu News metadata/content theo retention được phép và
  lưu Sentiment Result bất biến theo content hash cùng model version.
- **FR-012**: Hệ thống MUST lưu riêng sentiment label, confidence và polarity;
  confidence thuộc `0..1`, polarity thuộc `-1..1`.
- **FR-013**: Hệ thống MUST gắn mỗi Experiment với đúng một user identity hợp lệ
  và cung cấp đường ownership duy nhất từ mọi dữ liệu Experiment con.
- **FR-014**: Hệ thống MUST từ chối quyền đọc/ghi trực tiếp business data của
  client role; application authentication và authorization không thuộc feature này.
- **FR-015**: Hệ thống MUST scope HTTP idempotency theo user, command scope và key.
- **FR-016**: Hệ thống MUST lưu durable outbox và processed-message identity để
  hỗ trợ at-least-once delivery mà không nhân đôi business outcome.
- **FR-017**: Hệ thống MUST giữ dữ liệu tái lập đang được tham chiếu; dữ liệu kỹ
  thuật hết hạn MUST có timestamp/index hỗ trợ cleanup theo retention. Scheduled
  cleanup không thuộc feature này.
- **FR-018**: Hệ thống MUST dùng biểu diễn decimal chính xác cho price, quantity,
  money, fee, rate, metric và score; không chấp nhận sai số nhị phân.
- **FR-019**: Hệ thống MUST kiểm tra trong cùng transaction các invariant không
  thể biểu diễn bằng constraint đơn giản, gồm Candidate/Attempt, Dataset/Candle,
  Leaderboard/Experiment và số component tối thiểu. Việc kiểm tra thuộc
  persistence feature sau; baseline MUST ghi rõ contract này.
- **FR-020**: Baseline MUST được áp dụng và verification trên shared development
  sau khi có phê duyệt triển khai riêng; dry-run một mình không đủ để hoàn thành
  feature.
- **FR-021**: Baseline MUST NOT dùng database trigger để triển khai business
  immutability hoặc invariant nhiều bảng.

### Key Entities

- **User/Profile**: Identity đăng nhập và metadata hiển thị; sở hữu Experiment.
- **Asset/Trading Pair/Candle**: Market reference và closed OHLCV có identity ổn định.
- **Dataset Version/Membership**: Snapshot có thứ tự của Candle dùng cho backtest.
- **Strategy/Composite Version**: Snapshot versioned của plugin, parameters và policy.
- **Experiment/Manifest**: Runtime identity cùng input/provenance bất biến.
- **Candidate/Execution Attempt**: Cấu hình được sinh và lịch sử các lần chạy.
- **Backtest Result/Trade**: Kết quả thành công bất biến và chuỗi giao dịch.
- **Evaluation/Leaderboard Revision**: Metric versioned và projection Top-K bất biến.
- **News Item/Sentiment Result**: Tin chuẩn hóa và kết quả model theo version.
- **Outbox/Processed Message/Idempotency Record**: Reliability và chống xử lý trùng.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Một thành viên có thể khởi tạo toàn bộ baseline trên môi trường
  development trống bằng một quy trình duy nhất, không chỉnh cấu trúc thủ công.
- **SC-008**: Migration đã review chạy thành công trên shared development
  100% database verification bắt buộc đạt mà không chỉnh schema thủ công.
- **SC-002**: 100% test duplicate cho Candle, Dataset membership, Candidate,
  Result, Evaluation, Leaderboard rank, Sentiment và processed message bị từ chối
  hoặc trả lại business outcome đã có.
- **SC-003**: 100% test tạo Experiment thiếu owner bị từ chối; toàn bộ Candidate,
  Result và Trade fixture truy được về đúng một owner, và idempotency key giữa
  hai user không xung đột.
- **SC-004**: Một Result bất kỳ truy được 100% Dataset, Strategy, manifest,
  assumptions, software version và evaluation version cần cho reproduction.
- **SC-005**: Database lưu được hai Experiment fixture có cùng manifest fingerprint
  nhưng identity riêng, reference reproduction hợp lệ và đầy đủ Trade/metric data.
- **SC-006**: Sau khi xóa toàn bộ cache/queue transient trong bài kiểm thử recovery,
  không mất Experiment/Result và mọi event chưa publish vẫn được nhận diện.
- **SC-007**: Không credential đặc quyền hoặc quyền đọc/ghi business data trực
  tiếp xuất hiện trong client/browser bundle.

## Assumptions

- MVP có user cá nhân, chưa có tenant, organization, workspace hoặc role phức tạp.
- Market data, Strategy catalog và News là dữ liệu dùng chung giữa các user.
- Xóa tài khoản/cascade business data không thuộc scope; vô hiệu hóa user vẫn giữ
  dữ liệu phục vụ audit và reproducibility.
- Database development hiện tại chưa chứa business schema cần bảo toàn.
- News content retention phụ thuộc license provider và tối đa 30 ngày khi được lưu.
- Full persistence adapter, business use case và UI đăng nhập được triển khai ở
  feature sau; feature này cung cấp baseline và contract kiểm chứng được.
- Persistence feature sau chịu trách nhiệm ngăn update/delete dữ liệu bất biến
  và kiểm tra invariant nhiều bảng trong transaction.
- Background maintenance feature sau chịu trách nhiệm chạy retention cleanup;
  database baseline chỉ cung cấp timestamp và index cần thiết.
- Không bổ sung đường truy cập trực tiếp từ ứng dụng người dùng tới kho dữ liệu
  nghiệp vụ trong feature này.
