# F014 Public Boundary and Integration Gap Register

## Phạm vi audit

- Contract chuẩn: `specs/014-end-to-end-demo-hardening/contracts/integrated-demo-contract.md`.
- Boundary được kiểm tra: REST API, WebSocket, web service/mappers và các DTO dùng trong luồng Market → Strategy → Search → Backtest → Evaluation → Leaderboard → Result → News.
- Đây là kết quả đọc code tại T015. Trạng thái `PASS` nghĩa là mapping hiện diện và nhất quán ở mức contract; chưa thay thế cho live E2E evidence.

## Boundary đã khớp

| Stage | Public boundary | Web consumer | Kết quả audit |
|---|---|---|---|
| Market | `GET /api/v1/candles`; WebSocket `SUBSCRIBE_CANDLES` | `market-api.ts`, `market-realtime-controller.ts` | PASS — pair/timeframe và candle payload khớp; server giới hạn bốn candle subscriptions |
| Strategy catalog | `GET /api/v1/strategies` | `strategy-api.ts`, `schemas.ts` | PASS — bốn plugin dùng cùng descriptor contract; T014 đã kiểm tra response và validation |
| User Strategy | `/api/v1/user-strategies/**` | `strategy-api.ts` | PARTIAL — route, ownership và response schema khớp; request typing/composite identity còn gap INT-001/002 |
| Search | `POST /api/v1/experiments`; Experiment/Job/Candidate read endpoints | `experiment-command-service.ts`, `experiment-service.ts` | PARTIAL — identity, idempotency và stop bounds có mapping; cấu hình UI/search space còn gap INT-003/004 |
| Progress | WebSocket `SUBSCRIBE_EXPERIMENT` | `useExperimentRealtime.ts` | PARTIAL — snapshot URL và lifecycle events có; client validation còn gap INT-005 |
| Leaderboard | `GET /api/v1/experiments/{id}/leaderboard`; `SUBSCRIBE_LEADERBOARD` | `leaderboard-service.ts`, `useLeaderboardRealtime.ts` | PASS cho route/identity; payload hardening thuộc INT-005 |
| Result | `GET /api/v1/backtest-results/{resultId}` và `/api/v1/backtests/{id}/result` | `backtest-result-service.ts` | PARTIAL — authoritative result, trades, bốn metrics và provenance khớp; signal evidence còn gap INT-006 |
| News | `GET /api/v1/news-items` | `news-api.ts`, `schemas.ts` | PARTIAL — pagination/status/sentiment khớp; degraded presentation còn gap INT-007 |
| Authentication | JWT cho `/api/**`; short-lived single-use ticket cho `/ws` | shared API client và `realtime-client.ts` | PASS — REST cần authenticated session; WebSocket subscription Experiment/Leaderboard được kiểm tra ownership trước activation |

## Findings cần remediation

### INT-001 — Strategy form gửi sai kiểu tham số

- **Trạng thái**: RESOLVED tại T022; component tests và phần Strategy của `f014-research-flow.spec.ts` pass.
- **Mức độ**: HIGH — tạo Strategy đơn có tham số `INTEGER`, `DECIMAL`, `BOOLEAN` hoặc `ENUM` có thể bị API từ chối.
- **Bằng chứng**: `StrategyForm.tsx` và `strategy-draft.ts` giữ toàn bộ parameter dưới dạng `string`; `StrategyRequestMapper.parameter(...)` yêu cầu JSON number cho `INTEGER`, boolean cho `BOOLEAN`, text cho `ENUM/TEXT`.
- **Contract ảnh hưởng**: FR-005, SC-003.
- **Owner/remediation**: Web Strategy + API Strategy, T022/T028. Serialize theo `ParameterType`, giữ decimal dưới dạng chuỗi chính xác, và thêm integration/component test cho cả bốn schema.

### INT-002 — Composite form dùng policy identity không tồn tại

- **Trạng thái**: RESOLVED tại T022; UI gửi `majority-vote@1.0.0`, trình bày quy tắc tie → HOLD và E2E tạo/publish composite chạy qua.
- **Mức độ**: BLOCKING — composite tạo từ UI hiện không thể materialize.
- **Bằng chứng**: `StrategyForm.tsx` gửi `weighted-signal@1`; production registry chỉ có `majority-vote@1.0.0` trong `MajorityVotePolicy.java`; `SemanticVersion.parse` bắt buộc `MAJOR.MINOR.PATCH`.
- **Contract ảnh hưởng**: FR-005.
- **Owner/remediation**: Web Strategy + Combination/API, T018/T022/T028. Dùng identity thật từ boundary được công bố hoặc tối thiểu cấu hình đúng `majority-vote@1.0.0`, đồng thời hiển thị quy tắc xử lý xung đột.

