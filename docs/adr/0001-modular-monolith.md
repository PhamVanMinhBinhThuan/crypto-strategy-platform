# ADR-0001: Sử dụng Modular Monolith cho Backend cốt lõi

**Status**: Proposed
**Date**: 2026-08-10
**Owners**: Tiến Luật

## Context

Crypto StrategyLab phải hỗ trợ nhiều khả năng nghiệp vụ liên quan nhưng có tốc độ thay đổi khác nhau:

- Lấy dữ liệu lịch sử và realtime từ Market Data Provider;
- Bổ sung Strategy mới mà không sửa Backtester;
- Kết hợp Strategy thành Composite Strategy;
- Backtest, đánh giá metrics và trực quan hóa giao dịch;
- Thay đổi thuật toán Search mà không sửa Evaluation hoặc Leaderboard;
- Thu thập News và phân tích Sentiment;
- Tăng số lượng Backtest Worker trong tương lai.

Nhóm có bốn thành viên và thời gian thực hiện ngắn. Nếu triển khai toàn bộ hệ thống bằng Microservices ngay từ đầu, nhóm phải xử lý thêm service discovery, distributed tracing, network failure, contract versioning và nhiều pipeline triển khai. Những chi phí này không trực tiếp giúp hoàn thành MVP hoặc chứng minh khả năng mở rộng của Strategy.

Một monolith phân tầng thông thường dễ triển khai hơn, nhưng có nguy cơ biến thành `God Service`: Controller, Strategy, Backtest, Search, News và Persistence gọi trực tiếp lẫn nhau. Cách này không đáp ứng trọng tâm kiến trúc của đề là thay đổi Strategy, Search Algorithm và Data Provider với ảnh hưởng tối thiểu.

Hệ thống vì vậy cần một cấu trúc đủ đơn giản để nhóm hoàn thành MVP, đồng thời phải có ranh giới rõ để kiểm thử, thay đổi và tách riêng module khi có nhu cầu scale thực tế.

## Decision

Backend Java cốt lõi sẽ sử dụng **Modular Monolith** với Java 21 và Spring Boot 3.

`apps/api` là composition root và application chạy chính của MVP. Logic nghiệp vụ được đặt trong các module độc lập dưới `modules/`, không đặt trực tiếp trong Controller hoặc lớp khởi động Spring Boot.

Các module chính gồm:

| Module          | Trách nhiệm chính                                           |
| --------------- | ----------------------------------------------------------- |
| `domain`        | Các kiểu dữ liệu và quy tắc domain ổn định                  |
| `contracts`     | DTO, command, query và event contract dùng qua boundary     |
| `market-data`   | Market Data Port, Binance Adapter và chuẩn hóa Candle       |
| `strategy-core` | Strategy contract, signal và registry                       |
| `strategies`    | MA, RSI, Bollinger Bands và Support/Resistance              |
| `combination`   | Tạo và thực thi Composite Strategy                          |
| `backtesting`   | Mô phỏng giao dịch trên historical data                     |
| `evaluation`    | Tính Return, Win Rate, Max Drawdown và Number of Trades     |
| `search`        | Sinh candidate bằng Random Search và quản lý stop condition |
| `leaderboard`   | Xếp hạng và duy trì Top-K                                   |
| `news`          | News Provider contract và chuẩn hóa News Item               |
| `persistence`   | Adapter lưu trữ PostgreSQL/Supabase và Redis                |

Các quy tắc cấp cao:

1. Module chỉ giao tiếp qua public contract hoặc application service đã công bố; không gọi vào implementation nội bộ của module khác.
2. `domain`, Strategy implementation và Evaluation không phụ thuộc Spring, database, Binance hoặc giao diện người dùng.
3. Frontend chỉ gọi Backend API/WebSocket, không gọi trực tiếp Binance và không chứa Strategy, Backtest hoặc Ranking logic.
4. Các bảng có owner logic theo module dù MVP có thể dùng chung một PostgreSQL/Supabase instance.
5. Giao tiếp nội bộ đồng bộ được ưu tiên cho luồng đơn giản; domain event được dùng khi cần giảm coupling.
6. `apps/worker` chỉ trở thành runtime riêng cho Backtest/Search khi [ADR-0006: Queue và Worker cho Backtest/Search](0006-queue-worker-backtesting.md) được chấp nhận. Worker tái sử dụng public contract và module nghiệp vụ, không sao chép business logic.
7. Sentiment bằng Python/FastAPI là service boundary riêng vì có runtime và failure mode khác; chi tiết được quyết định trong [ADR-0008: Tách Sentiment Service](0008-sentiment-service-boundary.md).
8. Không đưa Kafka, Kubernetes hoặc Microservices theo từng module vào MVP nếu chưa có bằng chứng về nhu cầu.

ADR này quyết định kiến trúc tổng thể. Dependency cụ thể giữa từng module sẽ được quy định trong [ADR-0002: Ranh giới và phụ thuộc giữa các Module](0002-module-boundaries.md).

