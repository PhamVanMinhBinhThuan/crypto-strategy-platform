# Feature Specification: Backtest, Evaluation and Leaderboard

**Feature Branch**: `feature/006-backtest-evaluation-leaderboard`

**Created**: 2026-08-31

**Status**: Implemented — Pending Review

**Input**: F-006 cung cấp Backtest xác định, Evaluation có phiên bản, Leaderboard Top-K và khả năng tái tạo kết quả từ dữ liệu cùng cấu hình đã đóng băng.

## Clarifications

### Session 2026-08-31

- Q: MVP nên áp dụng vốn, phí giao dịch và slippage cho mỗi Trade như thế nào? → A: Dùng 100% số dư khả dụng; BUY price = next open × (1 + slippage), SELL price = next open × (1 − slippage); fee = order notional × feeRate ở cả hai chiều.
- Q: Các metrics nên được chuẩn hóa và áp dụng ngưỡng số Trade tối thiểu như thế nào trước khi xếp hạng? → A: `returnScore = clamp(TotalReturn, 0, 1)`, `winRateScore = clamp(WinRate, 0, 1)`, `drawdownScore = 1 - clamp(MaxDrawdown, 0, 1)`; dưới 5 Trades vẫn lưu Evaluation nhưng không đủ điều kiện vào Leaderboard.
- Q: Khi phép tính tạo nhiều chữ số thập phân, hệ thống phải làm tròn theo quy tắc nào? → A: Price, quantity, money, fee và P&L dùng scale 12; rate, metric và score dùng scale 10; rounding `HALF_EVEN`; giữ độ chính xác trung gian tối đa.
- Q: Fingerprint và tiêu chí reproduction nên được tổ chức như thế nào để xác định chính xác phần nào bị lệch? → A: Dùng fingerprint phân tầng có version: `backtest-v1` cho frozen provenance, assumptions, ordered Trades và Result; `evaluation-v1` cho Result fingerprint, metric version, bốn metrics và score; `leaderboard-v1` cho Experiment, ranking version và ordered entries; reproduction chỉ thành công khi Trades, metrics cùng fingerprints liên quan đều khớp.
- Q: Backtest phải xử lý Dataset hoặc CandleBatch không hợp lệ như thế nào? → A: Fail fast khi Dataset rỗng, sequence thiếu/trùng, Candle sai thứ tự/trùng identity hoặc checksum sai; trả error code cụ thể và không lưu partial business outcome.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Backtest chiến lược trên dữ liệu lịch sử (Priority: P1)

Người nghiên cứu chạy một Candidate thuộc Experiment trên Dataset đóng băng để nhận chuỗi giao dịch và kết quả có thể giải thích. Dataset được xử lý theo lô, đúng thứ tự và không phải tải toàn bộ vào bộ nhớ.

**Why this priority**: Evaluation và Leaderboard chỉ có giá trị khi Backtest đúng và xác định.

**Independent Test**: Dùng Dataset fixture gồm nhiều batch, Strategy xác định và successful Execution Attempt; xác nhận Trade, vốn cuối và fingerprint giống nhau qua nhiều lần chạy.

**Acceptance Scenarios**:

1. **Given** Dataset, Strategy provenance, Manifest và successful Attempt hợp lệ, **When** Backtest hoàn tất, **Then** hệ thống tạo đúng một Backtest Result cùng Trade bất biến, liên kết đúng Experiment, Candidate và Attempt.
2. **Given** cùng frozen inputs và versions, **When** chạy lại, **Then** Trade sequence, kết quả và fingerprint giống lần gốc.
3. **Given** Dataset lớn gồm nhiều batch liên tục, **When** Backtest chạy, **Then** mỗi Candle được xử lý đúng một lần mà không materialize toàn Dataset.
4. **Given** Attempt chưa thành công hoặc thuộc Candidate/Experiment khác, **When** ghi Result, **Then** hệ thống từ chối và không tạo dữ liệu bền vững.

