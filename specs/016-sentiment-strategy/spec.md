# Feature Specification: F-016 — Sentiment Strategy

**Feature Branch**: `feature/016-sentiment-strategy`

**Created**: 2026-09-05

**Status**: Draft

**Input**: Người dùng muốn triển khai Sentiment thành một Strategy có thể cấu hình, Search, Backtest,
kết hợp với Strategy kỹ thuật và truy vết tới kết quả Machine Learning nguồn để đáp ứng phần mở rộng
của rubric.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Backtest Sentiment Strategy (Priority: P1)

Người nghiên cứu chọn Sentiment Strategy từ catalog, cấu hình khoảng thời gian News, số bài tối thiểu
và ngưỡng tín hiệu, sau đó chạy Backtest bằng các Sentiment Result đã được đóng băng cùng dữ liệu thị
trường. Kết quả giải thích được tín hiệu BUY, SELL hoặc HOLD bằng score và News nguồn.

**Why this priority**: Đây là lát cắt tối thiểu chứng minh Sentiment không chỉ được thu thập và hiển
thị mà thực sự tham gia vào quyết định Strategy.

**Independent Test**: Dùng một Dataset và sentiment snapshot cố định có đủ ba vùng điểm; Backtest phải
tạo đúng BUY/HOLD/SELL đã biết, thực thi theo chính sách hiện hữu và trả Result có evidence nguồn.

**Acceptance Scenarios**:

1. **Given** đủ News hợp lệ trong lookback và sentiment score bằng hoặc lớn hơn ngưỡng mua, **When**
   Strategy đánh giá tại thời điểm đóng Candle, **Then** nó trả BUY cùng score, số bài và định danh
   snapshot/model đã dùng.
2. **Given** đủ News hợp lệ và score bằng hoặc nhỏ hơn ngưỡng bán, **When** Strategy đánh giá,
   **Then** nó trả SELL; trong Backtest long-only, SELL chỉ đóng vị thế LONG hiện có và không mở SHORT.
3. **Given** score nằm giữa hai ngưỡng hoặc số News thấp hơn mức tối thiểu, **When** Strategy đánh giá,
   **Then** nó trả HOLD với lý do rõ ràng thay vì dựng dữ liệu hoặc fail toàn Backtest.
4. **Given** có News được phát hành sau thời điểm đánh giá, **When** Strategy chạy, **Then** News tương
   lai không tham gia score và không xuất hiện trong evidence của quyết định.

---

### User Story 2 - Search và kết hợp Sentiment với Strategy kỹ thuật (Priority: P2)

Người nghiên cứu có thể dùng Sentiment Strategy độc lập hoặc kết hợp nó với MA, RSI, Bollinger Bands
hay Support/Resistance theo chính sách Composite hiện hữu. Các tham số sentiment hợp lệ có thể tham
gia Search hữu hạn để so sánh Candidate bằng cùng Dataset và model release.

**Why this priority**: Luồng này chứng minh Strategy mới tuân theo cùng cơ chế mở rộng như Strategy kỹ
thuật và tạo giá trị vượt pipeline Sentiment cơ bản.

**Independent Test**: Tạo một Composite gồm Sentiment và một Strategy kỹ thuật, publish, chạy Search
với stop condition hữu hạn, rồi xác nhận Candidate/Result tham chiếu đúng hai thành phần cùng
sentiment provenance.

**Acceptance Scenarios**:

1. **Given** Sentiment Strategy đã xuất hiện trong catalog, **When** người dùng tạo Composite với một
   Strategy kỹ thuật, **Then** conflict được giải quyết đúng chính sách đã chọn và không có logic tổng
   hợp tín hiệu riêng ở giao diện.
2. **Given** search space chứa ngưỡng mua, ngưỡng bán, lookback và số bài tối thiểu hợp lệ, **When**
   Search chạy, **Then** mọi Candidate giữ cùng sentiment snapshot/model release và dừng theo điều
   kiện hữu hạn đã cấu hình.
3. **Given** Candidate thay đổi một tham số Sentiment Strategy, **When** Backtest hoàn tất, **Then**
   Candidate, quyết định, Result và ranking vẫn truy được về đúng bộ tham số đó.

---

### User Story 3 - Giải thích và tái lập kết quả Sentiment Strategy (Priority: P2)

Người nghiên cứu mở một Result và xem được Sentiment score, ngưỡng, số News đủ điều kiện, model
release cùng bằng chứng nguồn của từng quyết định quan trọng. Khi reproduce, hệ thống tạo run mới và
so sánh với kết quả gốc mà không ghi đè artifact đã chấp nhận.

