# Demo Checklist

Checklist này được dùng cho buổi tổng duyệt và buổi demo chính thức. Tech Lead chịu trách nhiệm xác nhận trạng thái chung; mỗi thành viên xác nhận phần mình phụ trách.

## 1. Một tuần trước demo

- [ ] Xác nhận thời lượng trình bày và thời lượng hỏi đáp.
- [ ] Chốt máy dùng để demo và máy dự phòng.
- [ ] Chốt người thao tác máy, người điều phối và người trình bày từng phần.
- [ ] Hoàn thành luồng MVP trong [Demo Script](demo-script.md).
- [ ] Chốt nội dung thuyết trình trong [Presentation Outline](presentation-outline.md).
- [ ] Chốt phạm vi chức năng sẽ demo trực tiếp và chức năng chỉ giải thích.
- [ ] Chuẩn bị Market Data fixture, News fixture và Experiment Result dự phòng.
- [ ] Chuẩn bị slide/sơ đồ kiến trúc cần mở trong phần Architecture Proof.
- [ ] Kiểm tra repository không chứa secret hoặc `.env` thật.
- [ ] Kiểm tra tài khoản, mạng và quyền truy cập các dịch vụ dùng trong demo.

## 2. Buổi tổng duyệt

### Phiên bản và dữ liệu

- [ ] Tạo commit/tag ứng viên dùng để demo.
- [ ] Ghi commit/tag vào [Demo Data](demo-data.md).
- [ ] Chốt pair, timeframes và Historical range.
- [ ] Chốt Dataset ID/version, Candle count và checksum.
- [ ] Chốt Strategy versions và parameters.
- [ ] Chốt Backtest assumptions và fee.
- [ ] Chốt Random seed, Stop Conditions và Top-K.
- [ ] Ghi expected metrics/result cùng tolerance chấp nhận được.
- [ ] Không thay Dataset hoặc parameters sau tổng duyệt nếu chưa chạy lại toàn bộ kịch bản.

### Hệ thống

- [ ] Build Frontend thành công.
- [ ] Build và test Java API thành công.
- [ ] Build và test Java Worker thành công.
- [ ] Khởi động Python Sentiment Service thành công.
- [ ] PostgreSQL/Supabase kết nối được.
- [ ] Redis và Redis Streams hoạt động.
- [ ] Health check của các service cần thiết pass.
- [ ] Database migration chạy thành công trên môi trường demo sạch.
- [ ] Không có warning/error nghiêm trọng trong log khi chạy luồng chính.

### Tổng duyệt kịch bản

- [ ] Chạy toàn bộ kịch bản một lần không dừng để sửa code.
- [ ] Tổng thời gian không vượt giới hạn đã thống nhất.
- [ ] Mỗi người nói đúng phần và chuyển phần không bị ngắt quãng.
- [ ] Thử ít nhất một tình huống fallback.
- [ ] Ghi lại video dự phòng sau khi luồng chạy ổn định.
- [ ] Lưu Result ID và Experiment ID dự phòng.
- [ ] Chốt tag cuối sau khi mọi thành viên xác nhận.

## 3. Trước ngày demo

- [ ] Clone hoặc kiểm tra lại đúng tag demo trên máy trình bày.
- [ ] Chạy toàn bộ test liên quan.
- [ ] Xác nhận fixture và dữ liệu dự phòng nằm đúng vị trí.
- [ ] Xác nhận video dự phòng mở được khi không có mạng.
- [ ] Xác nhận slide và sơ đồ kiến trúc mở được offline.
- [ ] Kiểm tra Docker image/dependency cần thiết đã có trên máy.
- [ ] Kiểm tra dự án chạy được trên kiến trúc máy demo, gồm ARM64 nếu dùng Mac Apple Silicon.
- [ ] Tắt notification, ứng dụng chat và nội dung cá nhân có thể xuất hiện.
- [ ] Dọn terminal history/màn hình để không lộ secret.
- [ ] Sạc máy, chuẩn bị nguồn điện, adapter trình chiếu và hotspot dự phòng.

## 4. Trước giờ demo 30–60 phút

### Môi trường

