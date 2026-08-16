# Demo Script

- **Status**: Draft — chốt sau buổi tổng duyệt
- **Demo Version/Tag**: Chốt trong `demo-data.md` sau tổng duyệt
- **Thời lượng mục tiêu**: 12–15 phút
- **Người điều phối**: Tech Lead

## 1. Mục tiêu buổi demo

Chứng minh Crypto StrategyLab cho phép người dùng:

1. Theo dõi Historical và Realtime Candles trên 1–4 chart độc lập.
2. Chọn Strategy, tạo Composite Strategy và chạy Backtest.
3. Xem bốn metrics cùng vị trí Entry/Exit của Trade.
4. Chạy Random Search có điều kiện dừng và xem Top-K Leaderboard.
5. Xem News Sentiment mà không làm ảnh hưởng Market Dashboard khi Sentiment Service lỗi.

Thông điệp kiến trúc chính:

> Có thể thêm Strategy, thay Search Algorithm, thay Market Data Provider hoặc tăng Backtest Worker với ảnh hưởng tối thiểu đến phần còn lại của hệ thống.

## 2. Điều kiện trước khi bắt đầu

- Dùng đúng commit/tag đã tổng duyệt.
- Các service cần thiết đã healthy.
- Dataset, Strategy parameters, Random seed và expected results đã được cố định trong [Demo Data](demo-data.md).
- Fixture fallback và kết quả Backtest/Search chạy sẵn đã được chuẩn bị.
- Không hiển thị `.env`, API key, token hoặc secret trên màn hình.
- Đã mở sẵn Dashboard và trang kiến trúc cần trình bày; tắt notification không liên quan.

## 3. Phân bổ thời gian đề xuất

| Phần | Thời lượng |
| --- | ---: |
| Giới thiệu bài toán và scope | 1 phút |
| Market Dashboard | 2 phút |
| Strategy, Composite và Backtest | 3 phút |
| Search và Leaderboard | 3 phút |
| News Sentiment và fault isolation | 1 phút |
| Chứng minh kiến trúc | 2–3 phút |
| Kết luận | 1 phút |

Nếu thời gian demo do giảng viên quy định khác, giữ nguyên thứ tự và rút ngắn phần giải thích; không bỏ luồng chính từ Dashboard đến Leaderboard.

## 4. Mở đầu

### Thao tác

1. Mở Market Dashboard.
2. Giới thiệu ngắn phạm vi MVP.

### Nội dung nói

> Crypto StrategyLab là nền tảng thử nghiệm chiến lược giao dịch crypto. Người dùng có thể quan sát dữ liệu thị trường, kết hợp chiến lược, Backtest và tìm bộ tham số tốt bằng Random Search. Trọng tâm của đồ án là kiến trúc có thể mở rộng Strategy, Search và Data Provider mà không phải sửa toàn bộ hệ thống.

### Giới hạn cần nói rõ nếu được hỏi

- Không giao dịch tiền thật.
- Không có authentication phức tạp.
- Không dùng Genetic Algorithm trong MVP.
- Không triển khai Kafka, Kubernetes hoặc Microservices theo từng module.
- Chỉ tích hợp một Market Data Provider trong MVP.

## 5. Market Dashboard

### Thao tác và kết quả mong đợi

| Bước | Thao tác | Kết quả phải thấy |
| ---: | --- | --- |
| 1 | Chọn pair mặc định trong `demo-data.md` | Historical Candles hiển thị đúng |
| 2 | Chọn bố cục bốn chart | Bốn chart xuất hiện trong cùng Dashboard |
| 3 | Đặt bốn timeframe khác nhau | Mỗi chart hiển thị timeframe riêng |
| 4 | Chờ một realtime update | Candle đang mở được cập nhật mà trang không reload |
| 5 | Đổi timeframe của Chart 1 | Chỉ Chart 1 tải lại; Chart 2–4 giữ nguyên |
| 6 | Chỉ vào trạng thái realtime | UI hiển thị rõ `CONNECTED` hoặc trạng thái hiện tại |

### Nội dung nói

> Frontend tải lịch sử bằng REST và nhận cập nhật mới qua một WebSocket connection có nhiều logical subscriptions. Mỗi chart có `subscriptionId`, pair và timeframe riêng. Frontend không kết nối trực tiếp Binance; Java Backend sử dụng Market Data Adapter để chuyển dữ liệu thành Candle chuẩn của hệ thống.

### Điểm kiến trúc chứng minh