**Why this priority**: Nếu không khóa provenance theo thời gian và model version, Backtest dùng News
rất dễ nhìn thấy tương lai hoặc thay đổi kết quả khi model được nâng cấp.

**Independent Test**: Chọn một Result có Sentiment Strategy, lần theo toàn bộ evidence chain rồi
reproduce bằng snapshot cũ sau khi active model đã đổi; kết quả phải có verdict rõ và artifact gốc
giữ nguyên.

**Acceptance Scenarios**:

1. **Given** một Result đã chấp nhận, **When** người dùng mở evidence, **Then** họ xác định được
   sentiment snapshot, model/preprocessing version, News/Sentiment Result nguồn, tham số Strategy và
   thời điểm đánh giá.
2. **Given** active model release đã thay đổi, **When** reproduce Result cũ, **Then** reproduction vẫn
   dùng release và snapshot đã đóng băng của run gốc.
3. **Given** bất kỳ News, model version, score hoặc ordered evidence nào không khớp, **When** so sánh
   reproduction, **Then** verdict là MISMATCHED và chỉ rõ lớp evidence lệch.

---

### User Story 4 - Giữ luồng kỹ thuật hoạt động khi Sentiment gián đoạn (Priority: P3)

Người nghiên cứu vẫn xem Market, News đã lưu và chạy technical Strategy khi việc thu thập hoặc phân
tích Sentiment mới tạm gián đoạn. Sentiment Strategy chỉ chạy khi có snapshot hợp lệ; nó không tự
thay thế dữ liệu thiếu bằng NEUTRAL.

**Why this priority**: Feature nâng cao không được làm giảm failure isolation đã có của hệ thống.

**Independent Test**: Dừng nguồn Sentiment mới, xác nhận technical Backtest vẫn hoàn tất; một
Sentiment Backtest có snapshot hợp lệ vẫn tái lập được, còn request thiếu snapshot nhận trạng thái
an toàn và có hướng xử lý.

**Acceptance Scenarios**:

1. **Given** Sentiment service không sẵn sàng, **When** người dùng chạy technical Strategy, **Then**
   Backtest không bị gián đoạn bởi dependency này.
2. **Given** một frozen sentiment snapshot hợp lệ đã tồn tại, **When** service đang gián đoạn và người
   dùng reproduce, **Then** run không cần gọi lại model để thay đổi evidence lịch sử.
3. **Given** không thể tạo snapshot hợp lệ cho run mới, **When** người dùng chọn Sentiment Strategy,
   **Then** hệ thống từ chối bắt đầu bằng trạng thái dễ hiểu và không tạo Result một phần.

### Edge Cases

- Không có News liên quan đúng tài sản hoặc không đủ số bài trong lookback.
- Tổng confidence của các quan sát bằng zero.
- News trùng identity, cùng timestamp hoặc được liên kết với nhiều tài sản.
- Sentiment Result có model version khác release đã chọn.
- News được phát hành đúng tại evaluation time hoặc ngay sau evaluation time.
- Ngưỡng mua bằng/thấp hơn ngưỡng bán, ngưỡng vượt miền score, lookback hoặc số bài tối thiểu không
  dương.
- Snapshot thiếu News/Sentiment Result nguồn, checksum/fingerprint không khớp hoặc chứa identity lặp.
- Active model đổi sau khi Experiment bắt đầu.
- Composite nhận tín hiệu xung đột giữa Sentiment và Strategy kỹ thuật.
- Dữ liệu Sentiment quá lớn trong một lookback; kết quả phải vẫn hữu hạn và có thứ tự xác định.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST cung cấp Sentiment Strategy như một Strategy hệ thống có identity, version,
  mô tả, category và parameter schema duy nhất trong catalog hiện hữu.
- **FR-002**: Người dùng MUST cấu hình được lookback, số bài tối thiểu, ngưỡng BUY và ngưỡng SELL;
  mọi cấu hình MUST được kiểm tra trước khi publish, Search hoặc Backtest.
- **FR-003**: Ngưỡng BUY MUST lớn hơn zero, ngưỡng SELL MUST nhỏ hơn zero, ngưỡng SELL MUST nhỏ hơn
  ngưỡng BUY, lookback và số bài tối thiểu MUST dương.
- **FR-004**: Mỗi run dùng Sentiment Strategy MUST tham chiếu một sentiment snapshot bất biến và một
  model release cụ thể trước khi tạo Candidate hoặc bắt đầu Backtest.
