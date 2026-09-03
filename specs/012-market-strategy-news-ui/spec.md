# Feature Specification: Market, Strategy and News UI

**Feature Branch**: `012-market-strategy-news-ui`

**Created**: 2026-09-03

**Status**: Draft

**Input**: User description: "Triển khai F-012 Market, Strategy and News UI trên Web Foundation F-011 và public contracts F-009."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Theo dõi thị trường (Priority: P1)

Là người dùng đã đăng nhập, tôi muốn chọn cặp giao dịch và tối đa bốn khung thời gian để xem Candle lịch sử,
nhận cập nhật mới và biết trạng thái kết nối, từ đó có thể quan sát thị trường trước khi chọn chiến
lược hoặc đọc tin liên quan.

**Why this priority**: Market Dashboard là điểm vào chính của sản phẩm và cung cấp bối cảnh chung
cho Strategy, News và các feature Experiment sau này.

**Independent Test**: Mở Market Dashboard với một pair và bốn timeframe hợp lệ, tải Candle lịch sử, nhận
một cập nhật realtime rồi mô phỏng mất kết nối; biểu đồ và trạng thái phải phản ánh đúng từng bước
mà không cần Strategy hoặc News hoạt động.

**Acceptance Scenarios**:

1. **Given** người dùng vừa mở Market Dashboard, **When** pair và các timeframe mặc định hợp lệ được chọn,
   **Then** giao diện hiển thị loading rõ ràng rồi render tối đa bốn panel Candle theo đúng thứ tự thời gian.
2. **Given** lịch sử đã tải, **When** Candle realtime mới đến, **Then** Candle cùng kỳ được cập nhật
   hoặc Candle kỳ mới được nối vào mà không tạo timestamp trùng.
3. **Given** kết nối realtime bị gián đoạn, **When** hệ thống reconnect hoặc yêu cầu snapshot mới,
   **Then** người dùng thấy trạng thái trung thực và dữ liệu lịch sử đang có vẫn đọc được.
4. **Given** người dùng đổi pair hoặc timeframe nhanh, **When** response cũ về sau response mới,
   **Then** lựa chọn mới nhất vẫn là authority và dữ liệu cũ không ghi đè màn hình.

---

### User Story 2 - Khám phá và quản lý Strategy (Priority: P1)

Là người dùng đã đăng nhập, tôi muốn xem Strategy hệ thống và thư viện riêng, hiểu version cùng
parameters, đồng thời tạo, tạo version mới, publish hoặc archive Strategy riêng khi được phép.

**Why this priority**: Strategy là input nghiệp vụ cốt lõi cho Backtest/Experiment và F-009 đã có
public ownership boundary để F-012 cung cấp trải nghiệm quản lý an toàn.

**Independent Test**: Dùng một tài khoản có Strategy riêng, xem catalog, tạo Strategy hợp lệ, tạo
version tiếp theo, publish và archive; xác nhận dữ liệu của user khác không xuất hiện và immutable
version cũ không bị thay đổi.

**Acceptance Scenarios**:

1. **Given** catalog có Strategy hệ thống và private Strategy của owner, **When** mở trang Strategy,
   **Then** hai nhóm được phân biệt, có trạng thái, version và mô tả đủ để chọn xem chi tiết.
2. **Given** parameter schema của Strategy, **When** nhập thiếu, sai kiểu hoặc vi phạm constraint,
   **Then** lỗi gắn với field phù hợp và không gửi thay đổi không hợp lệ.
3. **Given** draft hợp lệ, **When** owner tạo hoặc tạo version mới, **Then** giao diện hiển thị
   authoritative response và version trước vẫn bất biến.
4. **Given** owner publish/archive hoặc retry thao tác, **When** server trả success, conflict hay
   inaccessible, **Then** UI reconcile lại resource và không giả định thành công từ optimistic state.

---

### User Story 3 - Đọc News và Sentiment trung thực (Priority: P2)

Là người dùng đã đăng nhập, tôi muốn đọc News và xem trạng thái phân tích
Sentiment để bổ sung bối cảnh, nhưng vẫn đọc được tin khi dịch vụ phân tích bị chậm hoặc lỗi.

