# ADR-0009: Strategy Versioning và Reproducible Experiments

**Status**: Accepted
**Date**: 2026-08-11
**Owners**: Tiến Luật

## Context

Mỗi kết quả Backtest hoặc Top-K Leaderboard chỉ có ý nghĩa khi hệ thống biết chính xác nó được tạo ra từ:

- dữ liệu thị trường nào;
- Strategy và Composite Strategy version nào;
- bộ parameters nào;
- Backtest assumptions và fee nào;
- Search Algorithm, Stop Condition và random seed nào;
- Evaluation/Ranking formula version nào;
- Sentiment data/model version nào nếu có;
- phiên bản source code nào.

Ví dụ, `MA-RSI Strategy v1` có thể dùng `MA20/MA50/RSI14`, sau đó nhóm đổi thành `MA10/MA30/RSI21`. Nếu overwrite Strategy hoặc kết quả cũ, Experiment trên Leaderboard không còn giải thích hoặc chạy lại được.

Dữ liệu từ Binance cũng có thể được bổ sung, sửa hoặc tải theo thứ tự khác. Random Search chạy song song có thể hoàn thành candidate theo thứ tự khác nhau. Sentiment Model mới có thể trả nhãn khác cho cùng một News Item.

Đề bài yêu cầu trả lời được: “Một kết quả trên Leaderboard được tạo ra bởi Strategy version nào?” và nhấn mạnh khả năng tái lập Experiment.

Theo [ADR-0005: Strategy Contract và Plugin Registry](0005-strategy-plugin-registry.md), Strategy có ID/version/parameters. Theo [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md), job có thể retry và chạy trên nhiều Worker. Theo [ADR-0007: PostgreSQL/Supabase và Redis](0007-postgresql-redis-ownership.md), PostgreSQL là nguồn dữ liệu bền vững.

## Decision

### 1. Experiment là cấu hình bất biến

Module `experiment` sở hữu Experiment Manifest, runtime status và reproduction metadata. Các module `search`, `backtesting`, `evaluation` và `leaderboard` chỉ sở hữu dữ liệu nghiệp vụ của bước tương ứng và liên kết về `experimentId`.

Khi Experiment bắt đầu, hệ thống tạo một **Experiment Manifest bất biến**. Sau khi manifest được xác nhận, không cập nhật trực tiếp các field cấu hình.

Nếu người dùng thay đổi Strategy, dataset, fee hoặc Search configuration, hệ thống tạo Experiment mới và có thể ghi `derivedFromExperimentId`.

Experiment status/progress được cập nhật riêng, nhưng manifest đầu vào không thay đổi.

```text
Experiment
├── Immutable Manifest
├── Mutable Runtime Status
├── Candidate Definitions
├── Execution Attempts
└── Immutable Results
```

### 2. Experiment Manifest

Manifest tối thiểu lưu:

| Nhóm       | Dữ liệu bắt buộc                                                              |
| ---------- | ----------------------------------------------------------------------------- |
| Identity   | `experimentId`, `createdAt`, `createdBy` hoặc demo actor                      |
| Dataset    | ID, version, checksum, provider, pair, timeframe, start/end, candle count     |
| Strategy   | Plugin ID, version và exact parameters                                        |
| Composite  | Policy ID/version, Strategy con và parameters/weights                         |
| Backtest   | Initial capital, fee, execution price rule, position mode, slippage rule      |
| Evaluation | Metric implementation/version và Ranking formula/version                      |
| Search     | Algorithm ID/version, Search Space, random seed, Stop Conditions và Top-K     |
| Sentiment  | Dataset/content hash, model version và preprocessing version nếu được sử dụng |
| Software   | Application version, Git commit SHA và contract version                       |

Ví dụ rút gọn:

```json
{
  "experimentId": "01J...",
  "manifestVersion": 1,
  "dataset": {
    "datasetId": "btc-5m-2026-01",
    "version": "1",
    "checksum": "sha256:...",
    "provider": "BINANCE",
    "pair": "BTC/USDT",
    "timeframe": "5m",
    "startTime": "2026-01-01T00:00:00Z",
    "endTime": "2026-01-31T23:55:00Z",
    "candleCount": 8928
  },
  "strategy": {
    "pluginId": "ma-crossover",
    "version": "1.0.0",
    "parameters": {
      "fastPeriod": 20,
      "slowPeriod": 50
    }
  },
  "backtest": {
    "initialCapital": "10000.00",
    "feeRate": "0.001",
    "executionPrice": "CANDLE_CLOSE",
    "positionMode": "LONG_ONLY_ONE_POSITION",
    "slippageRate": "0"
  },
  "search": {
    "algorithmId": "random-search",
    "algorithmVersion": "1.0.0",
    "randomSeed": 20260811,
    "maxCandidates": 100,
    "topK": 10
  },
  "software": {
    "applicationVersion": "0.1.0",
    "gitCommit": "abcdef1"
  }
}
```

### 3. Strategy versioning

Strategy được định danh bằng:

```text
pluginId + strategyVersion + exact parameters
```

Quy tắc:

1. `pluginId` ổn định và không phụ thuộc Java class name.
2. Thay đổi logic có thể làm tín hiệu khác phải tạo Strategy version mới.
3. Không overwrite Strategy Definition/version đã được Experiment sử dụng.
4. Thay default parameter không thay đổi parameters của Experiment cũ.
5. Registry phải resolve được version mà Experiment tham chiếu hoặc báo rõ artifact không còn khả dụng.
6. Strategy regression fixture lưu chuỗi tín hiệu mong đợi theo version.

Sử dụng Semantic Versioning cho Strategy plugin:

- MAJOR: breaking change contract/meaning;
- MINOR: thêm khả năng tương thích;
- PATCH: sửa implementation không được làm thay đổi quyết định dự kiến; nếu output thay đổi, phải đánh giá tăng version phù hợp.

### 4. Composite Strategy versioning

Composite Definition bất biến và lưu:

- Composite ID/version;
- Combination Policy ID/version;
- danh sách Strategy con;
- ID, version và exact parameters của mỗi Strategy con;
- weight/threshold nếu policy sử dụng;
- quy tắc tie, ví dụ Majority Vote hòa thì HOLD.

Không chỉ lưu tên như `MA + RSI`. Hai Composite cùng tên nhưng khác version/parameters là hai định nghĩa khác nhau.

### 5. Dataset bất biến

Backtest không tải lại dữ liệu hiện tại từ Binance khi reproduce. Nó sử dụng Dataset đã freeze.

Dataset Manifest lưu:

| Field                  | Ý nghĩa                                |
| ---------------------- | -------------------------------------- |
| `datasetId`            | ID ổn định                             |
| `datasetVersion`       | Version của snapshot                   |
| `provider`             | Nguồn dữ liệu ban đầu                  |
| `pair/timeframe`       | Market scope                           |
| `startTime/endTime`    | Khoảng lịch sử                         |
| `candleCount`          | Số Candle thực tế                      |
| `checksum`             | Hash của canonical ordered Candle data |
| `createdAt`            | Thời điểm freeze                       |
| `normalizationVersion` | Version quy tắc ánh xạ/chuẩn hóa       |

Canonical checksum được tính trên Candle:

- sắp xếp tăng dần theo `openTime`;
- timestamp UTC;
- decimal ở canonical string form;
- chỉ chứa field domain đã quy định;
- không phụ thuộc thứ tự JSON object field.

Dataset đã được Experiment sử dụng không được sửa tại chỗ. Nếu provider bổ sung hoặc sửa Candle, tạo Dataset version mới với checksum mới.

Có thể tránh copy toàn bộ Candle bằng immutable dataset membership/reference, nhưng hệ thống phải đảm bảo Candle revision được tham chiếu không bị overwrite.

### 6. Backtest assumptions

Mỗi Experiment lưu rõ assumptions thay vì phụ thuộc default hiện tại của code.

MVP mặc định:

| Assumption                 | Giá trị                                             |
| -------------------------- | --------------------------------------------------- |
| Position mode              | Long-only                                           |
| Concurrent position        | Tối đa một position                                 |
| Entry/Exit execution       | Giá đóng cửa Candle tạo tín hiệu                    |
| Trading fee                | Fixed rate được lưu trong manifest                  |
| Slippage                   | `0` nếu chưa mô phỏng, vẫn phải lưu rõ              |
| Initial capital            | Decimal được lưu trong manifest                     |
| Open position cuối dataset | Policy phải được ghi rõ trong feature spec/manifest |

Thay một assumption tạo Experiment mới. Backtester không đọc default mới để chạy lại Experiment cũ.

### 7. Evaluation và Ranking version

Metrics lưu cả value và implementation/version:

- Total Return;
- Win Rate;
- Maximum Drawdown;
- Number of Trades;
- metrics mở rộng nếu có.

Leaderboard entry lưu:

- Evaluation Result ID;
- Ranking Formula ID/version;
- exact score;
- Top-K revision;
- tie-break result.

Ranking phải deterministic. Khi hai candidate có cùng score, MVP dùng tie-break theo thứ tự ổn định đã lưu, ví dụ:

```text
score giảm dần
→ generationIndex tăng dần
→ candidateId tăng dần
```

Không dùng thứ tự Worker hoàn thành làm tie-break.

### 8. Random Search reproducibility

Random Search lưu:

- Search Algorithm ID/version;
- pseudo-random generator name/version nếu cần;
- random seed;
- Search Space và parameter ranges;
- Strategy category/rule constraints;
- max candidates, max duration và no-improvement limit;
- Top-K;
- generation index của từng candidate.

Candidate generation được thực hiện theo thứ tự logic tập trung. Worker có thể hoàn thành khác thứ tự nhưng candidate definition và generation index không thay đổi.

`maxDuration` phụ thuộc thời gian thực nên chạy lại có thể tạo số candidate khác nếu dùng riêng. Để reproduce chính xác candidate set, hệ thống lưu danh sách Candidate Definition thực tế đã sinh. Replay ưu tiên danh sách đã lưu thay vì chạy lại time-based stop.

### 9. Candidate và Execution Attempt

Phân biệt:

- **Candidate Definition**: Strategy/Composite + parameters bất biến;
- **Execution Attempt**: một lần Worker thử chạy candidate;
- **Backtest Result**: kết quả nghiệp vụ thành công;
- **Evaluation Result**: metrics/score từ Backtest Result.

Retry tạo Execution Attempt mới hoặc tăng attempt record, không tạo Candidate Definition khác. Duplicate delivery cùng Job/Candidate không tạo Result trùng, theo [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md).

Mỗi attempt lưu Worker ID, started/finished time, status và error classification để audit, nhưng thông tin Worker không ảnh hưởng business result.

### 10. Result không bị overwrite

- Result thành công là immutable.
- Chạy lại để kiểm chứng tạo `Reproduction Run` mới liên kết `reproducesExperimentId` hoặc `reproducesResultId`.
- Kết quả mới được so sánh với expected result; không ghi đè result gốc.
- Nếu result khác, lưu mismatch report gồm trades/metrics/checksum khác nhau.
- Failed/cancelled attempt được giữ trạng thái và error summary, không biến thành success record.
- Không xóa Dataset/Strategy version còn được Experiment tham chiếu nếu chưa có retention policy an toàn.

### 11. Canonical Manifest và fingerprint

Hệ thống tạo `experimentFingerprint` bằng SHA-256 trên canonical manifest:

- JSON field được sắp xếp ổn định;
- decimal serialize dạng string chuẩn;
- timestamp UTC;
- array giữ thứ tự có ý nghĩa;
- bỏ field runtime như progress, Worker ID và completion time;
- include version của schema/contract.

Fingerprint giúp phát hiện hai Experiment có cùng input logic và xác nhận manifest không bị thay đổi sau khi tạo.

Fingerprint không thay thế `experimentId`: hai lần chạy có thể cùng fingerprint nhưng vẫn là hai Experiment riêng.

### 12. Software build và environment

Mỗi Experiment lưu tối thiểu:

- application/artifact version;
- Git commit SHA;
- Strategy, Backtester, Evaluation và Ranking version;
- Java/runtime version nếu ảnh hưởng kết quả;
- feature flags hoặc configuration có ảnh hưởng business logic.

