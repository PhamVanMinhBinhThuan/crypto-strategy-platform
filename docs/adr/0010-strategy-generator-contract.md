# ADR-0010: Strategy Generator Contract

**Status**: Proposed
**Date**: 2026-08-13
**Owners**: Tiến Luật

## Context

MVP sử dụng Random Search để sinh các Strategy hoặc Composite Strategy candidate. Trong tương lai, nhóm có thể muốn thay cách sinh candidate bằng:

- Domain-guided Search dựa trên category và quy tắc nghiệp vụ;
- Genetic Search sử dụng population, mutation và crossover;
- generator cố định dùng cho test hoặc demo;
- thuật toán khác chưa biết tại thời điểm thiết kế.

Nếu Search Coordinator tự chứa logic Random hoặc dùng trực tiếp class `Random`, việc thay thuật toán sẽ buộc sửa luồng queue, Backtest, Evaluation và Leaderboard. Điều này trái với mục tiêu thay Search Algorithm với ảnh hưởng tối thiểu.

Theo [ADR-0002: Ranh giới giữa các Module](0002-module-boundaries.md), contract nghiệp vụ phải nằm trong module sở hữu, không đặt trong `modules/contracts`. Theo [ADR-0005: Strategy Plugin Registry](0005-strategy-plugin-registry.md), Strategy Registry là nguồn cung cấp Strategy descriptor và parameter schema. Theo [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md), Search chỉ sinh candidate và điều phối job; Backtest, Evaluation và Ranking là các bước độc lập.

## Decision

### 1. Định nghĩa `StrategyGenerator` trong module `search`

`StrategyGenerator` là contract nghiệp vụ thuần Java do module `search` sở hữu:

```java
public interface StrategyGenerator {
    GeneratorDescriptor descriptor();

    GenerationBatch generate(GenerationRequest request);
}
```

Contract được công khai qua public API của `search`, ví dụ:

```text
modules/search/api/StrategyGenerator.java
```

`StrategyGenerator` không nằm trong `modules/contracts`, vì nó không phải HTTP DTO, WebSocket event hoặc queue message.

### 2. Trách nhiệm của Generator

Generator chỉ chịu trách nhiệm:

- chọn Strategy hoặc Composite Strategy hợp lệ;
- sinh bộ parameter từ Search Space;
- áp dụng constraint khi sinh candidate;
- sử dụng seed/state đầu vào để hỗ trợ tái lập;
- trả Candidate Definition và state kế tiếp.

Generator không được:

- chạy Backtest;
- tính Return, Win Rate, Drawdown hoặc Trades;
- tính Ranking hoặc cập nhật Top-K;
- ghi PostgreSQL, Redis hoặc publish queue message;
- gọi Binance, Sentiment Service hoặc Frontend;
- tự quyết định Experiment đã hoàn thành.

### 3. Input của Generator

`GenerationRequest` tối thiểu chứa:

| Field | Ý nghĩa |
| --- | --- |
| `experimentId` | Experiment đang yêu cầu candidate |
| `algorithmId/version` | Thuật toán Generator được chọn |
| `randomSeed` | Seed gốc của Experiment |
| `generationIndex` | Vị trí logic của batch/candidate tiếp theo |
| `searchSpace` | Strategy được phép, parameter range và composite rule |
| `constraints` | Ràng buộc như `fastPeriod < slowPeriod` |
| `generatorState` | State bất biến từ lần generate trước, nếu thuật toán cần |
| `observations` | Feedback chuẩn hóa từ candidate trước, nếu thuật toán cần |
| `batchSize` | Số candidate tối đa cần sinh trong lần gọi |

`observations` là model của module `search`, chỉ chứa dữ liệu tối thiểu như Candidate reference, score và trạng thái. Nó không sử dụng entity hoặc DTO nội bộ của `evaluation` hay `leaderboard`.