---

### User Story 2 - Đánh giá bằng metrics có phiên bản (Priority: P2)

Người nghiên cứu nhận bốn metrics bắt buộc và overall score có thể truy ngược về phiên bản công thức.

**Why this priority**: Metrics nhất quán giúp so sánh Candidate công bằng và giải thích được.

**Independent Test**: Đưa Trade và `EquityCurveSummary` đã biết vào Evaluator rồi so sánh Total Return, Win Rate, Maximum Drawdown, Number of Trades và score với kết quả kỳ vọng.

**Acceptance Scenarios**:

1. **Given** Backtest Result hợp lệ, **When** Evaluation chạy, **Then** tạo đủ bốn metrics bằng exact decimal semantics.
2. **Given** cùng Result và versions, **When** Evaluation lặp lại, **Then** metrics, score và fingerprint giống nhau.
3. **Given** Evaluation đã hoàn tất, **When** sửa metrics hoặc version, **Then** hệ thống từ chối sửa bản gốc.

---

### User Story 3 - Xem Leaderboard Top-K nhất quán (Priority: P3)

Người nghiên cứu xem Top-K Candidate của một Experiment tại một revision cụ thể và truy được mỗi entry về Evaluation nguồn.

**Why this priority**: Leaderboard chỉ đáng tin khi không trộn Experiment và thứ tự luôn ổn định.

**Independent Test**: Tạo projection nhiều lần từ cùng Evaluation set và xác nhận cùng thành viên, rank, revision fingerprint cùng provenance.

**Acceptance Scenarios**:

1. **Given** Evaluation Results thuộc cùng Experiment, **When** tạo revision, **Then** giữ tối đa K entry và rank liên tục từ 1.
2. **Given** nhiều Result bằng điểm, **When** tạo lại revision, **Then** thứ tự luôn giống nhau.
3. **Given** Result thuộc Experiment khác, **When** thêm vào revision, **Then** hệ thống từ chối.
4. **Given** revision đã công bố, **When** có kết quả mới, **Then** tạo revision mới thay vì sửa revision cũ.

---

### User Story 4 - Tái tạo kết quả (Priority: P4)

Người nghiên cứu tái tạo kết quả đã lưu từ frozen Dataset, Strategy, Manifest và các versions để kiểm chứng provenance.

**Why this priority**: Reproduction chứng minh kết quả không phụ thuộc dữ liệu hiện tại hoặc default ẩn.

**Independent Test**: Lưu và đọc lại provenance, chạy reproduction rồi xác nhận Dataset checksum, Trade sequence, bốn metrics cùng fingerprint khớp bản gốc.

**Acceptance Scenarios**:

1. **Given** frozen provenance đầy đủ, **When** reproduction hoàn tất, **Then** tạo run mới liên kết bản gốc và xác nhận Trades, metrics cùng fingerprint khớp.
2. **Given** checksum, input hoặc version không khớp, **When** reproduction chạy, **Then** không xác nhận thành công và không sửa evidence gốc.

### Edge Cases