- **FR-005**: Snapshot MUST chỉ chứa Sentiment Result hợp lệ, liên quan tới tài sản được đánh giá, có
  model version đúng và có News `publishedAt` không sau thời điểm cuối của Dataset.
- **FR-006**: Tại mỗi evaluation time, Strategy MUST chỉ dùng quan sát có `publishedAt` không sau thời
  điểm đó và nằm trong lookback đã cấu hình; `analyzedAt` chỉ dùng để audit.
- **FR-007**: Một News identity MUST đóng góp nhiều nhất một lần vào một lần đánh giá, kể cả khi nó có
  nhiều quan hệ hoặc được đọc lại.
- **FR-008**: Aggregate score MUST là trung bình polarity có trọng số confidence của các quan sát đủ
  điều kiện, sử dụng quy tắc số thập phân và làm tròn có version.
- **FR-009**: Strategy MUST trả BUY khi aggregate score bằng hoặc lớn hơn ngưỡng BUY, SELL khi score
  bằng hoặc nhỏ hơn ngưỡng SELL và HOLD khi score nằm giữa hai ngưỡng.
- **FR-010**: Strategy MUST trả HOLD cùng reason code khi số quan sát thấp hơn minimumArticles hoặc
  tổng trọng số bằng zero; hệ thống MUST NOT tự tạo quan sát NEUTRAL thay dữ liệu thiếu.
- **FR-011**: Mỗi quyết định MUST chứa aggregate score, eligible article count, lookback, thresholds,
  model version và fingerprint của ordered Sentiment evidence đã dùng.
- **FR-012**: Cùng Strategy version, parameters, Candle sequence, sentiment snapshot và evaluation
  time MUST luôn tạo cùng tín hiệu, evidence và fingerprint.
- **FR-013**: Sentiment Strategy MUST chỉ đọc input đã được cấp trong evaluation context; việc đánh
  giá MUST không tự gọi nguồn News, model, clock hoặc mutable external state.
- **FR-014**: Search MUST hỗ trợ các parameter domain hợp lệ của Sentiment Strategy, giữ snapshot/model
  cố định giữa các Candidate và áp dụng stop condition hữu hạn hiện hữu.
- **FR-015**: Sentiment Strategy MUST có thể tham gia Single hoặc Composite Strategy thông qua cùng
  validation, publication và combination policy đã phát hành.
- **FR-016**: Backtest MUST áp dụng timing/execution policy hiện hữu; SELL trong chế độ long-only chỉ
  đóng vị thế LONG và không được trình bày là giao dịch SHORT.
- **FR-017**: Result MUST truy được về Dataset, Strategy/Composite version, parameters, sentiment
  snapshot, model/preprocessing version và ordered Sentiment Result evidence.
- **FR-018**: Reproduction MUST tạo run mới, sử dụng snapshot/model release của run nguồn, so sánh
  evidence/decision/result và không ghi đè artifact gốc.
- **FR-019**: Thay active model MUST không sửa hoặc làm mất khả năng đọc và reproduce Result lịch sử.
- **FR-020**: Giao diện MUST dùng các read/command boundary đã cấp quyền và MUST không tự tính score,
  tín hiệu, Backtest hoặc Composite outcome.
- **FR-021**: Trang News MUST có hành động đưa Sentiment Strategy authoritative vào Strategy Composer
  khi capability sẵn sàng; trạng thái loading, empty, invalid, inaccessible và degraded MUST có text
  rõ ràng, không chỉ dùng màu.
- **FR-022**: Strategy Catalog và Result evidence MUST trình bày nguồn Sentiment là tín hiệu nghiên cứu,
  không phải cam kết lợi nhuận hoặc lời khuyên tài chính.
- **FR-023**: Khi không thể tạo sentiment snapshot cho run mới, hệ thống MUST fail trước execution,
  trả lỗi an toàn và không lưu partial Result/Trade/Evaluation.
- **FR-024**: Sentiment failure MUST không chặn Market Data, technical Strategy, technical Backtest hoặc
  quyền đọc News/Sentiment Result đã được lưu.
- **FR-025**: Dữ liệu fixture hoặc prototype MUST được gắn nhãn rõ và MUST không được dùng để tuyên bố
  Sentiment Strategy live hoặc ML nâng cao đã được kiểm chứng.

### Key Entities