**Why this priority**: News tăng giá trị quan sát nhưng không được làm gián đoạn Market hoặc đánh
đồng kết quả mô hình với lời khuyên tài chính.

**Independent Test**: Tải danh sách News, lọc theo analysis status, mở một item có sentiment hoàn tất rồi mô
phỏng trạng thái pending/failed; nội dung tin vẫn dùng được và UI mô tả đúng mức độ sẵn sàng.

**Acceptance Scenarios**:

1. **Given** có nhiều News item, **When** người dùng lọc hoặc tải trang tiếp theo,
   **Then** kết quả giữ thứ tự ổn định, không lặp item và phản ánh đúng filter hiện hành.
2. **Given** sentiment đã hoàn tất, **When** item được hiển thị, **Then** label/confidence/polarity
   công khai được trình bày rõ và không diễn đạt thành khuyến nghị mua bán.
3. **Given** sentiment pending, unavailable hoặc failed, **When** News tải thành công,
   **Then** nội dung News vẫn hiển thị với degraded state đúng nguyên nhân và hướng retry phù hợp.

---

### User Story 4 - Trải nghiệm nhất quán và có thể phục hồi (Priority: P2)

Là người dùng trên desktop hoặc mobile, tôi muốn các màn hình Market, Strategy và News có navigation,
loading, empty, error, retry và session behavior nhất quán để không mất ngữ cảnh khi mạng chập chờn.

**Why this priority**: Ba màn hình dùng chung foundation; hành vi không nhất quán sẽ gây sai hiểu
về dữ liệu authoritative và tạo chi phí bảo trì cho các feature Web tiếp theo.

**Independent Test**: Chạy ba route ở viewport 360px và 1440px, inject loading/empty/4xx/5xx,
session expiry và reconnect; mọi trạng thái vẫn thao tác được bằng bàn phím và không lộ chi tiết nội bộ.

**Acceptance Scenarios**:

1. **Given** request đang tải, rỗng hoặc lỗi, **When** user chuyển giữa ba route,
   **Then** shared state pattern giữ layout ổn định và cung cấp hành động tiếp theo phù hợp.
2. **Given** session hết hạn, **When** REST hoặc realtime báo unauthorized,
   **Then** foundation xử lý theo auth lifecycle chung, không tạo vòng retry hoặc client cạnh tranh.
3. **Given** viewport nhỏ hoặc chỉ dùng bàn phím, **When** user thực hiện primary journeys,
   **Then** nội dung chính, controls, focus và status vẫn truy cập được.

### Edge Cases

- Candle response rỗng, có khoảng trống, trùng open time hoặc realtime đến trước snapshot.
- Pair/timeframe trong URL không còn được hỗ trợ hoặc user đổi filter liên tục.
- Realtime reconnect nhiều lần, message duplicate/out-of-order hoặc snapshot revision cũ.
- Strategy catalog rỗng; Strategy/version bị archive giữa lúc user đang xem hoặc chỉnh draft.
- Parameter schema có enum dài, decimal boundary, cross-field constraint hoặc field không còn hỗ trợ.
- Create/publish/archive timeout sau khi server có thể đã commit; UI không tự retry mutation.
- News item trùng identity, thiếu source hoặc không còn ở trang hiện tại.
- Sentiment pending lâu, failed hoặc unavailable; UI không suy diễn provenance hay aggregate analytics.
- REST thành công nhưng realtime chậm; realtime đến khi resource tương ứng chưa có trong local view.
- Mất mạng, session hết hạn hoặc quyền ownership thay đổi trong lúc đang thao tác.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: F-012 MUST dùng application shell, auth/session lifecycle, navigation và public client
  contracts do F-011 cung cấp; MAY mở rộng `RealtimeClient` tương thích ngược bằng event/status
  observers, nhưng MUST NOT tạo auth, REST hoặc realtime singleton cạnh tranh.
- **FR-002**: Mọi business data MUST đi qua owner-authorized public F-009 boundary; browser MUST
  NOT gọi trực tiếp business table, Binance hoặc Sentiment Service nội bộ.
