# ADR-0005: Strategy Contract và Plugin Registry

**Status**: Accepted
**Date**: 2026-08-11
**Owners**: Strategy Owner và Tech Lead

## Context

Crypto StrategyLab phải có ít nhất bốn Strategy trong MVP:

- Moving Average;
- RSI;
- Bollinger Bands;
- Support/Resistance.

Trong tương lai, hệ thống có thể bổ sung MACD, SMC, Wyckoff hoặc Sentiment Strategy. Yêu cầu kiến trúc quan trọng là thêm Strategy mới với ảnh hưởng tối thiểu đến Backtester, Composite Engine, Search, Evaluation, Leaderboard và Frontend.

Cách viết chuỗi điều kiện như sau không đáp ứng yêu cầu:

```java
if (type == MA) {
    // ...
} else if (type == RSI) {
    // ...
} else if (type == BOLLINGER) {
    // ...
}
```

Mỗi lần thêm Strategy, cách này buộc phải sửa nhiều nơi và dễ tạo logic không nhất quán. Strategy cũng không được tự gọi Binance, database hoặc Spring Service vì như vậy kết quả Backtest không còn độc lập và dễ tái lập.

Theo [ADR-0001: Sử dụng Modular Monolith](0001-modular-monolith.md) và [ADR-0002: Ranh giới giữa các Module](0002-module-boundaries.md), Strategy cần một contract thuần Java và một registry để các consumer làm việc qua abstraction.

## Decision

### 1. Strategy là contract thuần Java

Module `strategy-core` định nghĩa contract khái niệm:

```java
public interface Strategy {
    StrategyDecision analyze(StrategyContext context);
}
```

`Strategy` không phụ thuộc:

- Spring Framework;
- Binance hoặc Market Data Provider;
- PostgreSQL, Supabase hoặc Redis;
- Controller, WebSocket hoặc Frontend;
- Backtester, Search hoặc Leaderboard implementation.

Strategy chỉ nhận input đã chuẩn hóa và trả quyết định chuẩn hóa.

### 2. Strategy Context

`StrategyContext` là immutable input, có thể chứa:

| Dữ liệu | Ý nghĩa |
|---|---|
| `pair` | Cặp giao dịch chuẩn như `BTC/USDT` |
| `timeframe` | Khung thời gian đang phân tích |
| `candles` | Danh sách Candle đã sắp xếp tăng dần theo thời gian |
| `evaluationTime` | Thời điểm Candle đang được đánh giá |
| `indicatorData` | Indicator đã tính sẵn nếu use case cung cấp |
| `marketState` | Context thị trường tùy chọn |
| `sentimentData` | Sentiment tùy chọn cho Strategy tương lai |

Context không cho Strategy quyền gọi ngược ra provider hoặc database. Nếu thiếu dữ liệu cần thiết, Strategy trả lỗi domain chuẩn hoặc quyết định không đủ dữ liệu theo contract được chốt trong feature plan.

### 3. Strategy Decision

Kết quả phân tích sử dụng ba tín hiệu chuẩn:

```text
BUY
SELL
HOLD
```

`StrategyDecision` tối thiểu gồm:

| Field | Ý nghĩa |
|---|---|
| `signal` | BUY, SELL hoặc HOLD |
| `occurredAt` | Candle time tạo ra quyết định |
| `strategyRef` | Strategy ID và version |
| `reason` | Mô tả ngắn phục vụ debug/demo |
| `evidence` | Giá trị indicator/vùng giá cần cho visualization, nếu có |

`reason` và `evidence` không chứa UI markup. Frontend quyết định cách hiển thị thông qua API contract.

MVP dùng BUY/SELL/HOLD. Việc diễn giải BUY/SELL thành mở hoặc đóng position thuộc Backtester và backtest assumptions, không nằm trong Strategy.

### 4. Strategy Plugin

Mỗi Strategy type được cung cấp dưới dạng một `StrategyPlugin`:

```java
public interface StrategyPlugin {
    StrategyDescriptor descriptor();

    Strategy create(StrategyParameterValues parameters);
}
```

`StrategyDescriptor` mô tả:

| Field | Ví dụ | Mục đích |
|---|---|---|
| `id` | `ma-crossover` | ID ổn định, không phụ thuộc tên class |
| `version` | `1.0.0` | Tái lập Experiment cũ |
| `displayName` | `MA Crossover` | Hiển thị trên UI |
| `category` | `TREND` | Hỗ trợ Search theo domain trong tương lai |
| `description` | Mô tả ngắn | Giúp người dùng hiểu Strategy |
| `parameterSchema` | fast/slow period | Sinh form và Search Space |
| `requiredLookback` | 30 candles | Kiểm tra dataset đủ dữ liệu |

`StrategyPlugin.create()` phải:

- validate đầy đủ parameters;
- từ chối field không hợp lệ hoặc không được hỗ trợ;
- tạo Strategy instance immutable;
- không tự đọc configuration từ database hoặc biến toàn cục.

### 5. Parameter Schema

Parameter không được hard-code riêng trong Frontend hoặc Search. Mỗi plugin công bố schema có:

- tên parameter;
- kiểu dữ liệu;
- required/default value;
- min/max hoặc allowed values;
- mô tả;
- constraint liên quan giữa nhiều parameter.

Ví dụ MA Crossover:

```text
fastPeriod: integer, min 2, max 200
slowPeriod: integer, min 3, max 500
constraint: fastPeriod < slowPeriod
```

Schema được dùng để:

- Backend validate request;
- Frontend dựng form cấu hình chung;
- Random Search biết khoảng parameter hợp lệ;
- lưu exact parameters của Experiment.

Validation ở Backend là bắt buộc ngay cả khi Frontend đã validate.

### 6. Strategy Registry

`StrategyRegistry` thuộc module `strategy-core` và cung cấp:

```java
public interface StrategyRegistry {
    List<StrategyDescriptor> listAvailable();

    StrategyDescriptor getDescriptor(StrategyReference reference);

    Strategy create(StrategyReference reference, StrategyParameterValues parameters);
}
```

Registry dùng khóa:

```text
strategyId + strategyVersion
```

Quy tắc:

1. Registry từ chối hai plugin trùng ID và version ngay khi application startup.
2. Consumer không dùng `switch`, `if/else` hoặc reflection theo tên class để tạo Strategy.
3. Frontend lấy danh sách Strategy và parameter schema từ API đọc Registry, không hard-code danh sách.
4. Search lấy Search Space từ descriptor của Registry.
5. Backtester chỉ nhận `Strategy`, không biết plugin hoặc implementation cụ thể.
6. Experiment lưu Strategy ID, version và parameters chính xác theo [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md).

### 7. Cách đăng ký plugin

MVP sử dụng **compile-time trusted plugins**:

- Strategy implementation nằm trong `modules/strategies`;
- application composition root cung cấp danh sách `StrategyPlugin` cho Registry qua Dependency Injection;
- plugin là plain Java; Spring configuration nằm ở composition layer, không nằm trong Strategy logic;
- thêm plugin yêu cầu build và deploy lại application.

Thêm `MACDStrategy` tối thiểu cần:

1. implement Strategy;
2. tạo `MACDStrategyPlugin` với descriptor và parameter schema;
3. thêm unit test và contract test;
4. đăng ký plugin tại composition root.

Không sửa Backtester, Evaluation, Leaderboard, Composite Engine hoặc Search Generator.

MVP không hỗ trợ upload JAR, tải plugin động lúc runtime hoặc chạy Strategy script không tin cậy.

### 8. Strategy implementation trong MVP

| Plugin ID | Category | Parameters chính |
|---|---|---|
| `ma-crossover` | `TREND` | `fastPeriod`, `slowPeriod` |
| `rsi-threshold` | `MOMENTUM` | `period`, `buyThreshold`, `sellThreshold` |
| `bollinger-bands` | `VOLATILITY` | `period`, `standardDeviation` và rule mode |
| `support-resistance` | `STRUCTURE` | `lookback`, tolerance và rule mode |

Tên, default value và rule chi tiết sẽ được chốt trong Strategy/Backtest feature spec. ADR này chỉ chốt cơ chế contract và plugin.

### 9. Composite Strategy

Composite Strategy triển khai cùng interface `Strategy`, vì vậy Backtester không cần phân biệt Strategy đơn và Composite Strategy.

```text
CompositeStrategy
    ├── MA Strategy
    ├── RSI Strategy
    └── Support/Resistance Strategy
```

MVP sử dụng `MajorityVotePolicy`:

- Strategy con trả BUY, SELL hoặc HOLD;
- tín hiệu có số phiếu cao nhất được chọn;
- trường hợp hòa trả HOLD;
- Composite phải có tối thiểu hai Strategy con;
- thứ tự Strategy con không được làm thay đổi kết quả.

Combination policy là abstraction riêng để sau này có thể thêm Weighted Combination mà không sửa Strategy implementation hoặc Backtester.

Composite descriptor/version phải ghi lại:

- Combination Policy và version;
- danh sách Strategy con;
- version và parameters của từng Strategy con;
- threshold/weight nếu policy có sử dụng.

### 10. Determinism và lifecycle

Strategy phải deterministic trong cùng điều kiện:

```text
same Strategy version
+ same parameters
+ same ordered input data
= same Strategy decisions
```

Quy tắc:

- Strategy instance immutable và không giữ mutable state dùng chung giữa Backtest;
- không đọc system clock trực tiếp; dùng `evaluationTime` từ context;
- không tự sinh random value;
- không gọi network hoặc database;
- không thay đổi input Candle;
- implementation phải thread-safe hoặc được tạo instance riêng cho từng execution;
- mọi thay đổi làm biến đổi kết quả phải tạo Strategy version mới.

## Alternatives Considered