- **Sentiment Strategy Definition**: Strategy hệ thống có version và schema cho lookback,
  minimumArticles, buyThreshold và sellThreshold.
- **Sentiment Observation**: Bằng chứng bất biến liên kết một News đã công bố với Sentiment Result,
  asset, polarity, confidence, model release và các timestamp cần audit.
- **Sentiment Snapshot**: Tập ordered Sentiment Observation được đóng băng cho một Experiment, có
  identity, khoảng thời gian, model/preprocessing version, count và fingerprint.
- **Sentiment Aggregate**: Score và count tại một evaluation time, kèm fingerprint của tập evidence
  đủ điều kiện.
- **Sentiment Strategy Decision**: BUY/SELL/HOLD cùng reason, thresholds và aggregate evidence.
- **Experiment Sentiment Provenance**: Liên kết Experiment/Result/Reproduction tới snapshot và model
  release đã dùng.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Catalog authenticated hiển thị đủ năm Strategy hệ thống, trong đó Sentiment Strategy đi
  qua cùng validation/publication gate như bốn Strategy kỹ thuật.
- **SC-002**: Với bộ dữ liệu acceptance cố định, 100% quyết định BUY/HOLD/SELL khớp aggregate score và
  boundary threshold đã công bố.
- **SC-003**: 100% News có `publishedAt` sau evaluation time bị loại khỏi score và evidence trong kiểm
  thử chống look-ahead.
- **SC-004**: Mười lần chạy lại cùng frozen inputs tạo tín hiệu, evidence fingerprint, Trade sequence
  và Result fingerprint giống nhau 100%.
- **SC-005**: Một người dùng có thể chọn Sentiment Strategy, cấu hình, chạy Backtest và mở Result có
  provenance trong tối đa 10 phút của demo.
- **SC-006**: 100% Result được chọn trong demo truy được tới sentiment snapshot, model/preprocessing
  version và mọi Sentiment Result nguồn dùng cho ít nhất một quyết định giao dịch.
- **SC-007**: Search demo với Sentiment Strategy dừng đúng candidate/time limit, không tạo Candidate
  trùng và giữ cùng snapshot/model release cho 100% Candidate.
- **SC-008**: Khi Sentiment service dừng, technical Backtest vẫn hoàn tất và reproduction có snapshot
  hợp lệ vẫn cho verdict; không có accepted Result bị mất hoặc ghi đè.
- **SC-009**: Giao diện hoạt động bằng bàn phím và không tràn toàn trang tại 360px, 768px, 1024px và
  1440px; status/evidence không phụ thuộc riêng vào màu.
- **SC-010**: Demo live tạo ít nhất một BUY hoặc SELL từ Sentiment Strategy và trình bày được score,
  thresholds, article count cùng model version mà không lộ credential hoặc dữ liệu cá nhân.

## Assumptions

- Release đầu tiên dùng aggregate polarity có trọng số confidence; thay đổi công thức hoặc rounding
  tạo version semantics mới.
- Giá trị mặc định đề xuất là lookback 24 giờ, minimumArticles 3, buyThreshold `0.25` và
  sellThreshold `-0.25`; người dùng có thể chọn giá trị hợp lệ khác.
- Sentiment Result và News liên quan tài sản đã có từ capability News/Sentiment; F-016 không huấn luyện
  hoặc tự đánh giá chất lượng model.
- Chỉ model release được đóng băng trong snapshot mới được dùng; active release hiện tại không tự
  thay đổi Experiment đã bắt đầu.
- Strategy tuân theo chế độ Backtest long-only và next-open hiện hữu.
- Feature mở rộng các màn hình Strategy, News, Search và Result hiện hữu; không tạo application shell,
  authentication hoặc route cạnh tranh.
- Quyết định ownership và contract xuyên News, Strategy, Experiment/Backtest cần được chốt bằng ADR
  được chấp nhận trước khi implementation phụ thuộc được merge.

## Out of Scope

- Huấn luyện model, tạo tập nhãn hoặc báo cáo Accuracy/Precision/Recall/F1; đây là workstream ML
  evaluation riêng.
- Giao dịch tiền thật, mở vị thế SHORT, quản lý ví hoặc cung cấp lời khuyên đầu tư.
- Cho Strategy gọi trực tiếp provider, database hoặc Sentiment service trong lúc evaluate.
- Đồng thời phục vụ nhiều model release trong một process hoặc tự động chọn model tốt nhất.
- Dùng LLM để sinh Strategy, Genetic/Bayesian Search hoặc thêm sàn giao dịch mới.
