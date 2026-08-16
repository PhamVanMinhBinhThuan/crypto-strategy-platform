# Presentation Outline

- **Trạng thái**: Draft — chốt tên người trình bày sau tổng duyệt
- **Tổng thời lượng mục tiêu**: 15 phút
- **Thời lượng demo sản phẩm**: 8–9 phút
- **Thời lượng kiến trúc**: 4–5 phút, trình bày xen kẽ trong demo
- **Mở đầu và kết luận**: 1–2 phút

## 1. Phân chia phần trình bày

| Thứ tự | Phần | Người trình bày | Thời lượng | Nội dung chính |
| ---: | --- | --- | ---: | --- |
| 1 | Bài toán và phạm vi MVP | Tech Lead | 1 phút | Vấn đề cần giải quyết, mục tiêu mở rộng Strategy/Search/Data Provider và những phần ngoài scope |
| 2 | Architecture Overview | Tech Lead | 2 phút | Modular Monolith, ranh giới module, Web/API/Worker/Sentiment và luồng dữ liệu chính |
| 3 | Market Dashboard | Thành viên Frontend | 2 phút | Historical Candles, realtime, bốn chart và đổi timeframe độc lập |
| 4 | Strategy Composer | Thành viên Strategy | 1 phút | Bốn Strategy, Strategy contract, parameters và Majority Vote Composite |
| 5 | Backtest Results | Thành viên Strategy | 2 phút | Cấu hình Backtest, bốn metrics, Trades và Entry/Exit visualization |
| 6 | Search và Leaderboard | Thành viên Infra/Worker | 2 phút | Random Search, Stop Condition, progress, Worker Queue và Top-K |
| 7 | News Sentiment | Thành viên Infra/Worker | 1 phút | News, sentiment label/score/model version và fault isolation |
| 8 | Quality Scenarios và ADR chính | Tech Lead | 2 phút | Thêm Strategy, thay provider/generator, scale Worker và reproduce Experiment |
| 9 | Kết luận | Tech Lead | 1 phút | Những gì đã hoàn thành, giới hạn MVP và hướng mở rộng |

Nếu thời gian bị rút ngắn, gộp phần Architecture Overview vào từng màn hình và bỏ thao tác Stop Search trực tiếp; không bỏ luồng Dashboard → Strategy → Backtest → Search → Leaderboard.

## 2. Nội dung từng thành viên cần chuẩn bị

### Tech Lead

- Một câu mô tả bài toán và một câu mô tả giải pháp.
- Sơ đồ System/Container hoặc Module View đã mở sẵn.
- Ba quyết định chính: Modular Monolith, Adapter/Plugin contract và Queue/Worker.
- Phạm vi MVP và các phần không triển khai.
- Điều phối thời gian, chuyển người nói và nhận câu hỏi chung.

### Thành viên Frontend

- Dashboard mở sẵn với Dataset/fixture đã chốt.
- Chứng minh bốn chart dùng timeframe độc lập.
- Chỉ ra Historical dùng REST, realtime dùng WebSocket.
- Biết giải thích trạng thái Loading, Error, Reconnecting và Disconnected.
- Không cần giải thích thuật toán Strategy hoặc nội bộ Binance.

### Thành viên Strategy

- Parameters demo của MA, RSI, Bollinger Bands và Support/Resistance.
- Giải thích `Strategy` trả `BUY`, `SELL`, `HOLD` và không gọi DB/Binance.
- Giải thích Composite Majority Vote.
- Nắm bốn metrics: Total Return, Win Rate, Max Drawdown và Number of Trades.
- Nắm assumptions Backtest đã chốt trong `demo-data.md`.

### Thành viên Infra/Worker

- Experiment/Result dự phòng đã chạy sẵn.
- Giải thích Random Generator, Stop Condition và progress stages.
- Giải thích Redis Streams chỉ làm queue/cache, PostgreSQL/Supabase giữ dữ liệu chính.
- Chứng minh Top-K tự cập nhật hoặc mở Result đã chuẩn bị.
- Nắm fallback khi Binance, Worker hoặc Sentiment Service gặp lỗi.

## 3. Câu hỏi kiến trúc dự kiến

