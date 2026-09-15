# Kịch bản demo Full Flow — tập trung kiến trúc

Thời lượng: khoảng **5–6 phút**.

```text
Realtime → Strategy → Search → Backtest → Evaluate
→ Leaderboard → Visualize → News/Sentiment
```

## 1. Mở đầu

**Nói:**

> Em sẽ demo luồng hoàn chỉnh từ dữ liệu realtime đến kết quả xếp hạng và News Sentiment. Trọng tâm là cách các module giao tiếp qua contract, cách Worker xử lý tác vụ dài và cách kết quả được lưu để truy vết.

> Backend cốt lõi dùng Modular Monolith; API và Worker chạy ở hai process riêng. PostgreSQL là nguồn dữ liệu chính, Redis dùng để truyền job và progress, còn Sentiment là Python service độc lập.

## 2. Realtime Market Data

**Thao tác:** Mở Market Dashboard, chọn `BTC/USDT`, chỉ bốn timeframe và di chuột lên một Candle.

**Nói:**

> Historical Candle được tải qua REST, còn update mới đi qua WebSocket. Frontend không gọi Binance trực tiếp; Binance Adapter chuyển payload của sàn thành canonical Candle nên UI và Strategy không phụ thuộc provider.

> Nếu kết nối bị ngắt, backend reconnect, backfill và loại Candle trùng. Nhờ Adapter, sau này đổi provider thì frontend không phải đổi.

**Chuyển:**

> Candle chuẩn hóa này sẽ trở thành đầu vào chung cho các Strategy.

## 3. Chọn Strategy

**Thao tác:** Mở Strategy Composer, chỉ năm Strategy và chọn một Strategy hoặc Composite đã publish.

**Nói:**

> Mọi Strategy dùng cùng contract: nhận `StrategyContext` và trả BUY, SELL hoặc HOLD. Strategy không tự gọi Database hay Binance nên dễ test và tái lập.

> Plugin/Registry quản lý ID, version và parameters. Composite dùng Majority Vote hoặc Weighted Vote để kết hợp tín hiệu; nếu hòa thì trả HOLD.

> Sentiment Polarity cũng là một plugin bình thường, nhưng đầu vào là News đã phân tích và được đóng băng. Bốn tham số quyết định khoảng News, số bài tối thiểu và hai ngưỡng BUY/SELL.

**Chuyển:**

> Một bộ parameters chỉ là một cấu hình. Search sẽ thử nhiều cấu hình để tìm kết quả tốt hơn.

## 4. Search và Candidate

**Thao tác:** Mở Search, chọn Dataset và Strategy; chỉ `minimum`, `maximum`, `step`; đặt seed `42`, tối đa `3–4` Candidate và nhấn Start.

**Nói:**

> Search Generator tạo ra các Candidate, tức là các cấu hình Strategy với bộ tham số khác nhau. Nó chỉ tạo cấu hình; Backtest và xếp hạng do các thành phần phía sau thực hiện.

> Khi nhấn Start, API lưu lại toàn bộ cấu hình Search rồi trả kết quả ngay; phần tìm kiếm sẽ được Worker xử lý sau. Hệ thống cũng lưu tên, phiên bản thuật toán và seed để có thể tạo lại đúng các Candidate hoặc thay thuật toán khác mà không sửa Backtest.

**Chuyển:**

> Tiếp theo, công việc được đưa vào Redis để Worker xử lý ở phía sau.

## 5. Backtest và Evaluate

**Thao tác:** Chỉ progress, Candidate đang chạy và trạng thái `QUEUED/RUNNING/COMPLETED`.

**Nói:**

> Redis chia các Candidate cho những Worker đang rảnh. Worker dùng đúng dữ liệu, Strategy và tham số đã lưu để chạy Backtest. Sau đó, Evaluator tính lợi nhuận, tỷ lệ thắng, mức sụt giảm lớn nhất và số giao dịch.