Không cần lưu mọi biến môi trường. Chỉ lưu configuration có thể làm kết quả thay đổi; không lưu secret.

### 13. Sentiment reproducibility

Nếu Experiment sử dụng Sentiment:

- lưu News/Sentiment dataset ID/version;
- content hash hoặc normalized input reference;
- published time và analyzed time;
- model version và preprocessing version;
- label, confidence và polarity score đã dùng;
- chỉ sử dụng News có `publishedAt` không sau thời điểm Strategy đánh giá để tránh look-ahead bias.

`analyzedAt` được lưu để audit và truy vết model run, nhưng không phải điều kiện loại News khỏi dataset lịch sử; tính hợp lệ theo thời gian dựa trên `publishedAt`.

Experiment replay sử dụng Sentiment Result đã freeze, không gọi model mới. Quy tắc service/model thuộc [ADR-0008: Tách Sentiment Service](0008-sentiment-service-boundary.md).

### 14. API và UI truy vết

Chi tiết Backtest/Top-K phải hiển thị hoặc cho phép mở:

- Experiment ID;
- Strategy/Composite version và parameters;
- Dataset ID/version, range và checksum;
- Backtest assumptions;
- Search seed/configuration;
- metrics, score và Ranking version;
- software build/commit;
- hành động `Reproduce` hoặc tải Manifest khi được triển khai.

Frontend không tự ghép thông tin từ nhiều nguồn không version. Java API trả một Experiment Detail/read model thống nhất.

## Alternatives Considered

- **Chỉ lưu tên Strategy và metrics**: Ít dữ liệu nhưng không biết parameters, version, dataset hoặc assumptions đã tạo kết quả.
- **Overwrite Strategy/Result cũ**: Đơn giản CRUD nhưng phá vỡ audit và làm Leaderboard cũ không còn ý nghĩa.
- **Mỗi lần reproduce tải lại Binance data**: Dễ triển khai nhưng dữ liệu có thể thay đổi và không đảm bảo cùng input.
- **Chỉ lưu Docker image hoặc Git commit**: Giúp lấy lại code nhưng không chứa dataset, parameters, seed và runtime assumptions.
- **Serialize toàn bộ Java object**: Có thể restore nhanh nhưng phụ thuộc class layout, khó migrate và không phải contract bền vững.
- **Chỉ lưu random seed**: Không đủ khi Search dừng theo thời gian hoặc implementation/generator version thay đổi.
- **Lưu toàn bộ environment variables**: Quá nhiều và có nguy cơ lưu secret; chỉ lưu configuration ảnh hưởng kết quả.
- **Cho phép update Experiment manifest**: Thuận tiện sửa lỗi nhập liệu nhưng làm fingerprint/audit không còn đáng tin; thay đổi phải tạo Experiment mới.

## Consequences

### Positive

- Mỗi Leaderboard result truy ngược được Strategy, dataset, assumptions và code version.
- Có thể replay Backtest mà không gọi Binance hoặc model Sentiment mới.
- Strategy/version mới không làm mất ý nghĩa Result cũ.
- Random Search song song vẫn có candidate identity và ranking deterministic.
- Hỗ trợ audit, debug và giải thích kiến trúc khi demo.
- Phát hiện được input hoặc artifact bị thay đổi ngoài ý muốn bằng checksum/fingerprint.

### Negative

- Cần lưu nhiều metadata, version và immutable record hơn.
- Dataset snapshot/reference làm tăng storage và migration complexity.
- Phải duy trì Strategy/model/backtest artifact cũ hoặc có chính sách archive.
- Canonical serialization và checksum cần implementation/test cẩn thận.
- Reproduction tuyệt đối có thể khó nếu runtime/library cũ không còn khả dụng.
- Time-based Search không thể tái sinh candidate set chỉ bằng seed; phải lưu candidate đã tạo.

## Affected Components