Random Generator có thể bỏ qua `observations`. Domain-guided hoặc Genetic Generator có thể sử dụng chúng.

### 4. Output của Generator

`GenerationBatch` trả về:

| Field | Ý nghĩa |
| --- | --- |
| `candidates` | Danh sách Candidate Definition bất biến |
| `nextGeneratorState` | State dùng cho lần generate kế tiếp |
| `diagnostics` | Thông tin debug không ảnh hưởng kết quả nghiệp vụ |

Mỗi Candidate Definition tối thiểu lưu:

- Strategy/Composite ID và version;
- exact parameters;
- Combination Policy và version nếu là Composite;
- Strategy con cùng version/parameters;
- generation index;
- generator ID/version;
- seed hoặc state reference cần cho tái lập.

Generator không tự tạo Backtest Job. Search Coordinator nhận Candidate Definition, kiểm tra trùng, persist qua output port và mới yêu cầu tạo job.

### 5. Generator Registry

Module `search` cung cấp registry để chọn generator theo:

```text
generatorId + generatorVersion
```

Ví dụ:

| Generator ID | Phạm vi |
| --- | --- |
| `random-search` | Bắt buộc trong MVP |
| `domain-guided-search` | Có thể bổ sung sau |
| `genetic-search` | Ngoài MVP, có thể bổ sung sau |
| `fixture-generator` | Test và demo |

Registry từ chối trùng ID/version khi application khởi động. Search Coordinator không dùng `if/else` hoặc `switch` theo tên thuật toán.

### 6. Stop Condition không thuộc Generator

Search Coordinator sở hữu Stop Condition và kiểm tra:

- `maxCandidates`;
- `maxDuration`;
- `maxIterationsWithoutImprovement`;
- yêu cầu dừng thủ công;
- giới hạn queued/running job.

Generator chỉ sinh tối đa `batchSize` candidate khi được gọi. Generator không chạy vòng lặp vô hạn và không tự enqueue job.

### 7. Validation và Strategy Registry

Generator sử dụng descriptor/parameter schema từ `StrategyRegistry` thông qua public contract của `strategy-core`.

Quy tắc:

1. Không hard-code parameter schema khác với Strategy Plugin.
2. Candidate phải được validate trước khi persist hoặc enqueue.
3. Candidate không hợp lệ bị loại hoặc trả lỗi có cấu trúc; không gửi sang Backtester.
4. Constraint giữa nhiều parameter phải dùng cùng rule mà API và Strategy Plugin sử dụng.
5. Generator không được tạo Strategy bằng Java class name hoặc reflection tùy ý.

### 8. Determinism và reproducibility

Với cùng:

```text
generator ID/version
+ seed
+ search space/constraints
+ generation index
+ generator state
+ ordered observations
```

Generator phải tạo cùng Candidate Definition theo cùng thứ tự logic.

Worker hoàn thành khác thứ tự không được làm thay đổi identity của candidate đã sinh. Search Coordinator sắp xếp feedback theo thứ tự ổn định trước khi gọi Generator.

Experiment lưu generator ID/version, seed, Search Space, Candidate Definition thực tế và state cần thiết theo [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md).

### 9. Luồng sử dụng

```text
Search Coordinator
        ↓ tạo GenerationRequest
StrategyGenerator Registry
        ↓ chọn generator theo ID/version
StrategyGenerator
        ↓ trả GenerationBatch
Search Coordinator
        ↓ validate + persist Candidate Definition
Queue Backtest Job
        ↓
Backtest → Evaluation → Leaderboard
```

Thay `RandomStrategyGenerator` bằng generator khác không làm thay đổi Backtester, Evaluator, Ranking hoặc queue job contract.

## Alternatives Considered