- **Hard-coded `if/else` hoặc `switch` theo Strategy type**: Dễ bắt đầu nhưng mỗi Strategy mới buộc sửa factory, engine và có thể cả UI/Search.
- **Một abstract base class lớn cho mọi Strategy**: Chia sẻ code nhanh nhưng ép các Strategy khác bản chất vào cùng inheritance hierarchy và dễ tạo base class phình to.
- **Spring component scanning trực tiếp trong Strategy implementation**: Tự động đăng ký nhưng làm Strategy phụ thuộc framework; quyết định này giữ Spring ở composition layer.
- **Java `ServiceLoader`**: Giữ plugin độc lập Spring và giảm đăng ký thủ công, nhưng tăng cấu hình packaging/debug cho MVP; có thể xem xét sau.
- **Runtime JAR/plugin hot-loading**: Linh hoạt hơn nhưng cần classloader isolation, compatibility và security; vượt phạm vi đồ án.
- **Cho người dùng viết Strategy script**: Mở rộng mạnh nhưng cần sandbox, resource limit và bảo mật mã không tin cậy; không thuộc MVP.
- **Hard-code form parameters trong Frontend**: Làm UI nhanh nhưng thêm Strategy mới vẫn phải sửa Frontend; descriptor/schema từ Backend phù hợp hơn.

## Consequences

### Positive

- Thêm Strategy mới không yêu cầu sửa Backtester, Evaluation hoặc Leaderboard.
- Frontend và Search có thể đọc metadata/parameter schema thay vì hard-code từng Strategy.
- Strategy dễ unit test bằng Candle fixture, không cần Spring, Binance hoặc database.
- Strategy đơn và Composite dùng chung một contract.
- Version và exact parameters hỗ trợ tái lập Experiment.
- Category metadata tạo nền tảng cho Domain-guided Search trong tương lai.

### Negative

- Cần thêm descriptor, parameter schema và registration cho mỗi Strategy.
- Generic parameter representation cần mapper sang typed configuration của từng plugin.
- Compile-time plugin yêu cầu rebuild/redeploy khi thêm Strategy.
- Registry và schema cần quản lý compatibility theo version.
- `reason/evidence` phải được giữ ổn định để không biến thành cấu trúc tùy tiện.

## Affected Components

- `modules/domain`
- `modules/contracts`
- `modules/strategy-core`
- `modules/strategies`
- `modules/combination`
- `modules/backtesting`
- `modules/evaluation`
- `modules/search`
- `apps/api`
- `apps/worker`
- `apps/web`
- Strategy API và Experiment persistence

## Validation

- Viết contract test chạy chung cho cả bốn Strategy plugin MVP.
- Registry fail startup khi hai plugin có cùng ID và version.
- Parameter validation từ chối MA có `fastPeriod >= slowPeriod`.
- Cùng dataset, version và parameters phải sinh cùng chuỗi quyết định.
- Thêm plugin thử nghiệm `MACDStrategy` và xác nhận không sửa Backtester, Evaluation, Leaderboard hoặc Search Generator.
- API liệt kê plugin MACD và parameter schema mà Frontend không cần hard-code card/form mới.
- Backtester chạy được Strategy đơn và Composite Strategy qua cùng interface.
- Majority Vote trả HOLD khi số phiếu hòa.
- ArchUnit xác nhận Strategy không phụ thuộc Spring, Market Data Adapter hoặc Persistence.
- Chạy nhiều Backtest song song và xác nhận Strategy không chia sẻ mutable state.

## Risks and Mitigations

- **Risk**: `StrategyContext` trở thành object quá lớn chứa mọi dữ liệu hệ thống.

  **Mitigation**: Chỉ thêm dữ liệu có use case rõ; context sử dụng immutable view và tách sub-context khi cần.
- **Risk**: Parameter schema và validation implementation không đồng nhất.

  **Mitigation**: Plugin là nguồn sự thật duy nhất; cùng validator được dùng cho API, Search và khi restore Experiment.
- **Risk**: Strategy version bị quên cập nhật khi logic thay đổi.

  **Mitigation**: PR checklist yêu cầu đánh giá version; regression fixture lưu expected decisions theo version.
- **Risk**: Plugin lỗi làm hỏng toàn bộ Search loop.

  **Mitigation**: Validate khi tạo instance, cô lập candidate failure, ghi error và tiếp tục theo job policy.
- **Risk**: Evidence tùy ý làm contract khó version.

  **Mitigation**: Dùng các evidence type chuẩn hóa; dữ liệu chỉ để debug không đưa vào public contract nếu chưa cần.
- **Risk**: Composition root trở thành danh sách đăng ký dài.

  **Mitigation**: Nhóm plugin theo module configuration; chỉ chuyển sang ServiceLoader khi số plugin đủ lớn để có lợi ích rõ.
- **Risk**: Strategy instance chứa mutable cache và gây sai kết quả khi chạy song song.

  **Mitigation**: Ưu tiên immutable/stateless; contract test concurrency và tạo instance theo execution nếu cần.

## References

- [Đề bài Crypto StrategyLab](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Module View](../architecture/module-view.md)
- [Data Model Overview](../architecture/data-model-overview.md)
- [UI Stitch Guide](../ui/stitch-guide.md)
- [ADR-0001: Modular Monolith](0001-modular-monolith.md)
- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0003: Market Data Adapter](0003-market-data-adapter.md)
- [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md)
- [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md)

## Supersession

- Supersedes: None
- Superseded by: None