## Alternatives Considered

- **Layered Monolith truyền thống**: Dễ bắt đầu nhưng không tạo ranh giới nghiệp vụ đủ mạnh; Strategy, Search, Backtest và Persistence dễ phụ thuộc vòng hoặc tích tụ trong một service lớn.
- **Microservices cho từng module**: Cho phép deploy và scale độc lập, nhưng làm tăng mạnh độ phức tạp vận hành, network failure, distributed transaction, observability và contract versioning đối với nhóm bốn người.
- **Một service độc lập cho từng feature ngay từ đầu**: Có boundary rõ trên network nhưng tạo nhiều repository/runtime nhỏ, tăng thời gian tích hợp và không đem lại lợi ích rõ ràng cho MVP.
- **Serverless functions cho toàn bộ backend**: Phù hợp với tác vụ rời rạc, nhưng gây khó cho realtime connection, Backtest dài, local development và việc giữ domain flow nhất quán.

## Consequences

### Positive

- Một backend chính dễ chạy local, test, debug và triển khai bằng Docker Compose.
- Module boundary phản ánh đúng Market, Strategy, Backtest, Search, Leaderboard và News.
- Thêm Strategy hoặc Search Algorithm mới chủ yếu tác động module sở hữu và registry tương ứng.
- Giao tiếp trong process giảm độ trễ và tránh network failure không cần thiết cho MVP.
- Có thể tách Backtest Worker hoặc module có nhu cầu scale thành runtime/service riêng sau này.
- Phù hợp với quy mô bốn thành viên và thời gian đồ án nhưng vẫn chứng minh được modifiability.

### Negative

- Các module trong `apps/api` vẫn được deploy cùng nhau.
- Lỗi không được cô lập hoàn toàn như các Microservice riêng biệt.
- Không thể scale riêng từng module đang chạy trong API process.
- Shared database có thể tạo coupling nếu nhóm truy cập chéo bảng.
- Boundary chỉ có hiệu lực nếu được bảo vệ bằng cấu trúc build, architecture test và code review.

## Affected Components

- `apps/api`
- `apps/worker`
- `apps/web`
- `apps/sentiment`
- toàn bộ thư mục `modules/`
- `infra/compose/`
- `infra/database/`

## Validation

Quyết định được xem là phù hợp khi nhóm chứng minh được:

- build và test toàn bộ Java modules bằng một lệnh của build tool;
- architecture test phát hiện dependency bị cấm giữa các module;
- thêm `MACDStrategy` không yêu cầu sửa Backtester, Evaluator hoặc Frontend;
- thay Binance Adapter bằng một Market Data Adapter giả mà Frontend contract không đổi;
- thêm Search Generator mới không yêu cầu sửa Backtester hoặc Leaderboard;
- News/Sentiment Service ngừng hoạt động nhưng Market Dashboard và Market Data flow vẫn chạy;
- Backtest có thể chuyển sang `apps/worker` mà không sao chép Strategy hoặc Evaluation logic.

## Risks and Mitigations

- **Risk**: Modular Monolith dần trở thành monolith phụ thuộc chéo.

  **Mitigation**: Dùng Java modules/package boundary, ArchUnit test, public contract rõ ràng và review dependency trong Pull Request.

- **Risk**: Các module truy cập trực tiếp bảng của nhau trong shared database.

  **Mitigation**: Quy định data ownership trong [ADR-0007: PostgreSQL và Redis Ownership](0007-postgresql-redis-ownership.md); module khác phải đi qua port/application service của owner.

- **Risk**: API process trở thành nút thắt khi số lượng Backtest tăng.

  **Mitigation**: Theo dõi thời gian và số job; tách `apps/worker`, queue và worker pool theo [ADR-0006: Queue và Worker cho Backtest/Search](0006-queue-worker-backtesting.md) khi đạt ngưỡng đã thống nhất.

- **Risk**: Tạo quá nhiều module nhỏ làm nhóm khó phát triển.

  **Mitigation**: Chỉ tạo module theo architectural driver hoặc business capability; không tách module chỉ để tổ chức file.

- **Risk**: Contract dùng chung trở thành nơi chứa mọi kiểu dữ liệu.

  **Mitigation**: Chỉ đặt contract thật sự đi qua boundary trong `contracts`; model nội bộ ở lại module sở hữu.

## References

- [Đề bài Crypto StrategyLab](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf)
- [Architecture Overview](../architecture/architecture-overview.md)
- [Module View](../architecture/module-view.md)
- [ADR-0002: Module Boundaries](0002-module-boundaries.md)
- [ADR-0006: Queue và Worker](0006-queue-worker-backtesting.md)
- [ADR-0007: PostgreSQL và Redis Ownership](0007-postgresql-redis-ownership.md)
- [ADR-0008: Sentiment Service Boundary](0008-sentiment-service-boundary.md)

## Supersession

- Supersedes: None
- Superseded by: None