- [ ] Máy demo đang dùng đúng branch/tag và không có thay đổi chưa kiểm soát.
- [ ] Không có port conflict.
- [ ] Docker/services đều healthy.
- [ ] PostgreSQL/Supabase truy cập được.
- [ ] Redis truy cập được và Worker consumer sẵn sàng.
- [ ] Java API health check pass.
- [ ] Frontend mở được trên browser dùng để trình bày.
- [ ] Python Sentiment Service ready.
- [ ] Binance và News Provider truy cập được.
- [ ] Đồng hồ hệ thống và timezone đúng.

### Dữ liệu và giao diện

- [ ] Dataset demo tồn tại và checksum đúng.
- [ ] Strategy/Composite versions demo tồn tại.
- [ ] Result và Experiment dự phòng đọc được.
- [ ] Không còn dữ liệu rác làm Leaderboard khó theo dõi.
- [ ] Browser zoom, độ phân giải và dark mode hiển thị đúng.
- [ ] Các tab cần dùng đã mở và sắp xếp theo thứ tự demo.
- [ ] DevTools chỉ mở khi thực sự cần chứng minh WebSocket hoặc lỗi.
- [ ] Video, slide và fallback links đã mở thử.

## 5. Kiểm tra chức năng bắt buộc

### Market Dashboard

- [ ] Historical Candles tải được bằng REST.
- [ ] Realtime Candle cập nhật mà không reload trang.
- [ ] Hiển thị được bố cục 1–4 chart.
- [ ] Bốn chart có thể dùng timeframe độc lập.
- [ ] Đổi timeframe Chart 1 không làm Chart 2–4 reload.
- [ ] Có trạng thái Loading, Empty và Error.
- [ ] Có trạng thái Connected, Reconnecting và Disconnected.
- [ ] Pair, timeframe, OHLCV và timestamp hiển thị đúng.

### Strategy Composer

- [ ] Có MA, RSI, Bollinger Bands và Support/Resistance.
- [ ] Parameter form khớp Strategy metadata/schema.
- [ ] Validation chặn parameter không hợp lệ.
- [ ] Tạo được Composite Strategy bằng Majority Vote.
- [ ] Composite hiển thị rõ Strategy con và version.

### Backtest Results

- [ ] Chọn được Dataset đã freeze.
- [ ] Hiển thị đúng initial capital, fee và assumptions.
- [ ] Tạo Backtest job và nhận trạng thái ban đầu.
- [ ] Progress/completion được cập nhật.
- [ ] Có Total Return, Win Rate, Max Drawdown và Number of Trades.
- [ ] Trade history hiển thị được.
- [ ] Entry/Exit được đánh dấu đúng trên chart.
- [ ] Result truy vết được Strategy version và Dataset.

### Search và Leaderboard

- [ ] Chọn được Random Generator và seed cố định.
- [ ] Search Space validate theo Strategy schema.
- [ ] Không cho chạy Search nếu thiếu Stop Condition hữu hạn.
- [ ] Tạo được Experiment và Job.
- [ ] Progress hiển thị candidate counts và pipeline stage.
- [ ] Stop chuyển qua `STOP_REQUESTED` và kết thúc an toàn.
- [ ] Top-K cập nhật khi Leaderboard revision tăng.
- [ ] Mở được Candidate/Backtest Result từ Leaderboard.
- [ ] Không có candidate hoặc ranking bị ghi trùng khi event/job được phát lại.

### News Sentiment

- [ ] News Items hiển thị source và published time.
- [ ] Có label `POSITIVE`, `NEUTRAL`, `NEGATIVE`.
- [ ] Có sentiment score và model version.
- [ ] Có Loading, Empty và Service Unavailable state.
- [ ] Sentiment Service lỗi không làm Market Dashboard hoặc Backtest ngừng hoạt động.

## 6. Kiểm tra điểm kiến trúc