> Một công việc có thể được Redis gửi lại khi xảy ra lỗi. Hệ thống kiểm tra mã công việc và dữ liệu đã lưu để không tạo kết quả trùng. Nếu Worker bị dừng, công việc vẫn còn và Worker khác có thể xử lý tiếp.

**Chuyển:**

> Các kết quả đánh giá này sẽ được so sánh để tạo bảng xếp hạng.

## 6. Leaderboard và Visualize

**Thao tác:** Chỉ Top-K đang cập nhật, nhấn Candidate đầu bảng rồi mở Backtest Result.

**Nói:**

> Leaderboard là bảng Top-K được tạo từ các kết quả đánh giá. Mỗi lần cập nhật có số phiên bản riêng, nên giao diện chỉ nhận bản mới nhất và không bị sai thứ hạng khi các Worker hoàn thành không cùng lúc.

> Khi mở một Candidate, ta xem được Strategy, tham số, bộ dữ liệu và lần chạy đã tạo ra nó. Biểu đồ chỉ hiển thị lại tín hiệu và giao dịch do backend tính; frontend không tự chạy lại thuật toán.

> Khả năng lần ngược nguồn gốc như vậy được gọi là provenance. Nhờ đó, nhóm có thể giải thích chính xác vì sao một kết quả đứng đầu.

**Chuyển:**

> Ngoài dữ liệu giá, hệ thống còn phân tích cảm xúc tin tức trong một phần độc lập.

## 7. News/Sentiment

**Thao tác:** Mở News Sentiment và chọn một tin `ANALYZED`. Khi minh họa lỗi cô lập,
dừng Python Sentiment và chờ tối đa 5 giây để banner chuyển sang `Limited availability`.

**Nói:**

> Java Worker chuẩn hóa và lưu tin tức, sau đó gửi nội dung sang Python để phân tích. Kết quả lưu nhãn cảm xúc, độ tin cậy và phiên bản model, nên có thể biết model nào đã tạo ra kết quả đó.

> Nếu Python phản hồi chậm hoặc bị lỗi, Worker chỉ thử lại trong giới hạn rồi tạm ngừng gọi để tránh lỗi lan rộng. UI báo Sentiment đang gián đoạn nhưng vẫn giữ kết quả cũ; Market Dashboard và Backtest kỹ thuật vẫn hoạt động độc lập.

**Thao tác bổ sung F016:** Khi service khả dụng, nhấn `Use as Strategy`, kiểm tra Composer tự chọn
`Sentiment Polarity`, rồi mở Search/Backtest và chỉ phần Sentiment provenance trong Result.

**Nói:**

> Khi bắt đầu Search, hệ thống chụp một snapshot News và model duy nhất cho toàn bộ Candidate. Backtest chỉ đọc snapshot đó, không gọi Python và không nhìn thấy News tương lai. Vì Result lưu snapshot, model, score và ngưỡng, em có thể giải thích và tái lập tín hiệu đã tạo ra.

## 8. Kết luận

> Trong flow vừa rồi, mỗi phần có một nhiệm vụ riêng: Market chuẩn hóa dữ liệu, Strategy tạo tín hiệu, Search tạo Candidate, Worker chạy Backtest, Evaluator tính kết quả, Leaderboard xếp hạng và Python phân tích Sentiment.

> Vì các phần giao tiếp qua contract chung, nhóm có thể thêm Strategy, đổi thuật toán Search, đổi nguồn Market Data hoặc tăng Worker mà không phải sửa toàn bộ hệ thống.

## Cấu hình demo nhanh

- Pair: `BTC/USDT`.
- Generator: `random-search@1.0.0`.
- Seed: `42`.
- Maximum Candidates: `3` hoặc `4`.
- Concurrency: `1` hoặc `2`.
- Top-K: `3`.

Nếu Search mất hơn 15 giây, mở một Experiment đã hoàn tất và tiếp tục từ Leaderboard. Không nói Redis bảo đảm exactly-once hoặc 100.000 Backtests đã được benchmark nếu chưa có báo cáo đo thật.