- **Viết Random Search trực tiếp trong Search Coordinator**: Nhanh cho MVP nhưng làm Coordinator phụ thuộc thuật toán cụ thể và khó thay generator.
- **Mỗi Generator tự chạy toàn bộ Backtest/Evaluation**: Giảm số abstraction nhưng trộn generation với execution và tạo coupling mạnh.
- **Đặt `StrategyGenerator` trong `modules/contracts`**: Dễ dùng chung nhưng sai boundary; đây là contract nghiệp vụ nội bộ của `search`, không phải integration DTO.
- **Mỗi thuật toán có queue/service riêng**: Cho phép scale riêng nhưng tăng runtime và vận hành không cần thiết cho MVP.
- **Chỉ thiết kế contract cho Random Search**: Đơn giản hơn nhưng không chứng minh được khả năng thay Search Algorithm theo yêu cầu kiến trúc.

## Consequences

### Positive

- Có thể thay thuật toán sinh candidate mà không sửa Backtest, Evaluation hoặc Leaderboard.
- Random Search MVP vẫn đơn giản nhưng có đường mở rộng rõ ràng.
- Search Space và Strategy parameter schema có một nguồn sự thật.
- Candidate generation có thể tái lập bằng version, seed và state.
- Có thể test Generator độc lập mà không cần database, Redis hoặc Spring.

### Negative

- Cần thêm request/result/state model và Generator Registry.
- Domain-guided hoặc Genetic Generator cần chuẩn hóa feedback/state cẩn thận.
- Determinism khó hơn nếu thuật toán sử dụng population hoặc chạy song song.
- Việc đổi generator contract phải quản lý version và compatibility.

## Affected Components

- `modules/search`
- `modules/strategy-core`
- `modules/experiment`
- `apps/api`
- `apps/worker`
- Search configuration và Experiment Manifest

## Validation

- Chạy `RandomStrategyGenerator` hai lần với cùng input và xác nhận Candidate Definition cùng thứ tự.
- Đổi seed và xác nhận candidate set thay đổi.
- Thêm `FixtureStrategyGenerator` mà không sửa Backtester, Evaluator hoặc Leaderboard.
- Registry từ chối hai Generator trùng ID/version.
- Candidate vi phạm parameter constraint bị từ chối trước khi enqueue.
- ArchUnit xác nhận `search` không phụ thuộc implementation của `backtesting`, `evaluation` hoặc `leaderboard`.
- ArchUnit xác nhận `StrategyGenerator` không nằm trong hoặc phụ thuộc `modules/contracts`.
- Xác nhận Stop Condition nằm ở Search Coordinator và Generator không tạo vòng lặp vô hạn.
- Lưu generator ID/version, seed và Candidate Definition trong Experiment Manifest.

## Risks and Mitigations

- **Risk**: Contract chỉ phù hợp Random Search, không đủ cho Genetic Search.

  **Mitigation**: Cho phép input có ordered observations và versioned generator state nhưng không triển khai Genetic Search trong MVP.

- **Risk**: `generatorState` trở thành object tùy ý khó lưu và version.

  **Mitigation**: State phải có schema/version, canonical serialization và chỉ chứa dữ liệu cần tiếp tục thuật toán.

- **Risk**: Generator tạo candidate trùng.

  **Mitigation**: Search Coordinator tạo fingerprint, kiểm tra trùng và giới hạn số lần thử sinh lại.

- **Risk**: Feedback theo thứ tự Worker hoàn thành làm kết quả không deterministic.

  **Mitigation**: Persist observation và sắp xếp theo generation index/candidate ID trước khi gọi Generator.

- **Risk**: Strategy parameter schema và Search Space không đồng nhất.

  **Mitigation**: Strategy Plugin descriptor là nguồn sự thật và Candidate phải qua cùng validator.

## References

- [ADR-0001: Modular Monolith](0001-modular-monolith.md)
- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0005: Strategy Plugin Registry](0005-strategy-plugin-registry.md)
- [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md)
- [ADR-0009: Reproducible Experiments](0009-reproducible-experiments.md)

## Supersession

- Supersedes: None
- Superseded by: None