- **FR-003**: Market Dashboard MUST cho phép chọn một supported trading pair và từ một đến bốn
  timeframe panel độc lập, đồng thời giữ lựa chọn hiện hành rõ ràng qua mọi async state.
- **FR-004**: Market Dashboard MUST hiển thị Candle lịch sử theo UTC và thứ tự xác định, bảo toàn
  exact price/volume text semantics từ public contract.
- **FR-005**: Realtime Candle update của mỗi panel MUST merge theo canonical Candle identity, không
  tạo bản ghi trùng và không cho message stale/out-of-order làm state quay lùi.
- **FR-006**: UI MUST phân biệt transport state (`connecting`, `connected`, `reconnecting`,
  `disconnected`) với Market provider state nhận từ public event; presentation `live/unavailable`
  MUST được dẫn xuất trung thực và disconnect không được xóa snapshot đã tải thành công.
- **FR-007**: Khi pair/timeframe đổi, late response hoặc event của selection cũ MUST bị bỏ qua.
- **FR-008**: Strategy screen MUST phân biệt system catalog và owner-scoped private library, với
  trạng thái empty/loading/error độc lập khi một nguồn chưa sẵn sàng.
- **FR-009**: Strategy detail MUST hiển thị identity, version, status, parameter schema/values và
  provenance public cần thiết mà không expose implementation nội bộ.
- **FR-010**: Owner MUST có thể tạo private Strategy và version mới bằng form sinh từ supported
  parameter schema, với validation theo field và cross-field trước submit khi contract cho phép.
- **FR-011**: Owner MUST có thể publish và archive qua explicit confirmation; UI MUST reconcile
  authoritative response sau success, retry, conflict hoặc timeout.
- **FR-012**: Immutable published Strategy version MUST không được trình bày như có thể sửa tại chỗ;
  thay đổi MUST tạo version mới theo public workflow.
- **FR-013**: Resource missing và foreign-owner inaccessible MUST dùng cùng safe user-facing state,
  không giúp suy đoán sự tồn tại của private Strategy.
- **FR-014**: News screen MUST hỗ trợ danh sách có pagination/cursor và filter `analysisStatus`, với
  stable ordering và deduplication theo News identity; MUST NOT gửi pair filter khi chưa có public
  mapping từ canonical pair sang opaque `tradingPairId`.
- **FR-015**: Mỗi News item MUST chỉ hiển thị các field public: title, source, URL an toàn, publish
  time, related asset IDs và analysis status; không giả lập content hoặc summary.
- **FR-016**: Sentiment hoàn tất MUST hiển thị label/confidence/polarity public; pending, failed
  hoặc unavailable MUST hiển thị degraded state mà không chặn nội dung News.
- **FR-017**: Sentiment và market information MUST được mô tả là dữ liệu tham khảo, không phải cam
  kết lợi nhuận, lời khuyên tài chính hoặc tín hiệu đặt lệnh.
- **FR-018**: Mọi route MUST có loading, empty, validation, inaccessible, retryable failure và
  non-retryable failure state phù hợp, không lộ secret, token, stack, SQL hoặc provider payload.
- **FR-019**: Retry MUST bounded theo hành động user hoặc policy F-011; UI MUST không tạo request,
  subscription hoặc mutation loop vô hạn.
- **FR-020**: REST snapshot là authoritative; realtime chỉ cập nhật notification/state tạm thời và
  MUST trigger reconciliation khi phát hiện gap, reconnect hoặc revision không liên tục.
- **FR-021**: Navigation/filter state hữu ích MUST có thể khôi phục khi reload/back-forward mà không
  ghi token hoặc private payload vào URL.
- **FR-022**: Ba route MUST usable ở viewport 360px và 1440px, hỗ trợ keyboard navigation, visible
  focus, semantic labels và status announcement cho cập nhật bất đồng bộ quan trọng.
- **FR-023**: Production behavior MUST không dùng mock/fixture làm business truth; fixture chỉ được
  phép trong test hoặc explicit non-production development mode của F-011.