- Dataset rỗng hoặc khai báo `candleCount < 1` vi phạm contract F-003 và phải bị từ chối trước khi Backtest.
- Batch sequence âm, không bắt đầu từ 0, đứt đoạn, lặp hoặc có `hasMore`/`nextSequence` sai phải bị từ chối.
- Candle trùng identity, sai thời gian, chưa đóng hoặc ngoài Dataset membership phải làm integrity check thất bại.
- Dataset checksum/count khác provenance phải ngăn Result hoặc reproduction được xác nhận.
- Strategy không resolve đúng version hoặc trả output không hợp lệ không được tạo successful outcome.
- BUY khi đã có position, SELL khi chưa có position và position còn mở cuối Dataset tuân theo execution policy đã chốt.
- Fee, slippage, capital hoặc assumptions không hợp lệ phải bị từ chối.
- Zero trades hoặc zero winning trades phải tạo metrics hữu hạn; `initialCapital <= 0` là input không hợp lệ và phải fail validation trước khi Evaluation hoặc dữ liệu bền vững được tạo.
- K nhỏ hơn 1, Evaluation/Candidate trùng hoặc liên kết chéo Experiment phải bị từ chối.
- Ghi lặp cùng outcome không được tạo Trade, Result, Evaluation hoặc Entry trùng.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST chỉ chạy Backtest từ Dataset, Strategy provenance, Experiment Manifest và Candidate đã đóng băng, hợp lệ.
- **FR-002**: Hệ thống MUST tiêu thụ Candle batches có sequence từ 0, liên tục và không thiếu hoặc trùng Candle.
- **FR-003**: Hệ thống MUST giữ lượng Candle trong bộ nhớ bị giới hạn bởi batch size cộng rolling lookback và state giao dịch cần thiết, thay vì tải toàn Dataset; verification MUST đo số Candle tối đa được giữ trên một Dataset fixture lớn nhiều batch.
- **FR-004**: Hệ thống MUST kiểm tra Dataset membership, Candle ordering, closed state, count và checksum trước khi chấp nhận Result.
- **FR-004a**: Dataset rỗng, batch sequence thiếu hoặc trùng, Candle sai thứ tự hoặc trùng identity, và checksum/count không khớp MUST làm Backtest fail fast với error code cụ thể; hệ thống MUST không lưu partial Trade, Result, Evaluation hoặc Leaderboard outcome, còn caller chịu trách nhiệm ánh xạ lỗi sang Attempt handling.
- **FR-005**: Hệ thống MUST resolve đúng Strategy/Composite version và parameters trong provenance, không đọc default mới hơn.
- **FR-006**: Backtest MUST chỉ cho phép một LONG position tại một thời điểm; signal của Candle hiện tại được khớp tại giá open của Candle kế tiếp; BUY khi đã có position và SELL khi chưa có position bị bỏ qua; position còn mở được đóng tại giá close của Candle cuối.
- **FR-007**: Backtest assumptions MUST có version và gồm tối thiểu initial capital, fee rate, slippage rate, execution price rule và position mode. Với BUY, `buyFillPrice = nextOpen × (1 + slippage)` và `quantity = availableCash / (buyFillPrice × (1 + feeRate))`; entry fee bằng entry notional nhân fee rate, nên cash không được âm ngoài sai số canonical. Với SELL, `sellFillPrice = nextOpen × (1 - slippage)` và exit fee bằng exit notional nhân fee rate. Forced close dùng final Candle close với adverse SELL slippage và exit fee giống SELL thông thường. Mọi phép tính tuân theo FR-008.
- **FR-008**: Price, quantity, capital, money, fee và P&L MUST dùng scale 12; rate, return, drawdown, metric và score MUST dùng scale 10; mọi phép làm tròn MUST dùng `HALF_EVEN`, giữ độ chính xác trung gian tối đa và chỉ làm tròn tại canonical output hoặc phép chia bắt buộc.
- **FR-009**: Cùng frozen input và versions MUST tạo cùng Trade sequence, Result và fingerprint.
- **FR-010**: Result MUST liên kết đúng Experiment, Candidate và successful Execution Attempt; liên kết chéo hoặc Attempt chưa thành công MUST bị từ chối.
- **FR-011**: Ghi lặp MUST NOT tạo outcome trùng; FailureClassification MUST được tái sử dụng từ contract hiện có và không được định nghĩa lại.
- **FR-012**: Hệ thống MUST tính Total Return, Win Rate, Maximum Drawdown và Number of Trades theo metric version. Backtesting MUST tạo immutable `EquityCurveSummary` theo streaming gồm point count, peak/trough evidence của maximum drawdown và canonical curve digest mà không giữ toàn equity curve trong RAM; Evaluation tính Maximum Drawdown từ summary này. Win Rate chỉ tính Trade có realized net P&L sau entry/exit fee lớn hơn 0 là thắng; hòa vốn không phải Trade thắng.
- **FR-013**: Evaluation MUST xác định kết quả cho zero trades, zero wins và input biên mà không tạo giá trị không hữu hạn.
- **FR-014**: Hệ thống MUST tính `returnScore = clamp(TotalReturn, 0, 1)`, `winRateScore = clamp(WinRate, 0, 1)` và `drawdownScore = 1 - clamp(MaximumDrawdown, 0, 1)`, rồi tính overall score trong `[0,1]` với trọng số tương ứng 45%, 30% và 25%; công thức cùng ngưỡng MUST thuộc ranking version.
- **FR-014a**: Evaluation có dưới 5 Trades MUST vẫn được lưu cùng bốn metrics nhưng MUST không đủ điều kiện xuất hiện trong Leaderboard; Number of Trades không trực tiếp cộng vào overall score.
- **FR-015**: Evaluation MUST thuộc cùng Experiment với Backtest Result nguồn và immutable sau khi hoàn tất.
- **FR-016**: Hệ thống MUST tạo Leaderboard Revision bất biến chứa tối đa K entry thuộc cùng Experiment.
- **FR-017**: Leaderboard MUST mặc định giữ Top 10, sắp xếp theo overall score giảm dần, Maximum Drawdown tăng dần rồi fingerprint tăng dần; hệ thống MUST tạo revision mới khi projection thay đổi.
- **FR-018**: Mỗi Entry MUST truy được về Evaluation, Result, Candidate, Experiment và các versions tạo điểm.
- **FR-019**: Reproduction MUST dùng Reproduction Run bền vững do Experiment capability sở hữu, tạo run mới liên kết bản gốc, không overwrite evidence và chỉ thành công khi ordered Trade sequence, equity summary/digest, bốn metrics cùng mọi fingerprint liên quan đều khớp chính xác. Backtesting, Evaluation và Leaderboard chỉ trả immutable verification report qua public contract, không sở hữu Reproduction Run.
- **FR-020**: Fingerprints MUST dùng canonical ordering/serialization và được phân tầng có version: `equity-curve-v1` bao phủ ordered streaming equity valuations thông qua incremental digest và summary evidence; `backtest-v1` bao phủ frozen provenance, versioned assumptions, ordered Trades, EquityCurveSummary/digest và Backtest Result; `evaluation-v1` bao phủ Backtest Result fingerprint, metric version, bốn metrics cùng overall score; `leaderboard-v1` bao phủ Experiment identity, ranking version và ordered Leaderboard Entries.
- **FR-021**: Trade, Result, Evaluation, Leaderboard Revision và Entry đã hoàn tất MUST immutable.
- **FR-022**: Durable results MUST tồn tại độc lập cache/queue và được lưu qua boundary của capability sở hữu.
- **FR-023**: Feature MUST không thực hiện Worker/retry/queue/dead-letter, Search, public API, realtime, UI hoặc giao dịch thật.
- **FR-024**: Feature MUST không hỗ trợ short selling, margin hoặc leverage trong MVP.
- **FR-025**: Acceptance scenarios MUST có evidence kiểm tra được; evidence chưa chạy giữ trạng thái Planned.