### INT-003 — Search form chưa lấy Strategy contract thật

- **Trạng thái**: RESOLVED tại T023; form nạp catalog, sinh Search Space từ descriptor và E2E chọn published composite thành công.
- **Mức độ**: HIGH — người dùng phải gõ tay plugin/version và form vẫn giữ parameter range của MA khi đổi Strategy ID.
- **Bằng chứng**: `ExperimentConfigurationForm.tsx` render input text; `initialExperimentDraft` hard-code `ma-crossover` cùng `fastPeriod/slowPeriod`; hàm `strategies()` đã tồn tại nhưng form không dùng.
- **Contract ảnh hưởng**: FR-004, FR-006, SC-003.
- **Owner/remediation**: Web Experiment, T018/T023. Nạp catalog thật, chọn plugin/version và sinh Search Space từ descriptor thay vì browser hard-code.

### INT-004 — Search Space public mapping chỉ hỗ trợ khoảng số nguyên

- **Trạng thái**: MITIGATED tại T023; UI chỉ cho Search system Strategy có INTEGER hoặc ENUM/TEXT domain hợp lệ và ghi rõ Strategy chưa được MVP Search hỗ trợ. Contract decimal chưa được mở rộng.
- **Mức độ**: MEDIUM cho demo MA, HIGH nếu chọn Bollinger/enum — browser ép range qua `Number`, còn `ParameterRangeRequest` chỉ nhận `Long minimum/maximum` và `List<String> options`.
- **Bằng chứng**: `experiment-configuration.ts`, `CommandDtos.ParameterRangeRequest` và `ExperimentRequestMapper`.
- **Contract ảnh hưởng**: FR-004, FR-006.
- **Owner/remediation**: Search/API/Web, T020/T023/T028. Giữ demo live trên Strategy có integer domain nếu chưa mở rộng contract; nếu mở rộng decimal domain phải có compatibility decision trước vì F014 không tự đổi public semantics.

### INT-005 — Realtime client chưa kiểm tra chặt envelope/payload

- **Trạng thái**: RESOLVED tại T024–T025; Experiment và Leaderboard event v1 được runtime-validate, event lỗi kích hoạt REST reconcile, snapshot cũ được giữ, stale revision không ghi đè revision mới và terminal progress subscription được đóng.
- **Mức độ**: MEDIUM — client chỉ kiểm tra `eventType` là string; Experiment/Leaderboard payload được cast trực tiếp. Candle đã có Zod validation nhưng các work event chưa có schema tương đương.
- **Bằng chứng**: `realtime-client.ts`, `useExperimentRealtime.ts`, `useLeaderboardRealtime.ts`; server encoder/parser ở `RealtimeMessageMapper.java` là strict.
- **Contract ảnh hưởng**: FR-009, FR-012, FR-016.
- **Owner/remediation**: Web Realtime, T017/T021/T024/T025. Thêm runtime schema theo event version và reconcile snapshot khi payload/event không hợp lệ.

### INT-006 — Result chưa mang Strategy decision evidence cho Entry/Exit

- **Trạng thái**: RESOLVED cho scope cốt lõi tại T026; Result trình bày Entry/Exit execution, side, exit reason và Trades authoritative, đồng thời kiểm tra count/order/time consistency. Indicator-level reasoning được ghi rõ là không có trong contract và không bị browser suy diễn.
- **Mức độ**: HIGH đối với visualization/rubric — Result hiện có Trade side/time/price và bốn metrics nhưng không có RSI/Bollinger/zone hoặc reason/evidence của tín hiệu đã tạo entry/exit.
- **Bằng chứng**: `Trade.java` không lưu decision evidence; `ResultDtos.TradeResponse` và `TradeHistory.tsx` chỉ trình bày execution fields.
- **Contract ảnh hưởng**: FR-008, tiêu chí Visualization Strategy và Trade.
- **Owner/remediation**: Backtesting + API Result + Web Backtest, T020/T026/T028. T020 phải xác nhận phạm vi dữ liệu thật; không được dựng indicator/evidence trong browser.

### INT-007 — News chưa phân biệt đầy đủ empty/degraded state

- **Trạng thái**: RESOLVED tại T027; provider error/empty/sentiment-degraded có presentation riêng, News vẫn đọc được khi sentiment lỗi và schema chặn analysis state mâu thuẫn.
- **Mức độ**: MEDIUM — public payload có `analysisStatus` và sentiment nullable nhưng workspace gom lỗi retryable thành một thông báo chung, chưa giải thích trường hợp News có dữ liệu nhưng Sentiment đang lỗi.
- **Bằng chứng**: `NewsController.java`, `news/api/schemas.ts`, `NewsWorkspace.tsx`.
- **Contract ảnh hưởng**: FR-010, FR-011.
- **Owner/remediation**: News Web/API, T019/T027/T028. Giữ news usable, hiển thị analysis status theo item và phân biệt empty/provider failure/sentiment degraded.