- **FR-024**: F-012 MUST không triển khai Experiment/Result/Leaderboard UI, Search coordination,
  public transport mới, database migration hoặc provider integration mới.

### Key Entities

- **Market Selection**: Pair và một đến bốn timeframe panel, cùng URL/navigation có thể khôi phục.
- **Candle View**: Biểu diễn Candle ordered theo UTC identity và trạng thái snapshot/realtime merge.
- **Transport Connection State / Market Provider State**: Hai nguồn trạng thái riêng, lần cập nhật
  thành công và presentation phục hồi được dẫn xuất.
- **Strategy Summary/Version**: System hoặc private Strategy, immutable version, status, parameters
  và provenance được phép hiển thị.
- **Strategy Draft**: Dữ liệu form chưa authoritative, validation issues và mutation state.
- **News View**: News identity, title, source, URL, publish time, linked assets và pagination.
- **Sentiment View**: Analysis status, label/confidence/polarity public hoặc degraded reason an toàn.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 95% lần mở mỗi route với fixture chuẩn hiển thị usable primary content trong dưới 2 giây
  sau khi authorized response sẵn sàng.
- **SC-002**: Qua 100 duplicate/out-of-order Candle events, UI tạo đúng số canonical Candle và không
  để selection hoặc timestamp mới nhất quay lùi.
- **SC-003**: 100% supported Strategy parameter fixtures hiển thị đúng control/validation và không
  gửi mutation khi còn lỗi đã biết.
- **SC-004**: Trong 100 lần rapid-click khi mutation đang pending, UI gửi đúng một request; timeout
  không được hiển thị là success, không auto-retry, và retry do user khởi tạo luôn kết thúc bằng
  authoritative reload thay vì hứa server-side idempotency không có trong contract.
- **SC-005**: Với sentiment unavailable trong toàn bộ phiên test, 100% News item còn đọc được và
  Market/Strategy journeys không suy giảm chức năng.
- **SC-006**: Tất cả loading, empty, inaccessible, retryable và non-retryable fixtures cho ba route
  hiển thị đúng safe action, không có internal detail trong nội dung user nhìn thấy.
- **SC-007**: Primary journeys Market, Strategy và News hoàn thành bằng keyboard tại viewport 360px
  và 1440px mà không có critical accessibility violation tự động phát hiện.
- **SC-008**: Sau disconnect/reconnect hoặc event gap, 100% test scenario reconcile về cùng state
  như một fresh authorized snapshot.
- **SC-009**: Production build chứa zero privileged credential, direct business-table/provider call
  và mock business outcome.
- **SC-010**: F-012 không sửa auth/session, application shell hoặc business API F-009; thay đổi
  F-011 realtime chỉ là observer extension tương thích ngược, giữ nguyên toàn bộ method hiện có.

## Assumptions

- F-011 Web Foundation/Auth và các public client interfaces đã được merge; F-012 reuse chúng.
- F-009 cung cấp Market, Strategy và News read/mutation contracts hiện có; unsupported operation
  được hiển thị trung thực thay vì giả lập ở client.
- Candle chart MVP ưu tiên OHLCV readability, responsive interaction và deterministic updates;
  advanced drawing indicators, annotations và trading controls ngoài phạm vi.
- Pair/timeframe options đến từ versioned frontend Market catalog đã kiểm tra parity với released
  contract/docs; UI không tự phát minh provider symbol.
- News MVP không có pair filter cho tới khi public API cung cấp catalog/mapping `tradingPairId`.
- Aggregate sentiment, trend, topic và Strategy integration trong prototype là ngoài contract và
  không được suy diễn từ News page hiện tại.
- Time được nhận dưới dạng UTC instant và có thể hiển thị theo timezone người dùng với nhãn rõ ràng.
- News/Sentiment chỉ là thông tin tham khảo; F-012 không đặt lệnh, quản lý ví hoặc đưa lời khuyên.
- F-010 Search Coordinator và F-013 Experiment UI có thể phát triển song song, không chặn ba journey
  độc lập của F-012.