- [ ] Mở được System/Container/Module diagram cần trình bày.
- [ ] Giải thích được ranh giới giữa Web, API, Worker và Sentiment Service.
- [ ] Giải thích được Strategy Plugin/Registry.
- [ ] Giải thích được Market Data Port và Binance Adapter.
- [ ] Giải thích được Strategy Generator contract.
- [ ] Giải thích được Backtester, Evaluator và Ranking tách biệt.
- [ ] Giải thích được Queue/Worker và idempotency.
- [ ] Giải thích được PostgreSQL/Supabase ownership và vai trò của Redis.
- [ ] Giải thích được fault isolation khi News/Sentiment lỗi.
- [ ] Giải thích được Experiment reproducibility.
- [ ] Giải thích được Leaderboard read model và lý do không dùng Event Sourcing.
- [ ] Extensibility proof: thêm MACD mà downstream không phải thay đổi.
- [ ] Replaceability proof: thêm hoặc đổi Strategy Generator.
- [ ] Scale proof: so sánh một và ba Worker, đồng thời kiểm tra duplicate.
- [ ] Failure proof: tắt Sentiment và thử ngắt/kết nối lại Market Data.
- [ ] Provenance proof: Top-K truy ngược được đúng Manifest và version.
- [ ] Evidence gắn với commit/tag đã kiểm tra và không sử dụng số liệu giả.

Target, trạng thái và bằng chứng chi tiết nằm trong [Architecture Evidence](../architecture/architecture-evidence.md).

## 7. Kiểm tra WebSocket và lỗi

- [ ] Một browser tab chỉ mở một application WebSocket connection.
- [ ] Subscribe/unsubscribe đúng `subscriptionId`.
- [ ] Client bỏ qua event trùng theo `eventId`.
- [ ] Client không áp dụng Leaderboard revision cũ.
- [ ] Reconnect tự subscribe lại các subscription đang hoạt động.
- [ ] REST backfill lấp Candle gap sau reconnect.
- [ ] Lỗi một subscription không làm đóng toàn bộ connection khi có thể cô lập.
- [ ] Error UI không hiển thị stack trace hoặc thông tin nội bộ.
- [ ] Không có request từ Frontend gọi thẳng Binance hoặc Supabase.

## 8. Fallback phải sẵn sàng

- [ ] Market Data fixture chạy được khi Binance lỗi.
- [ ] Recorded WebSocket events hoặc dữ liệu tĩnh mở được.
- [ ] News fixture chạy được khi News Provider lỗi.
- [ ] Sentiment Result fixture đọc được khi Python Service lỗi.
- [ ] Backtest Result dự phòng mở được khi Worker chậm/lỗi.
- [ ] Experiment/Leaderboard dự phòng mở được khi Search không hoàn thành kịp.
- [ ] Video dự phòng có âm thanh/hình ảnh rõ và chạy offline.
- [ ] Slide kiến trúc mở được không cần Internet.
- [ ] Mỗi fallback có người chịu trách nhiệm chuyển đổi.
- [ ] Khi dùng fallback, người trình bày biết cách nói rõ đó là fixture/video.

Chi tiết đường dẫn và cách chuyển chế độ được cập nhật trong [Fallback Plan](fallback-plan.md) sau khi implementation ổn định.

## 9. Ngay trước khi bắt đầu

- [ ] Đặt điện thoại ở chế độ im lặng.
- [ ] Đóng tab, terminal và ứng dụng không liên quan.
- [ ] Đưa hệ thống về trạng thái bắt đầu của Demo Script.
- [ ] Kiểm tra màn hình máy chiếu hiển thị đúng.
- [ ] Người điều phối bắt đầu bấm giờ.
- [ ] Cả bốn thành viên có bản câu hỏi kiến trúc dự kiến.
- [ ] Thống nhất tín hiệu chuyển sang fallback hoặc bỏ bước khi gần hết giờ.

## 10. Sau demo

- [ ] Ghi lại câu hỏi và phản hồi của giảng viên.
- [ ] Ghi lỗi hoặc khác biệt so với lần tổng duyệt.
- [ ] Lưu phiên bản/tag cuối dùng để trình bày.
- [ ] Lưu tài liệu, slide và video cần nộp.
- [ ] Xóa hoặc thu hồi credential tạm nếu đã tạo cho môi trường demo.
- [ ] Tạo issue cho lỗi cần sửa sau demo; không sửa vội trên tag đã trình bày.