### INT-008 — Nhãn Search mô tả fixture xuất hiện cả trong live profile

- **Trạng thái**: RESOLVED tại T023; nguồn Generator hiển thị theo composition `Fixture profile` hoặc `Live API`.
- **Mức độ**: MEDIUM — chữ “Fixture-only discovery” là tĩnh dù `fixture=false`, có thể khiến reviewer hiểu sai nguồn chạy.
- **Bằng chứng**: `ExperimentConfigurationForm.tsx`; chỉ badge được điều kiện hóa, mô tả Generator thì không.
- **Contract ảnh hưởng**: FR-017, FR-025.
- **Owner/remediation**: Web Experiment, T023/T024. Nội dung LIVE/FIXTURE phải theo composition thật và fixture evidence không được trộn với live evidence.

### INT-009 — Đổi timeframe làm mất panel khi giá trị bị trùng

- **Trạng thái**: RESOLVED tại T021; unit tests và `f014-market-demo.spec.ts` pass.
- **Mức độ**: HIGH — Market Dashboard từ bốn panel giảm còn ba khi một panel chọn timeframe đang được panel khác sử dụng.
- **Bằng chứng**: `f014-market-demo.spec.ts` red tại assertion Panel 4; `parseMarketSelection` đi qua `canonicalEnumList`, nơi loại phần tử trùng trước khi giới hạn bốn panel.
- **Contract ảnh hưởng**: FR-003, SC-002.
- **Owner/remediation**: Market Web, T021. Giữ đúng bốn slot/panel theo vị trí; timeframe được phép trùng vì mỗi panel là một lựa chọn độc lập.

### INT-010 — Start Search chưa nhận published User Strategy version

- **Trạng thái**: RESOLVED tại T028; public request nhận `userStrategyVersionId` theo hướng mutually exclusive với system `searchSpace`, resolve snapshot theo authenticated owner, chỉ dùng version đã publish và đóng băng provenance/composite components trước khi Search bắt đầu. Mapping, authorization và empty frozen-candidate Search Space có automated tests.
- **Mức độ**: BLOCKING — UI/API hiện không thể dùng Strategy cá nhân hoặc composite đã publish làm đầu vào Search.
- **Bằng chứng**: `StartExperimentRequest`, `SearchStartCommandFactory.Request` và `SearchStartCommandFactoryService` chỉ nhận `StrategyPluginId + strategyVersion`; trong khi `StrategyProvenanceSnapshot` đã hỗ trợ `sourceUserStrategyVersionId` và composite provenance.
- **Contract ảnh hưởng**: FR-005, FR-007, FR-018.
- **Owner/remediation**: Experiment Execution + Strategy + API/Web, T018/T020/T023/T028. Thêm lựa chọn `userStrategyVersionId` theo hướng additive/mutually-exclusive, resolve published snapshot theo owner và đóng băng provenance; giữ request system plugin cũ để tương thích.

### INT-011 — News filter bị race khi đồng bộ URL và local state

- **Trạng thái**: RESOLVED tại T027; pending URL intent ngăn search params cũ ghi đè local filter và Playwright xác nhận query `analysisStatus` ổn định.
- **Mức độ**: MEDIUM — checkbox filter có thể bị trả về trạng thái cũ ngay sau click nên query mong muốn không được giữ ổn định.
- **Bằng chứng**: `f014-news-demo.spec.ts` red tại thao tác chọn `ANALYZED`; `NewsWorkspace` vừa dispatch local state vừa có effect đồng bộ ngược từ `useSearchParams` trong lúc `router.replace` chưa hoàn tất.
- **Contract ảnh hưởng**: FR-010 và khả năng demo News có kiểm soát.
- **Owner/remediation**: News Web, T027. Dùng URL làm nguồn state duy nhất hoặc bỏ vòng đồng bộ hai chiều gây race; giữ latest-request protection khi query đổi.

## Kết luận T015

- Không cần đổi route public; các thay đổi request đều additive và giữ đường system Strategy cũ.
- Các blocker của main flow đã được đóng. Giới hạn còn lại là decimal Search Space chưa mở rộng và indicator-level Entry/Exit reasoning chưa có trong contract; UI không dựng dữ liệu thay backend.
- Authoritative Leaderboard → Result link tiếp tục dùng `backtestResultId` đúng route.
- Gate mapping/authorization của T028 pass. Full API suite còn ba lỗi log-redaction đã được ghi từ baseline và thuộc final security/quality remediation, không được tính là pass cho tới khi xử lý.