### Key Entities

- **Backtest Assumptions**: Cấu hình bất biến có version cho capital, fee, slippage, execution price và position mode.
- **Position**: Trạng thái vị thế mô phỏng đang mở.
- **Trade**: Giao dịch đã đóng bất biến gồm entry/exit, quantity, fee, profit/loss và sequence.
- **Backtest Result**: Outcome thành công của Candidate gắn successful Attempt, capital summary, Trades, provenance và fingerprint.
- **Evaluation Result**: Bốn metrics, overall score, metric/ranking versions và fingerprint.
- **Leaderboard Revision**: Snapshot Top-K bất biến của một Experiment.
- **Leaderboard Entry**: Rank của một Evaluation trong revision, gồm score và tie-break key.
- **Reproduction Run**: Bản ghi bền vững do Experiment capability sở hữu, liên kết artifact gốc và các immutable verification report từ Backtesting, Evaluation và Leaderboard.
- **Failure Classification**: Contract năm giá trị hiện có; F-006 tái sử dụng nhưng không sở hữu retry policy.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cùng frozen inputs và versions tạo cùng Trade sequence, bốn metrics và fingerprint trong 100% lần kiểm chứng.
- **SC-002**: Dataset nhiều batch được xử lý đủ, đúng thứ tự mà không giữ quá một batch Candle cộng state cần thiết.
- **SC-003**: 100% Result từ Attempt chưa thành công hoặc liên kết chéo bị từ chối mà không tạo dữ liệu một phần.
- **SC-004**: Mọi Evaluation hợp lệ tạo đủ bốn metrics và không tạo NaN, infinity hoặc chia cho zero.
- **SC-005**: Cùng Evaluation set tạo cùng Top-K, ranks và revision fingerprint trong 100% lần kiểm chứng, kể cả hòa điểm.
- **SC-006**: Reproduction hợp lệ khớp Trades, metrics và fingerprint; mọi thay đổi frozen input/version có ảnh hưởng đều bị phát hiện.
- **SC-007**: 100% Results và Entries truy ngược được Experiment, Candidate, Dataset, Strategy, assumptions và versions liên quan.
- **SC-008**: Kiểm tra boundary xác nhận Backtest, Evaluation và Leaderboard policy không phụ thuộc framework, storage, provider, transport hoặc UI.