- Market Data Adapter cô lập cấu trúc riêng của Binance.
- Một WebSocket connection phục vụ tối đa bốn chart.
- Đổi timeframe một chart không ảnh hưởng chart khác.
- Reconnect sử dụng REST backfill để lấp Candle bị thiếu.

## 6. Strategy Composer

### Thao tác và kết quả mong đợi

| Bước | Thao tác | Kết quả phải thấy |
| ---: | --- | --- |
| 1 | Mở Strategy Composer | Có MA, RSI, Bollinger Bands và Support/Resistance |
| 2 | Chọn ít nhất hai Strategy | Form parameters tương ứng xuất hiện |
| 3 | Nhập parameters đã chốt trong `demo-data.md` | Validation hợp lệ |
| 4 | Chọn Majority Vote | Composite summary hiển thị các Strategy con |
| 5 | Lưu/tạo Composite | Nhận Composite ID/version hoặc chuyển được sang Backtest |

### Nội dung nói

> Tất cả Strategy triển khai cùng một Strategy contract và chỉ nhận Candle để tạo tín hiệu BUY, SELL hoặc HOLD. Composite Strategy cũng được xem như một Strategy, nên Backtester không cần biết nó là MA, RSI hay tổ hợp Majority Vote.

### Điểm kiến trúc chứng minh

- Strategy logic không gọi Database, Binance hoặc Spring.
- Strategy Registry cung cấp metadata và parameter schema.
- Thêm Strategy mới không yêu cầu sửa Backtester.

## 7. Backtest Results

### Thao tác và kết quả mong đợi

| Bước | Thao tác | Kết quả phải thấy |
| ---: | --- | --- |
| 1 | Chọn Dataset đã freeze | Hiển thị pair, timeframe và khoảng dữ liệu |
| 2 | Kiểm tra cấu hình | Long-only, một position, Candle close và fee theo `demo-data.md` |
| 3 | Bấm `Run Backtest` | Nhận Job ID và trạng thái `QUEUED`/`RUNNING` |
| 4 | Chờ hoàn thành hoặc mở Result đã chạy sẵn | Trạng thái chuyển `COMPLETED` |
| 5 | Mở metrics | Có Total Return, Win Rate, Max Drawdown và Number of Trades |
| 6 | Chọn một Trade | Chart highlight Entry/Exit tương ứng |

### Nội dung nói

> Backtest chạy bất đồng bộ để HTTP request không bị giữ lâu. Worker gọi Strategy qua contract, Backtester mô phỏng giao dịch, sau đó Evaluator tính metrics. Kết quả đầy đủ được đọc bằng REST; WebSocket chỉ báo tiến trình và Result ID.

### Điểm kiến trúc chứng minh

- Backtester, Evaluator và Ranking có trách nhiệm riêng.
- Dataset, Strategy version và assumptions được ghi lại để tái lập kết quả.
- Trade visualization dùng kết quả chuẩn, không tính lại logic giao dịch ở Frontend.

## 8. Search và Leaderboard

### Thao tác và kết quả mong đợi

| Bước | Thao tác | Kết quả phải thấy |
| ---: | --- | --- |
| 1 | Mở Search & Leaderboard | Form Search Space và Stop Condition xuất hiện |
| 2 | Chọn Random Generator và seed cố định | Cấu hình có thể tái lập |
| 3 | Nhập giới hạn candidate/thời gian | Không cho chạy nếu thiếu Stop Condition hữu hạn |
| 4 | Bấm `Start Search` | Experiment được tạo với trạng thái `QUEUED`/`RUNNING` |
| 5 | Quan sát progress | Thấy candidate counts và bước Generate/Backtest/Evaluate/Rank |
| 6 | Quan sát Leaderboard | Top-K cập nhật khi revision mới đến, không reload trang |
| 7 | Mở candidate Top-1 | Xem Strategy parameters, score và Backtest Result |
| 8 | Nếu thời gian cho phép, bấm Stop | Trạng thái đi qua `STOP_REQUESTED` rồi `STOPPED` an toàn |

### Nội dung nói

> Strategy Generator chỉ sinh candidate. Mỗi candidate được xử lý qua Backtest, Evaluation và Ranking. Redis Streams phân phối job theo cơ chế at-least-once, còn PostgreSQL/Supabase là nguồn dữ liệu chính. Việc ghi Result và cập nhật Leaderboard phải idempotent để không bị trùng khi job được giao lại.

### Điểm kiến trúc chứng minh

- Random Search luôn có Stop Condition; không dùng vòng lặp vô hạn.
- Có thể thay Random Generator bằng generator khác qua `StrategyGenerator` contract.
- Worker có thể tăng số instance mà không sao chép business logic.
- Leaderboard là read model có thể rebuild từ Evaluation Result; Redis chỉ hỗ trợ queue/cache.