| Câu hỏi | Người trả lời chính | Ý trả lời ngắn |
| --- | --- | --- |
| Tại sao dùng Modular Monolith thay vì Microservices? | Tech Lead | Nhóm nhỏ và thời gian ngắn; module vẫn có boundary rõ và có thể tách Worker/service khi có nhu cầu scale |
| Thêm MACD phải sửa những gì? | Thành viên Strategy | Thêm implementation và descriptor vào Strategy Registry; Backtester/Evaluator không đổi |
| Tại sao Composite không phụ thuộc `modules/contracts`? | Tech Lead | Composite dùng Strategy contract nội bộ của `strategy-core`, không giao tiếp qua HTTP/WebSocket/queue |
| Thay Binance bằng OKX hoặc provider khác thế nào? | Tech Lead | Viết adapter mới triển khai Market Data Port và trả canonical Candle; UI/Strategy/Backtest không đổi |
| Tại sao Frontend không gọi Binance trực tiếp? | Thành viên Frontend | Tránh phụ thuộc provider contract, giữ reconnect/error handling ở Backend và dùng một public contract ổn định |
| Scale lên nhiều Backtest như thế nào? | Thành viên Infra/Worker | Tăng số Worker trong consumer group; queue phân phối job và xử lý idempotent |
| Redis mất dữ liệu thì sao? | Thành viên Infra/Worker | PostgreSQL/Supabase là nguồn dữ liệu chính; Result/Leaderboard có thể phục hồi hoặc rebuild |
| Vì sao không dùng Kafka/Kubernetes? | Tech Lead | Chi phí vận hành không cần thiết cho MVP; Redis Streams và Docker Compose đủ để chứng minh kiến trúc |
| Stop Search có dừng ngay Worker không? | Thành viên Infra/Worker | Chuyển sang `STOP_REQUESTED`, ngừng sinh job mới và để job đang chạy dừng tại safe checkpoint |
| Kết quả Backtest có tái lập được không? | Thành viên Strategy | Lưu Dataset checksum, Strategy/version, parameters, assumptions, build version và random seed |
| News/Sentiment Service lỗi thì sao? | Thành viên Infra/Worker | News giảm chức năng nhưng Market Dashboard, Strategy và Backtest vẫn chạy nhờ boundary riêng |
| Tại sao không dùng Event Sourcing? | Tech Lead | Dữ liệu quan hệ và immutable results đã đủ; Event Sourcing tăng độ phức tạp không cần thiết cho MVP |
| Leaderboard dùng CQRS như thế nào? | Tech Lead | Evaluation Result là dữ liệu gốc; Top-K là read model tối ưu truy vấn và có thể rebuild |

## 4. Câu chuyển phần

### Tech Lead → Frontend

> Sau khi đã thấy các thành phần chính của hệ thống, phần tiếp theo sẽ chứng minh người dùng nhận dữ liệu Historical và Realtime như thế nào trên Market Dashboard.

### Frontend → Strategy

> Từ dữ liệu Candle đang hiển thị, người dùng có thể chọn các thuật toán tạo tín hiệu. Phần tiếp theo sẽ trình bày Strategy contract và cách tạo Composite Strategy.

### Strategy Composer → Backtest

> Sau khi chốt Strategy và parameters, nhóm dùng cùng cấu hình đó để mô phỏng giao dịch trên Dataset lịch sử và đánh giá kết quả.

### Strategy → Infra/Worker

> Một Backtest đánh giá một cấu hình cụ thể. Để tìm cấu hình tốt hơn, hệ thống chuyển sang Random Search và phân phối nhiều Backtest job qua Worker.

### Search & Leaderboard → News Sentiment

> Ngoài dữ liệu kỹ thuật, hệ thống còn thu thập tin tức và phân tích sentiment trong một service riêng để không ảnh hưởng luồng thị trường cốt lõi.

### Infra/Worker → Tech Lead

> Các luồng vừa trình bày cùng sử dụng contract và ranh giới module đã thống nhất. Phần cuối sẽ tổng kết cách kiến trúc đáp ứng các yêu cầu thay đổi và mở rộng của đề bài.

## 5. Quy tắc khi trình bày

- Mỗi người chỉ giải thích phần mình phụ trách; Tech Lead xử lý câu hỏi giao nhau giữa nhiều module.
- Nói kết quả trước, sau đó mới giải thích kiến trúc hỗ trợ kết quả đó.
- Không đọc nguyên văn tài liệu hoặc source code trên màn hình.
- Không thay parameters, Dataset hoặc environment trong lúc demo nếu chưa tổng duyệt.
- Nếu một thao tác quá 10–15 giây chưa có kết quả, chuyển sang Result đã chạy sẵn theo fallback plan.
- Khi dùng fixture hoặc video dự phòng, nói rõ cho người xem.
- Không tranh luận hoặc sửa code trực tiếp trong buổi trình bày.

## 6. Kết luận chung

Tech Lead kết luận bằng ba ý:

1. MVP hoàn thành luồng Market Data → Strategy → Backtest → Search → Leaderboard và News Sentiment.
2. Strategy, Search Generator và Market Data Provider có contract riêng nên có thể thay đổi với ảnh hưởng nhỏ.
3. PostgreSQL/Supabase giữ dữ liệu chính, Redis Streams hỗ trợ job và Worker có thể scale khi khối lượng Backtest tăng.

Câu kết đề xuất:

> Crypto StrategyLab không chỉ chạy được một chiến lược cố định, mà cung cấp nền tảng có cấu trúc để nhóm tiếp tục thêm Strategy, thuật toán Search và nguồn dữ liệu mới sau MVP.

## 7. Việc cần chốt sau tổng duyệt

- Thay tên vai trò bằng tên thật của bốn thành viên.
- Điều chỉnh thời lượng theo yêu cầu chính thức của giảng viên.
- Chốt ai thao tác máy và ai chỉ trình bày.
- Chốt người xử lý từng phương án fallback.
- Tập câu chuyển phần để không bị ngắt quãng.
- Mỗi thành viên tự trả lời thử ít nhất hai câu hỏi trong bảng kiến trúc.
