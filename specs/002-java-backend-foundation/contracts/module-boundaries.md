# Module Boundary Contract

## Projects

| Project | Base package | Allowed direct project dependencies |
|---|---|---|
| `:modules:domain` | `com.cryptostrategy.platform.domain` | none |
| `:modules:contracts` | `com.cryptostrategy.platform.contracts` | `domain` only when contract uses a stable domain value |
| `:modules:market-data` | `com.cryptostrategy.platform.marketdata` | `domain` |
| `:modules:strategy-core` | `com.cryptostrategy.platform.strategy` | `domain` |
| `:modules:strategies` | `com.cryptostrategy.platform.strategies` | `domain`, `strategy-core` |
| `:modules:combination` | `com.cryptostrategy.platform.combination` | `domain`, `strategy-core` |
| `:modules:backtesting` | `com.cryptostrategy.platform.backtesting` | `domain`, `strategy-core` |
| `:modules:evaluation` | `com.cryptostrategy.platform.evaluation` | `domain` |
| `:modules:experiment` | `com.cryptostrategy.platform.experiment` | `domain` |
| `:modules:search` | `com.cryptostrategy.platform.search` | `domain`, `strategy-core` |
| `:modules:leaderboard` | `com.cryptostrategy.platform.leaderboard` | `domain`, public `evaluation` API when needed |
| `:modules:news` | `com.cryptostrategy.platform.news` | `domain` |
| `:modules:persistence` | `com.cryptostrategy.platform.persistence` | `domain`, public output ports owned by capabilities |
| `:apps:api` | `com.cryptostrategy.platform.api` | capability public APIs, `contracts`, `persistence` |
| `:apps:worker` | `com.cryptostrategy.platform.worker` | required capability public APIs, `contracts`, `persistence` |

F-002 tạo project/package skeleton nhưng không cần khai báo mọi dependency được phép nếu
chưa có consumer. Feature sau chỉ được thêm dependency nằm trong bảng; dependency ngoài
bảng cần review ADR-0002 và không được merge cho tới khi quyết định liên quan đã được
ghi nhận trong ADR `Accepted`.

## Package rules

- `..api..` là public capability boundary.
- `..internal..` chỉ module owner được truy cập.
- Capability không phụ thuộc `apps.*`, controller/transport DTO hoặc persistence adapter.
- `domain`, `strategy-core`, `strategies` và `evaluation` không phụ thuộc Spring,
  provider client hoặc persistence technology.
- Không có dependency cycle giữa capability.
- `contracts` chỉ chứa integration DTO/message có version, không chứa business service.
- `persistence` implement output port; capability không import ngược adapter.
- Public/domain boundary dùng UUID cho identity, exact decimal thay binary floating-point
  và UTC instant thay local date-time không có timezone.

## Required evidence

- Mọi project xuất hiện trong Gradle project list và root `check`.
- Positive fixture chứng minh allowed dependency pass.
- Negative fixtures chứng minh internal import, forbidden technology dependency, canonical
  value violation và cycle làm architecture test fail.
- Thêm subproject nhưng không áp convention/test phải làm build verification fail.

## Automated enforcement in F-002

`architecture-tests` là project verification độc lập và chạy trong root `check`:

- `ModuleBoundaryTest` kiểm tra cả Gradle project dependency được khai báo và direct
  bytecode dependency theo bảng ở trên. Package owner `marketdata` ánh xạ tới
  `market-data`; `strategy` ánh xạ tới `strategy-core`.
- Mọi truy cập từ owner khác vào package `internal` đều bị từ chối. Các package `api`,
  `port.in`, `port.out` và `event` là những boundary được phép công khai; sự tồn tại của
  package public không tự động cấp thêm dependency ngoài bảng.
- `PurityAndCycleTest` cấm Spring, SQL/JPA/Hibernate/PostgreSQL, Binance và persistence
  adapter trong `domain`, `strategy-core`, `strategies`, `evaluation`; đồng thời kiểm tra
  cycle giữa các package owner.
- Public/domain signatures bị từ chối nếu dùng binary floating point, `LocalDateTime`,
  hoặc field identity không dùng UUID.
- Negative fixture nằm trong test source set riêng và chỉ được import bởi test tương ứng,
  nên fixture cố tình sai không làm nhiễu production scan.

Rule kiểm tra dependency hiện tại không thay đổi ma trận quyết định. Khi cần thêm một cạnh
không có trong bảng, phải review ADR-0002 thay vì nới rule trước.
