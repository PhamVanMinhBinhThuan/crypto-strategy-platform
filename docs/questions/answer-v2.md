# Answer V2 — Bộ trả lời ôn vấn đáp kiến trúc

Tài liệu này tổng hợp **23 câu hỏi hiện có** trong `docs/questions/` thành một file duy nhất. Mỗi câu được viết theo cách có thể trình bày trong khoảng 30–60 giây, sau đó có các ý mở rộng để trả lời khi giảng viên hỏi sâu.

> Cách trả lời nên dùng: **nêu quyết định → giải thích driver → mô tả cơ chế → nói trade-off → chốt trạng thái thực tế**. Không biến mục tiêu chưa benchmark thành kết quả đã đạt.

## Mục lục

1. [Architectural drivers](#1-architectural-drivers-là-gì)
2. [C4 Context và Container](#2-c4-context-và-container-của-nhóm)
3. [Module boundaries](#3-boundary-của-market--strategy--experiment--news)
4. [Thêm Strategy mới](#4-thêm-strategy-mới-sửa-ở-đâu)
5. [Đổi Search Algorithm](#5-đổi-search-algorithm-sửa-ở-đâu)
6. [Đổi Market Provider](#6-provider-mới-có-làm-frontend-đổi-không)
7. [Scale 100.000 Backtests](#7-100000-backtests-scale-thế-nào)
8. [Failure isolation](#8-một-service-lỗi-có-làm-hỏng-toàn-hệ-thống-không)
9. [Duplicate, retry và event order](#9-duplicateretryevent-order-xử-lý-thế-nào)
10. [Leaderboard provenance](#10-từ-kết-quả-leaderboard-truy-lại-nguồn-gốc-thế-nào)
11. [Phương pháp ADD](#11-nhóm-dùng-add-như-thế-nào)
12. [Trade-off kiến trúc](#12-trade-off-quan-trọng-nhất-là-gì)
13. [ATAM scenarios](#13-atam-scenario-nào-nguy-hiểm-nhất)
14. [Transaction boundary](#14-backtest-hoàn-tất-nghĩa-là-gì-về-transaction)
15. [Event-Driven](#15-nhóm-dùng-event-driven-ở-đâu)
16. [CQRS](#16-cqrs-có-được-dùng-không)
17. [Clean Architecture](#17-strategy-có-được-gọi-trực-tiếp-db-hay-binance-không)
18. [Bounded Context](#18-bounded-context-của-nhóm-là-gì)
19. [Composite Strategy](#19-composite-strategy-hoạt-động-thế-nào)
20. [Sentiment versioning](#20-kết-quả-sentiment-truy-vết-model-thế-nào)
21. [Sentiment isolation](#21-tại-sao-sentiment-là-service-python-riêng)
22. [Modular Monolith và Microservices](#22-modular-monolith-hay-microservices)
23. [Stateless và idempotent Worker](#23-để-worker-scale-stateless-cần-điều-kiện-gì)

---

## 1. Architectural drivers là gì?

### Trả lời vấn đáp

Architectural drivers là những yêu cầu có ảnh hưởng lớn đến hình dạng kiến trúc, chứ không phải toàn bộ yêu cầu chức năng. Với Crypto Strategy Lab, các driver chính là: dễ thêm Strategy và thay thuật toán Search; scale số lượng Backtest; hiệu năng và realtime; độ tin cậy; khả năng bảo trì, quan sát và tái tạo kết quả. Từ các driver đó, nhóm mới chọn module boundary, Plugin/Registry, provider adapter, Queue–Worker, idempotency và frozen provenance. Nghĩa là nhóm chọn kiến trúc theo vấn đề cần giải quyết, không chọn công nghệ chỉ vì nó phổ biến.

### Nếu giảng viên hỏi sâu

- **Modifiability:** thêm MACD hoặc Genetic Search mà không sửa Backtester.
- **Scalability:** đưa tác vụ dài qua queue và scale ngang Worker.
- **Reliability:** retry có giới hạn, recovery và chống xử lý trùng.
- **Reproducibility:** lưu version, seed, dataset và fingerprint để chạy lại.
- Các mục tiêu p95 hay throughput chỉ được khẳng định sau benchmark; hiện kiến trúc và cơ chế đã có nhưng một số phép đo vẫn là **Planned**.

**Câu chốt:** Driver trả lời câu hỏi “vì sao kiến trúc phải được thiết kế như vậy?”.

[Xem câu trả lời gốc](1_architectural_drivers/answer.md)

---

## 2. C4 Context và Container của nhóm?

### Trả lời vấn đáp

Ở mức **Context**, Crypto Strategy Lab là một hệ thống được User/Trader sử dụng, lấy dữ liệu thị trường từ Binance và tin tức từ News Providers. Ở mức **Container**, bên trong có Web Next.js, API Spring Boot, Worker Spring Boot, Sentiment FastAPI, PostgreSQL và Redis. Web gọi API qua REST/WebSocket; API xử lý request ngắn và realtime; Worker chạy Backtest/Search/News bất đồng bộ; PostgreSQL là nguồn dữ liệu bền vững; Redis dùng cho queue, cache hoặc progress. Việc tách API khỏi Worker giúp tác vụ dài không chặn HTTP request.

### Nếu giảng viên hỏi sâu

- C1 cho biết hệ thống giao tiếp với **ai và hệ thống ngoài nào**.
- C2 cho biết các **ứng dụng/runtime và data store lớn** bên trong.
- C3 mới đi sâu vào module như Market, Strategy, Experiment và News.
- Sentiment tách riêng vì dùng Python/ML; Worker tách riêng vì có tải CPU dài và cần scale độc lập.

**Câu chốt:** Context là nhìn từ ngoài vào; Container là mở hộp hệ thống ra.

[Xem câu trả lời gốc](2_c4_context_container/answer.md)

---

## 3. Boundary của Market / Strategy / Experiment / News?

### Trả lời vấn đáp

Mỗi module sở hữu một capability và ngôn ngữ nghiệp vụ riêng. Market sở hữu Candle, Dataset và provider normalization; Strategy sở hữu Strategy contract, Signal, Plugin và Registry; Experiment sở hữu Manifest, Candidate, Job và Attempt; News sở hữu NewsItem và vòng đời Sentiment. Module chỉ công bố public API, port hoặc event cần thiết; module khác không import package `internal` hay truy cập trực tiếp repository và bảng dữ liệu của owner khác. `apps/api` và `apps/worker` đóng vai trò composition/orchestration, còn persistence là adapter implement output port.

### Nếu giảng viên hỏi sâu

- Dùng chung PostgreSQL không có nghĩa là được đọc chéo bảng tùy ý.
- Quy tắc phụ thuộc được bảo vệ bằng Gradle modules, package `api/internal` và architecture tests.
- Output port thuộc về module nghiệp vụ; JDBC adapter chỉ là implementation ở ngoài.

**Câu chốt:** Boundary tốt không chỉ chia folder, mà quy định rõ ai sở hữu dữ liệu và module khác được phép gọi qua contract nào.

[Xem câu trả lời gốc](3_module_boundaries/answer.md)

---

## 4. Thêm Strategy mới sửa ở đâu?

### Trả lời vấn đáp

Để thêm Strategy mới như MACD, nhóm tạo `MacdStrategy` implement `Strategy`, tạo `MacdStrategyPlugin` khai báo ID, version, parameter schema và cách khởi tạo, viết test rồi đăng ký plugin vào `StrategyPlugins.trusted()`. Backtester chỉ gọi contract `evaluate(context)`, còn Search, Evaluation và Leaderboard làm việc với model chung, nên không cần thêm nhánh `if MACD`. Đây là cách áp dụng Open–Closed Principle: mở rộng bằng plugin mới và hạn chế sửa pipeline đang ổn định.

### Nếu giảng viên hỏi sâu

- Registry kiểm tra ID/version trùng và validate parameters.
- Plugin mô tả số candle warm-up và ràng buộc tham số.
- Hiện project đã có Moving Average Crossover; MACD là ví dụ mở rộng, **chưa phải implementation hiện có**.
- UI chỉ cần render theo descriptor/schema chung nếu contract không đổi.

**Câu chốt:** Thêm Strategy là thêm implementation và plugin, không sửa Backtester.

[Xem câu trả lời gốc](4_add_new_strategy/answer.md)

---

## 5. Đổi Search Algorithm sửa ở đâu?

### Trả lời vấn đáp

Muốn thay Random Search bằng Domain-guided hoặc Genetic Search, nhóm tạo generator mới implement `StrategyGenerator`, khai báo ID/version, bảo đảm tính deterministic, viết test và đăng ký qua factory/registry. Tất cả generator đều trả về `GeneratedCandidate` theo contract chung, nên Job, Worker, Backtest, Evaluation và Leaderboard không phụ thuộc candidate được sinh bằng thuật toán nào. Experiment lưu generator ID, version, seed, Search Space và state để có thể tái lập.

### Nếu giảng viên hỏi sâu

- Generator chỉ **đề xuất candidate**, không chạy Backtest hay xếp hạng.
- Cùng input, seed và state thì phải sinh cùng kết quả.
- Hiện có Random Search; Genetic/Domain-guided mới là điểm mở rộng dự kiến.
- Thay thuật toán có trade-off về chất lượng tìm kiếm, chi phí tính toán và khả năng tái lập.

**Câu chốt:** `StrategyGenerator` là điểm thay thế; output phía sau vẫn là `GeneratedCandidate` chuẩn.

[Xem câu trả lời gốc](5_replace_search_algorithm/answer.md)

---

## 6. Provider mới có làm frontend đổi không?

### Trả lời vấn đáp

Không, nếu provider mới có adapter tuân thủ `MarketDataProvider` và ánh xạ payload riêng sang canonical `Candle` và error model của hệ thống. Frontend chỉ gọi REST/WebSocket của Crypto Strategy Lab, không biết Binance dùng JSON, symbol hay mã lỗi nào. Khi đổi sang OKX, nhóm chỉ thêm OKX adapter, configuration và contract test ở backend. Các khác biệt như `BTCUSDT` và `BTC/USDT`, interval, timestamp hoặc OHLCV phải được adapter chuẩn hóa.

### Nếu giảng viên hỏi sâu

- Adapter che giấu transport, credential, rate limit và schema riêng của sàn.
- Backend xử lý reconnect, gap recovery và deduplicate cho realtime stream.
- WebSocket có thể multiplex nhiều subscription/timeframe trên cùng kết nối.
- Canonical model giúp frontend ổn định nhưng có thể làm mất field đặc thù; nếu field đó thực sự cần cho nghiệp vụ thì phải mở rộng contract có kiểm soát.
- Hiện Binance adapter đã có; OKX chưa được implement.

**Câu chốt:** Đổi provider chỉ không ảnh hưởng frontend khi adapter giữ nguyên canonical contract.

[Xem câu trả lời gốc](6_replace_market_provider/answer.md)

---

## 7. 100.000 Backtests scale thế nào?

### Trả lời vấn đáp

Nhóm không chạy 100.000 Backtests ngay trong HTTP request vì sẽ timeout và dễ cạn RAM. API đóng băng Experiment Manifest, ghi Job và Outbox trong database rồi trả `202 Accepted`. Outbox Publisher đẩy job vào Redis Stream; nhiều Worker trong Consumer Group lấy job để chia tải. Mỗi Worker đọc dataset theo batch, chạy Backtest và ghi kết quả về PostgreSQL. Vì Worker độc lập và job có trạng thái bền vững, hệ thống có thể scale ngang bằng cách tăng số Worker, còn frontend chỉ theo dõi progress qua polling hoặc WebSocket.

### Nếu giảng viên hỏi sâu

- **Queue–Worker:** tách tốc độ nhận request khỏi tốc độ xử lý.
- **Batching:** không nạp toàn bộ dataset lớn vào RAM.
- **Backpressure:** queue phải có giới hạn và admission control, không nhận vô hạn.
- **Top-K projection:** phục vụ Leaderboard mà không sort lại toàn bộ kết quả mỗi lần đọc.
- **Idempotency/recovery:** xử lý lại an toàn khi message lặp hoặc Worker chết.
- Thêm Worker không bảo đảm throughput tăng tuyến tính; PostgreSQL connection pool, lock và dataset I/O có thể trở thành bottleneck.
- Kiến trúc scale đã có, nhưng con số 100.000 cần benchmark mới được tuyên bố là đã đạt.

**Câu chốt:** 100.000 Backtests được biến thành nhiều job nhỏ, xử lý bất đồng bộ và phân phối cho Worker stateless.

[Xem câu trả lời gốc](7_scale_100000_backtests/answer.md)

---

## 8. Một service lỗi có làm hỏng toàn hệ thống không?

### Trả lời vấn đáp

Không phải mọi lỗi đều lan ra toàn hệ thống vì các capability có boundary và failure policy riêng. Ví dụ Sentiment Service down thì News/Sentiment chuyển sang degraded hoặc chờ xử lý lại, trong khi Market chart và Technical Backtest vẫn chạy vì không phụ thuộc runtime ML. Khi Binance WebSocket lỗi, Market thực hiện reconnect và backfill; dữ liệu lịch sử đã đóng băng vẫn có thể phục vụ Backtest. Timeout, retry có giới hạn, circuit breaker và durable state giúp giới hạn blast radius.

### Nếu giảng viên hỏi sâu

- Isolation không có nghĩa là “không bao giờ ảnh hưởng”; DB chung hoặc Redis vẫn có thể là shared failure point.
- Retry chỉ dành cho lỗi tạm thời và phải có backoff/jitter.
- Lỗi vĩnh viễn cần chuyển trạng thái rõ ràng hoặc Dead Letter Queue.
- Cần E2E/chaos test và đo thực tế trước khi khẳng định mức availability.

**Câu chốt:** Hệ thống cô lập lỗi theo capability; chức năng phụ thuộc trực tiếp có thể degraded, chức năng độc lập vẫn hoạt động.

[Xem câu trả lời gốc](8_failure_isolation/answer.md)

---

## 9. Duplicate/retry/event order xử lý thế nào?

### Trả lời vấn đáp

Hệ thống chọn delivery **at-least-once**, nên không giả định message chỉ đến đúng một lần. Mỗi message có `eventId/messageId`; Worker kiểm tra idempotency trước khi xử lý và database dùng unique key hoặc fingerprint để ngăn tạo effect nghiệp vụ trùng. Lỗi tạm thời được retry có giới hạn; Worker chết thì pending message được reclaim; lỗi vĩnh viễn đi Dead Letter Queue. Với event đến sai thứ tự, consumer dùng aggregate version, revision hoặc timestamp nghiệp vụ để bỏ bản cũ. Frontend cũng deduplicate theo event ID, Candle identity hoặc Leaderboard revision.

### Nếu giảng viên hỏi sâu

- Queue có thể giao cùng message nhiều lần, nhất là khi xử lý xong nhưng crash trước ACK.
- Idempotency phải bảo vệ **business effect**, không chỉ nhớ message trong RAM.
- Ordering toàn cục rất đắt; thường chỉ cần ordering theo aggregate/stream key.
- “Exactly once” end-to-end không được giả định chỉ vì broker có một tính năng tương tự.

**Câu chốt:** At-least-once + idempotent consumer + version check thực tế hơn lời hứa exactly-once.

[Xem câu trả lời gốc](9_retry_duplicate_event_order/answer.md)

---

## 10. Từ kết quả Leaderboard truy lại nguồn gốc thế nào?

### Trả lời vấn đáp

Mỗi Leaderboard Entry không chỉ có điểm và thứ hạng mà còn tham chiếu đến Evaluation Result, Backtest Result và fingerprint. Từ đó có thể lần ngược qua Successful Attempt, Candidate và Experiment Manifest để biết dataset version/checksum, Strategy ID/version, parameters, generator ID/version, seed, fee, slippage và các assumption đã dùng. Nhờ graph provenance được đóng băng, nhóm có thể giải thích vì sao một kết quả đứng Top 1 và tạo reproduction request để chạy lại đúng cấu hình.

### Nếu giảng viên hỏi sâu

```text
Leaderboard Entry
→ Evaluation Result
→ Backtest Result + Trades
→ Successful Attempt
→ Candidate
→ Frozen Experiment Manifest
```

- Không được “tái tạo” bằng cấu hình mới nhất vì version có thể đã đổi.
- Fingerprint giúp nhận biết cùng một input/cấu hình đã tạo kết quả canonical nào.
- Tái lập yêu cầu cả code/version, dataset và assumption, không chỉ Strategy parameters.

**Câu chốt:** Leaderboard là projection để đọc nhanh; nguồn sự thật vẫn là chuỗi kết quả bất biến và Manifest đã đóng băng.

[Xem câu trả lời gốc](10_leaderboard_provenance/answer.md)

---

## 11. Nhóm dùng ADD như thế nào?

### Trả lời vấn đáp

ADD là Attribute-Driven Design: bắt đầu từ architectural drivers và quality attribute scenario, không bắt đầu từ framework. Mỗi vòng lặp, nhóm chọn phần hệ thống và yêu cầu quan trọng nhất, chọn pattern/tactic phù hợp, phân rã trách nhiệm, định nghĩa interface rồi kiểm chứng bằng scenario và test. Ví dụ driver scalability dẫn đến Queue–Worker; từ đó nhóm định nghĩa Backtest Job, Outbox, Consumer Group, idempotency và metric throughput. Nếu kết quả chưa đáp ứng scenario thì quay lại điều chỉnh thiết kế.

### Nếu giảng viên hỏi sâu

- Input: business goal, functional requirement quan trọng, quality attribute và constraint.
- Output: responsibilities, interfaces, views, ADR và cách verify.
- ADD là lặp dần, không cố thiết kế toàn bộ hệ thống trong một lần.
- Pattern chỉ hợp lý khi truy ngược được về driver cụ thể.

**Câu chốt:** ADD tạo chuỗi lập luận có thể bảo vệ: driver → tactic/pattern → interface → verification.

[Xem câu trả lời gốc](11_add_method/answer.md)

---

## 12. Trade-off quan trọng nhất là gì?

### Trả lời vấn đáp

Trade-off lớn nhất là giữa khả năng scale/độ tin cậy với độ phức tạp. Nhóm chọn Queue–Worker bất đồng bộ để API phản hồi nhanh, retry được và scale ngang, nhưng phải chấp nhận eventual consistency, duplicate message, khó tracing và cần idempotency. Ở cấp deployment, nhóm chọn Modular Monolith cho backend cốt lõi để giữ build, test và vận hành đơn giản; đổi lại phải dùng architecture test để boundary không suy thoái. Các quyết định này được ghi trong ADR cùng lý do và hệ quả.

### Nếu giảng viên hỏi sâu

| Quyết định | Lợi ích | Chi phí |
| --- | --- | --- |
| Queue–Worker | Scale, retry, API không bị block | Eventual consistency, duplicate, tracing |
| Modular Monolith | Đơn giản cho nhóm nhỏ | Cần kỷ luật giữ boundary |
| Canonical model | Thay provider dễ | Có thể mất field đặc thù |
| Frozen provenance | Audit và tái lập | Tăng storage và metadata |

**Câu chốt:** Không có kiến trúc “tốt tuyệt đối”; quyết định tốt là quyết định phù hợp driver và nói rõ cái giá phải trả.

[Xem câu trả lời gốc](12_trade_off/answer.md)

---

## 13. ATAM scenario nào nguy hiểm nhất?

### Trả lời vấn đáp

Scenario nguy hiểm nhất là tải 100.000 Backtests vì nó đồng thời đánh vào scalability, performance và reliability. Nhóm giảm rủi ro bằng queue, nhiều Worker, batching, backpressure, idempotency và Top-K projection. Scenario quan trọng thứ hai là Sentiment/News bị lỗi nhưng chart vẫn phải sống; giải pháp là tách runtime, timeout và circuit breaker. Tuy nhiên nhóm chỉ có thể nói kiến trúc đã chuẩn bị tactic; khả năng chịu đúng mức tải mục tiêu vẫn cần benchmark và failure test để xác nhận.

### Nếu giảng viên hỏi sâu

- Một ATAM scenario nên có: **source, stimulus, environment, artifact, response, response measure**.
- Sensitivity point: batch size, số Worker, DB pool, timeout và retry count.
- Trade-off point: tăng Worker có thể tăng throughput nhưng gây áp lực lên DB.
- Rủi ro lớn phải có phép đo, không chỉ có sơ đồ.

**Câu chốt:** ATAM dùng scenario để “đập thử” quyết định kiến trúc và tìm sensitivity/trade-off point trước khi production gặp lỗi.

[Xem câu trả lời gốc](13_atam_scenarios/answer.md)

---

## 14. Backtest “hoàn tất” nghĩa là gì về transaction?

### Trả lời vấn đáp

Một Backtest chỉ được coi là hoàn tất khi Trades, Metrics, trạng thái `COMPLETED` và Outbox Event mô tả kết quả đã được ghi nhất quán. Database changes và Outbox record phải commit trong cùng transaction; sau đó Outbox Publisher mới gửi event sang Redis. Nếu Worker crash sau khi commit nhưng trước khi publish, publisher sẽ gửi lại. Nếu crash giữa lúc xử lý, stale attempt được reclaim và job chạy lại an toàn nhờ idempotency. Leaderboard chỉ đọc kết quả đã hoàn tất, không đọc dữ liệu nửa chừng.

### Nếu giảng viên hỏi sâu

- Không thể dùng một transaction ACID duy nhất bao trùm PostgreSQL và Redis một cách đơn giản.
- Transactional Outbox đóng “khoảng trống” giữa commit DB và publish message.
- Publish lại có thể tạo duplicate, vì vậy consumer vẫn phải idempotent.
- Attempt là lịch sử lần chạy; Job là ý định nghiệp vụ. Không nên nhập hai khái niệm này làm một.

**Câu chốt:** Atomic trong database, at-least-once ngoài queue, và idempotent ở consumer.

[Xem câu trả lời gốc](14_transaction_boundary/answer.md)

---

## 15. Nhóm dùng Event-Driven ở đâu?

### Trả lời vấn đáp

Nhóm dùng Event-Driven ở những ranh giới bất đồng bộ, đặc biệt từ Outbox sang Redis Stream và từ Backtest hoàn tất sang Evaluation/Ranking/Audit. Worker phát event như `BacktestCompleted` thay vì gọi trực tiếp từng consumer cụ thể. Nhờ vậy có thể thêm consumer mới mà không sửa producer, tác vụ dài không block request và sự cố tạm thời có thể retry. Nhóm không dùng event cho mọi thứ; các thao tác đồng bộ cần kết quả ngay và nằm trong cùng boundary vẫn dùng direct call.

### Nếu giảng viên hỏi sâu

- Event là sự kiện đã xảy ra, nên đặt tên ở thì quá khứ và có schema/version.
- Event-Driven giảm coupling về compile-time nhưng tăng coupling vào schema và semantics.
- Cần xử lý duplicate, ordering, retry, DLQ và observability.
- Direct call phù hợp khi cần phản hồi tức thời và transaction cục bộ rõ ràng.

**Câu chốt:** Dùng event tại ranh giới cần tách thời gian và tách consumer; không biến toàn hệ thống thành event chỉ để “hiện đại”.

[Xem câu trả lời gốc](15_event_driven/answer.md)

---

## 16. CQRS có được dùng không?

### Trả lời vấn đáp

Nhóm dùng **tư duy CQRS có chọn lọc** cho Leaderboard. Write side đi qua pipeline Backtest → Evaluation → Ranking và lưu dữ liệu chi tiết, trong khi read side dùng Top-K projection được tối ưu để UI đọc nhanh. Tuy nhiên nhóm không triển khai full CQRS hay Event Sourcing vì chưa có driver đủ mạnh; immutable records và frozen Manifest đã đáp ứng audit/reproducibility với chi phí thấp hơn. Đây là sự tách model đọc–ghi ở nơi có lợi ích rõ, không phải tách toàn hệ thống.

### Nếu giảng viên hỏi sâu

- Projection có thể eventual consistent so với write model.
- Cần revision/version để frontend không nhận bản Leaderboard cũ.
- Projection có thể rebuild từ source records nếu được thiết kế đầy đủ.
- CQRS không đồng nghĩa Event Sourcing; hai pattern có thể dùng độc lập.

**Câu chốt:** Nhóm dùng CQRS-lite cho Leaderboard, không dùng full CQRS/Event Sourcing vì chi phí lớn hơn nhu cầu.

[Xem câu trả lời gốc](16_cqrs/answer.md)

---

## 17. Strategy có được gọi trực tiếp DB hay Binance không?

### Trả lời vấn đáp

Không. Strategy là business policy ở lớp trong, nên không import repository, Spring, database schema hay Binance client. Dữ liệu cần thiết được chuẩn bị và truyền vào qua `StrategyContext`; các port định nghĩa abstraction, còn adapter ở lớp ngoài implement việc đọc DB hoặc gọi provider. Nhờ Dependency Rule, đổi Binance sang OKX hoặc đổi persistence không làm thay đổi thuật toán Strategy, đồng thời Strategy có thể unit test bằng input thuần.

### Nếu giảng viên hỏi sâu

- Nếu Strategy tự query DB, kết quả dễ phụ thuộc ngầm vào trạng thái bên ngoài và khó tái lập.
- Dependency Inversion nghĩa là domain sở hữu interface cần dùng, infrastructure phụ thuộc và implement interface đó.
- Orchestrator/Application Service chịu trách nhiệm lấy dữ liệu rồi gọi Strategy.

**Câu chốt:** Strategy nhận data, không tự đi tìm data.

[Xem câu trả lời gốc](17_clean_architecture_dependency/answer.md)

---

## 18. Bounded Context của nhóm là gì?

### Trả lời vấn đáp

Nhóm có bốn Bounded Context chính: Market Data với Candle/Pair/Timeframe; Strategy với StrategyDefinition/Signal/Combination; Experiment với Candidate/Backtest/Evaluation/Ranking; và News Intelligence với NewsItem/Sentiment. Ranh giới ngữ nghĩa điển hình là `Signal` khác `Trade`: Signal chỉ là quyết định phân tích BUY/SELL/HOLD, còn Trade là giao dịch đã được mô phỏng với entry, exit, PnL, fee và slippage. Nếu dùng chung hai khái niệm, Strategy sẽ bị kéo vào chi tiết của Backtest và tạo semantic coupling.

### Nếu giảng viên hỏi sâu

- Cùng một từ có thể mang nghĩa khác nhau ở context khác nhau.
- Mỗi context có model và invariant riêng, trao đổi qua contract rõ ràng.
- Đổi cách tính fee/slippage trong Experiment không nên làm Strategy API đổi.
- Bounded Context là ranh giới ngôn ngữ và model, không nhất thiết bằng một microservice.

**Câu chốt:** Signal là ý định phân tích; Trade là kết quả thực thi mô phỏng.

[Xem câu trả lời gốc](18_bounded_context/answer.md)

---

## 19. Composite Strategy hoạt động thế nào?

### Trả lời vấn đáp

Composite Strategy chạy các strategy con để lấy Signal, sau đó giao cho một `CombinationPolicy` quyết định Signal cuối. Với Majority Vote, BUY chiếm 2/3 thì kết quả là BUY. Với Weighted Vote, mỗi strategy có trọng số; quy đổi BUY = 1, HOLD = 0, SELL = -1, cộng điểm rồi so với threshold. Không strategy con nào tự quyết định kết quả tổng hợp. Việc tách policy khỏi strategy giúp thêm cách kết hợp mới và test độc lập mà không sửa thuật toán con.

### Nếu giảng viên hỏi sâu

- Majority Vote phù hợp khi các strategy có độ tin cậy ngang nhau.
- Weighted Vote phù hợp khi có bằng chứng để gán độ tin cậy khác nhau.
- Weight và threshold phải được lưu trong frozen configuration để tái lập.
- Cần quy tắc rõ cho hòa phiếu, thiếu signal hoặc strategy con lỗi.

**Câu chốt:** Strategy con tạo tín hiệu; CombinationPolicy sở hữu luật giải quyết khi các tín hiệu “cãi nhau”.

[Xem câu trả lời gốc](19_composite_strategy/answer.md)

---

## 20. Kết quả Sentiment truy vết model thế nào?

### Trả lời vấn đáp

Mỗi `SentimentResult` lưu `newsId`, nhãn sentiment, score, `modelName`, `modelVersion`, `inputVersion` và `createdAt`. Metadata này cho biết prediction được tạo bởi model và pipeline tiền xử lý nào. Nếu model v3 có regression, nhóm có thể lọc đúng các kết quả do v3 tạo, so sánh trên cùng tập tin, rollback hoặc chạy lại chỉ phần bị ảnh hưởng. Ngoài version, hệ thống cần monitor inference failure, latency, input error và distribution drift.

### Nếu giảng viên hỏi sâu

- Model version mà không có preprocessing/input version vẫn chưa đủ để tái lập.
- Cần log version đang deploy và correlation ID từ NewsItem đến prediction.
- Automated drift detection/alerting hiện là **Planned**, không nên nói đã hoàn thiện nếu chưa có evidence.
- Versioning hỗ trợ audit, canary comparison và rollback.

**Câu chốt:** Một prediction production phải trả lời được ai tạo, bằng model nào, input version nào và lúc nào.

[Xem câu trả lời gốc](20_mlops_sentiment_versioning/answer.md)

---

## 21. Tại sao Sentiment là service Python riêng?

### Trả lời vấn đáp

Sentiment được tách thành Python/FastAPI service vì có runtime và thư viện ML khác Java, resource profile riêng và failure mode riêng như model load, inference timeout hoặc OOM. Nó cũng có thể scale độc lập theo tải phân tích tin. Khi service này down, chỉ News/Sentiment pipeline chuyển sang degraded hoặc pending; Market chart và Technical Backtest vẫn hoạt động vì không gọi Sentiment. Java Worker giao tiếp qua HTTP contract có timeout, retry giới hạn và circuit breaker.

### Nếu giảng viên hỏi sâu

- Tách service tăng deployment, network và contract overhead.
- Lợi ích là fault isolation và khả năng scale/runtime độc lập.
- Không retry vô hạn vì sẽ gây retry storm.
- Sentiment-based Strategy cần chính sách rõ khi sentiment chưa có; không được âm thầm dùng dữ liệu sai.

**Câu chốt:** Tách Sentiment vì có driver thật về runtime, scale và failure isolation, không phải vì muốn gọi hệ thống là microservices.

[Xem câu trả lời gốc](21_sentiment_service_isolation/answer.md)

---

## 22. Modular Monolith hay Microservices?

### Trả lời vấn đáp

Nhóm chọn Modular Monolith cho backend Java cốt lõi: các capability nằm trong một codebase/deployable nhưng có public API và boundary được kiểm tra bằng package rule/ArchUnit. Với nhóm nhỏ, lựa chọn này giảm chi phí deploy, network failure, distributed tracing và data consistency so với tách microservices sớm. Nhóm chỉ tách process khi có driver rõ: Worker có tải CPU dài và cần scale ngang; Sentiment dùng Python/ML runtime khác. Vì vậy đây không phải monolith không cấu trúc, cũng không phải microservices toàn phần.

### Nếu giảng viên hỏi sâu

- Modular Monolith vẫn có thể tách service sau nếu boundary và contract tốt.
- Shared database là rủi ro; module ownership và cấm truy cập chéo phải được enforce.
- “Distributed monolith” xảy ra khi tách deploy nhưng vẫn coupling chặt.
- Microservices chỉ đáng dùng khi scale, isolation, ownership hoặc deployment cadence tạo đủ lợi ích.

**Câu chốt:** Nhóm giữ phần chưa cần phân tán ở Modular Monolith và chỉ tách nơi driver biện minh được chi phí.

[Xem câu trả lời gốc](22_modular_monolith_vs_microservices/answer.md)

---

## 23. Để Worker scale stateless cần điều kiện gì?

### Trả lời vấn đáp

Worker muốn scale ngang phải **stateless**, job phải **idempotent**, và việc phân phối phải dùng Consumer Group. Stateless nghĩa là Worker không giữ trạng thái nghiệp vụ chỉ trong RAM giữa các job; job state nằm ở PostgreSQL, còn queue/progress phù hợp có thể ở Redis. Idempotent nghĩa là cùng message được xử lý lại vẫn chỉ tạo một kết quả nghiệp vụ canonical; `messageId`, unique constraint và fingerprint dùng để chống trùng. Consumer Group chia message giữa các Worker, còn pending recovery xử lý trường hợp Worker chết trước ACK.

### Nếu giảng viên hỏi sâu

- Consumer Group giảm xử lý đồng thời nhưng không thay thế idempotency.
- Cache cục bộ không nhất thiết vi phạm stateless nếu không phải nguồn sự thật và mất cache không làm sai nghiệp vụ.
- ACK chỉ nên thực hiện sau khi effect bền vững đã được ghi.
- Khi tăng Worker, phải theo dõi DB pool, lock contention, I/O và queue lag.

**Câu chốt:** Stateless giúp thay Worker được; idempotency giúp chạy lại được; Consumer Group giúp chia tải được.

[Xem câu trả lời gốc](23_worker_stateless_idempotent/answer.md)

---

## Bản ôn siêu ngắn trước khi vào vấn đáp

| Nếu bị hỏi về… | Từ khóa phải nói được |
| --- | --- |
| Lý do kiến trúc | Driver → decision → trade-off → verification |
| Mở rộng tính năng | Port/contract, Plugin/Registry, Open–Closed |
| Tải lớn | Async Queue–Worker, batching, backpressure, scale ngang |
| Độ tin cậy | Transactional Outbox, at-least-once, idempotency, recovery |
| Dữ liệu realtime | Adapter, canonical model, reconnect, backfill, deduplicate |
| Ranh giới | Ownership, public API, không import `internal`, không đọc chéo bảng |
| Truy vết | Frozen Manifest, version, seed, checksum, fingerprint |
| Đọc nhanh | Top-K projection, CQRS-lite, revision |
| ML/Sentiment | Python runtime, isolation, model/input version, degraded mode |
| Điều chưa đo | Nói rõ **Planned**, đề xuất benchmark/chaos test; không bịa số liệu |

## Công thức xử lý câu hỏi phản biện

Khi giảng viên hỏi “tại sao không chọn cách khác?”, có thể trả lời theo bốn ý:

1. **Driver:** yêu cầu nào quan trọng nhất trong bối cảnh nhóm?
2. **Decision:** nhóm đã chọn pattern/tactic nào?
3. **Trade-off:** lợi ích và chi phí của lựa chọn đó là gì?
4. **Evidence:** code, test, ADR hoặc benchmark nào chứng minh; phần nào vẫn Planned?

Ví dụ: “Nhóm chọn Queue–Worker vì Backtest là tác vụ dài và cần scale. Lợi ích là API không block và có thể thêm Worker; chi phí là eventual consistency, duplicate và tracing phức tạp. Vì vậy nhóm bổ sung Outbox, idempotency, recovery và metric. Cơ chế đã có code/test, còn ngưỡng 100.000 phải benchmark để xác nhận.”