## 9. News Sentiment

### Thao tác và kết quả mong đợi

| Bước | Thao tác | Kết quả phải thấy |
| ---: | --- | --- |
| 1 | Mở News Sentiment | Danh sách tin đã chuẩn hóa xuất hiện |
| 2 | Lọc theo sentiment | Có `POSITIVE`, `NEUTRAL`, `NEGATIVE` |
| 3 | Mở một News Item | Hiển thị source, published time, label, score và model version |
| 4 | Quay lại Market Dashboard | Market Dashboard vẫn hoạt động độc lập |

### Nội dung nói

> Java Worker gọi Python FastAPI qua contract nội bộ. Python chỉ chịu trách nhiệm phân tích sentiment; lỗi của News hoặc Sentiment Service không được làm Market Dashboard, Strategy hoặc Backtest ngừng hoạt động.

### Điểm kiến trúc chứng minh

- Python Sentiment là runtime riêng vì dùng công nghệ và failure mode khác.
- News/Sentiment được cô lập khỏi luồng Market Data.
- Model version được lưu cùng kết quả để hỗ trợ giải thích và tái lập.

## 10. Architecture Proof

Không cần sửa code trực tiếp trong lúc demo. Mở sơ đồ/module hoặc đoạn contract đã chuẩn bị trước và giải thích ngắn theo bảng sau.

| Câu hỏi cần chứng minh | Bằng chứng trình bày | Câu trả lời ngắn |
| --- | --- | --- |
| Thêm MACD Strategy sửa gì? | Strategy contract và Registry | Thêm implementation/descriptor mới; Backtester và Evaluator không đổi |
| Thay Binance bằng provider khác? | `MarketDataProvider` và adapter | Viết adapter mới trả canonical Candle; Frontend và Strategy không đổi |
| Thay Random Search? | `StrategyGenerator` contract | Đăng ký generator implementation mới; pipeline Backtest/Evaluation/Ranking giữ nguyên |
| Scale nhiều Backtest? | Queue/Worker flow | Tăng Worker consumer; API và business contract không đổi |
| Redis mất dữ liệu? | PostgreSQL ownership và Leaderboard flow | Result nằm trong PostgreSQL; cache/read model có thể rebuild |
| News/Sentiment lỗi? | Container/module boundary | Chỉ News UI giảm chức năng; Market Dashboard vẫn chạy |
| Tái lập Experiment? | Experiment Manifest | Dùng cùng dataset checksum, versions, assumptions và random seed |

## 11. Fallback trong lúc demo

| Sự cố | Cách chuyển nhanh |
| --- | --- |
| Binance hoặc Internet lỗi | Chuyển sang Market Data fixture đã tổng duyệt |
| WebSocket không ổn định | Dùng recorded events hoặc trình bày Historical chart và trạng thái disconnected |
| Backtest/Search chạy quá lâu | Mở Result/Experiment đã chạy sẵn bằng ID cố định |
| News/Sentiment lỗi | Chứng minh trạng thái unavailable và Market Dashboard vẫn hoạt động |
| Toàn hệ thống lỗi | Chuyển sang video dự phòng và slide kiến trúc |

Đường dẫn, lệnh chuyển chế độ và ID cụ thể phải được điền trong [Fallback Plan](fallback-plan.md) sau khi hệ thống chạy được.

## 12. Kết luận

### Nội dung nói

> Nhóm đã xây dựng luồng từ dữ liệu thị trường đến Strategy, Backtest, Search, Leaderboard và News Sentiment. MVP ưu tiên kiến trúc module rõ ràng, contract ổn định và khả năng thay đổi từng thành phần mà không ảnh hưởng toàn hệ thống. Những phần như giao dịch thật, Genetic Algorithm, nhiều sàn và hạ tầng Kubernetes nằm ngoài phạm vi hiện tại.

### Trước khi kết thúc

- Quay lại màn hình kết quả chính hoặc Architecture Overview.
- Không để màn hình ở trạng thái lỗi không được giải thích.
- Tech Lead nhận câu hỏi và chuyển cho thành viên phụ trách đúng phần.

## 13. Nội dung phải chốt sau tổng duyệt

- Commit/tag dùng để demo.
- Tổng thời lượng chính thức và người trình bày từng phần.
- Pair, timeframe, Dataset và Strategy parameters.
- Random seed, Stop Condition và Top-K.
- Expected metrics và tolerance.
- Result/Experiment ID dự phòng.
- Lệnh bật fixture và vị trí video dự phòng.