## Assumptions

- Public contracts của F-003, F-004 và F-005 ổn định và được tái sử dụng, không sao chép.
- Dataset dùng cho Backtest đã freeze, chỉ gồm closed Candles và có membership/checksum kiểm chứng được.
- Business IDs dùng typed ULID; Supabase User ID dùng UUID.
- FailureClassification có năm giá trị: `TRANSIENT_NETWORK_ERROR`, `DATA_UNAVAILABLE_RETRY`, `WORKER_CRASHED`, `PERMANENT_LOGIC_ERROR`, `UNKNOWN_ERROR`.
- Chỉ successful Attempt tạo Result; retry và runtime failure mapping thuộc feature Worker.
- MVP chỉ cho phép một LONG position tại một thời điểm và khớp signal tại open của Candle kế tiếp để tránh look-ahead bias.
- Mỗi vị thế dùng 100% số dư khả dụng; fee tính trên notional ở cả hai chiều và slippage luôn điều chỉnh giá khớp theo hướng bất lợi cho người giao dịch.
- Overall score thuộc `[0,1]` với trọng số Return 45%, Win Rate 30% và Drawdown 25%; Number of Trades chỉ là ngưỡng độ tin cậy.
- Metrics dùng fixed clamp normalization; Evaluation cần tối thiểu 5 Trades để đủ điều kiện vào Leaderboard.
- Canonical trade values dùng scale 12, canonical metric/score values dùng scale 10 và mọi rounding dùng `HALF_EVEN`; database không được là nơi đầu tiên quyết định cách làm tròn.
- Fingerprints được phân tầng thành `backtest-v1`, `evaluation-v1` và `leaderboard-v1`; reproduction phải so sánh cả canonical outputs và fingerprint tương ứng.
- Mọi lỗi toàn vẹn Dataset/batch làm Backtest fail fast mà không tạo partial business outcome; việc ánh xạ lỗi sang Attempt thuộc caller/F-007.
- Leaderboard mặc định là Top 10 và dùng score, drawdown, fingerprint theo thứ tự tie-break đã chốt.
- PostgreSQL-compatible storage là source of truth; schema change dùng forward migration và không apply remote trong F-006.
- Đây là mô phỏng nghiên cứu, không đặt lệnh thật và không phải cam kết lợi nhuận hay lời khuyên tài chính.