- `modules/domain`
- `modules/strategy-core`
- `modules/combination`
- `modules/backtesting`
- `modules/evaluation`
- `modules/experiment`
- `modules/search`
- `modules/leaderboard`
- `modules/news`
- `modules/persistence`
- `apps/api`
- `apps/worker`
- `apps/sentiment`
- `apps/web`
- PostgreSQL/Supabase schema, migration và retention
- Demo dataset/configuration

## Validation

- Chạy cùng manifest hai lần và so sánh Trade sequence cùng bốn metrics bắt buộc.
- Tạo lại checksum từ Dataset và xác nhận khớp manifest trước khi Backtest.
- Thay một Candle và xác nhận checksum/fingerprint thay đổi hoặc mutation bị từ chối.
- Thay Strategy logic và xác nhận cần version mới; Experiment cũ vẫn resolve version cũ.
- Dùng cùng random seed/Search Space và xác nhận thứ tự Candidate Definition giống nhau.
- Chạy candidate trên một và nhiều Worker; xác nhận ranking không phụ thuộc completion order.
- Retry cùng Candidate Job và xác nhận không tạo Result/Evaluation trùng.
- Reproduce Experiment dùng `maxDuration` từ danh sách candidate đã lưu, không phụ thuộc tốc độ máy hiện tại.
- Thay Sentiment Model version và xác nhận Experiment cũ vẫn dùng frozen Sentiment Result.
- Click Top-K trên UI và truy ra được Experiment ID, Strategy version, Dataset và parameters.
- Export/import manifest và xác nhận `experimentFingerprint` không đổi.
- Xác nhận manifest hoặc log không chứa database password, API key hay secret.

## Risks and Mitigations

- **Risk**: Thành viên quên tăng Strategy/version khi thay logic.

  **Mitigation**: PR checklist, regression fixture và validation không cho sửa definition đã được tham chiếu.

- **Risk**: Dataset bị sửa hoặc xóa sau khi Experiment hoàn thành.

  **Mitigation**: Immutable dataset version, checksum, foreign-key/reference protection và retention aware dependency.

- **Risk**: Lưu snapshot làm database tăng nhanh.

  **Mitigation**: Deduplicate immutable Candle revisions, dataset membership/reference và retention policy theo usage.

- **Risk**: Canonical JSON khác nhau giữa Java/Python.

  **Mitigation**: Publish canonicalization rule, shared fixture và checksum contract test hai runtime.

- **Risk**: Cùng seed nhưng candidate khác do thay Search implementation.

  **Mitigation**: Lưu Search Algorithm version, PRNG metadata và Candidate Definition thực tế.

- **Risk**: Kết quả khác nhau do decimal/floating-point.

  **Mitigation**: Dùng decimal/BigDecimal cho price, fee và metrics quan trọng; lưu rounding policy/version.

- **Risk**: Artifact/version cũ không còn chạy được.

  **Mitigation**: Lưu Git commit/artifact version, pin dependencies và archive demo/release artifact quan trọng.

- **Risk**: Sentiment lịch sử dùng tin xuất bản sau thời điểm Backtest.

  **Mitigation**: Enforce published-time cutoff và versioned sentiment dataset để tránh look-ahead bias.

- **Risk**: Manifest chứa quá nhiều thông tin nhưng vẫn thiếu một config ảnh hưởng kết quả.

  **Mitigation**: Backtest/Evaluation configuration phải đi qua typed manifest; cấm đọc hidden global default trong execution.

## References

- [Đề bài Crypto StrategyLab](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Architecture Overview](../architecture/architecture-overview.md)
- [Data Model Overview](../architecture/data-model-overview.md)
- [Demo Data and Configuration](../demo/demo-data.md)
- [UI Stitch Guide](../ui/stitch-guide.md)
- [ADR-0001: Modular Monolith](0001-modular-monolith.md)
- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0003: Market Data Adapter](0003-market-data-adapter.md)
- [ADR-0005: Strategy Plugin Registry](0005-strategy-plugin-registry.md)
- [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md)
- [ADR-0007: PostgreSQL/Supabase và Redis](0007-postgresql-redis-ownership.md)
- [ADR-0008: Sentiment Service](0008-sentiment-service-boundary.md)

## Supersession

- Supersedes: None
- Superseded by: None
