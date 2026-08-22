# Hướng dẫn thiết kế giao diện bằng Google Stitch

## 1. Mục tiêu

Tạo prototype giao diện cho Crypto StrategyLab trước khi viết feature spec và code Frontend.

Kết quả cần đạt:

- Có năm màn hình chính đúng phạm vi MVP.
- Các màn hình dùng chung phong cách và thanh điều hướng.
- Có thể bấm qua luồng demo chính.
- Có các trạng thái đang tải, trống, lỗi và mất kết nối cần thiết.
- Có ảnh xuất ra để lưu trong repository và dùng khi viết Spec Kit.

Giai đoạn này chưa cần database, API hoạt động hoặc code Next.js.

## 2. Thông tin phải chốt trước khi vẽ

| Nội dung | Giá trị dùng cho bản đầu |
|---|---|
| Tên sản phẩm | Crypto StrategyLab |
| Nền tảng | Web trên máy tính |
| Kích thước thiết kế | 1440px |
| Chủ đề | Bảng điều khiển tài chính nền tối |
| Cặp giao dịch mẫu | BTC/USDT |
| Khung thời gian mặc định | 5m, 15m, 1h, 4h |
| Khung thời gian có thể chọn | 1m, 5m, 15m, 30m, 1h, 2h, 4h, 1d |
| Chiến lược | MA, RSI, Bollinger Bands, Support/Resistance |
| Cách kết hợp MVP | Majority Vote |
| Chỉ số Backtest | Total Return, Win Rate, Max Drawdown, Number of Trades |
| Sentiment | Positive, Neutral, Negative |
| Ngoài phạm vi | Đăng nhập phức tạp, mobile, tiền thật, Genetic Algorithm |

Nếu một nội dung trong bảng thay đổi, cập nhật file này trước khi yêu cầu Stitch sửa hàng loạt màn hình.

## 3. Phân chia công việc trên Stitch

- Chỉ tạo **một project Stitch chung** tên `Crypto StrategyLab`.
- Một người tạo màn hình Market Dashboard trước để chốt phong cách chung.
- Mỗi thành viên chỉ chỉnh sửa những màn hình được giao.
- Không tạo lại sidebar, header hoặc bảng màu khác nhau trên từng màn hình.
- Mỗi màn hình chỉ giữ một bản chính và tối đa một bản thay thế.
- Không lấy code Stitch đưa thẳng vào `apps/web/` trong giai đoạn thiết kế.

Phân chia đề xuất:

| Người | Màn hình phụ trách |
|---|---|
| Thành viên UI 1 | Market Dashboard, Strategy Composer |
| Thành viên UI 2 | Backtest Results, Search & Leaderboard, News Sentiment |

## 4. Tạo project Stitch

1. Truy cập `https://stitch.withgoogle.com/` và đăng nhập.
2. Tạo project mới hoặc nhập prompt tại màn hình bắt đầu.
3. Đặt tên project là `Crypto StrategyLab`.
4. Chọn loại giao diện Web/Desktop nếu Stitch hiển thị lựa chọn này.
5. Tạo Market Dashboard trước.
6. Chia sẻ link project cho cả nhóm.
7. Ghi link vào `docs/ui/README.md`.

Tên nút trên Stitch có thể thay đổi theo phiên bản, nhưng quy trình vẫn là: tạo project, nhập prompt, sinh giao diện, chỉnh sửa và tạo prototype.

## 5. Prompt nền chung

Dùng prompt này khi tạo màn hình đầu tiên. Các màn hình sau phải yêu cầu Stitch giữ nguyên design system đã tạo.

```text
Design a desktop web application called "Crypto StrategyLab" for university
students who want to test cryptocurrency trading strategies.

Create a consistent design system for all screens.

Visual style:
- Modern dark financial dashboard
- Desktop width 1440px
- Dark navy background
- Clean, practical and easy to understand
- Avoid excessive gradients, glass effects and decorative cards
- Green for profit, BUY and positive sentiment
- Red for loss, SELL and negative sentiment
- Yellow for warnings and neutral sentiment
- Clear typography, spacing and visual hierarchy
- Accessible contrast and readable labels

Shared application shell:
- A left sidebar with Market Dashboard, Strategy Composer, Backtest,
  Search Leaderboard and News Sentiment
- A top header with the app name, BTC/USDT current price,
  Binance connection status, realtime status and settings icon
- Highlight the current page in the sidebar
- Use the same sidebar, header, buttons, form controls, cards and colors
  across every screen

Product constraints:
- Desktop web only
- No authentication screens
- No mobile screens
- No real-money trading or order placement
- Use realistic sample cryptocurrency data
- Clearly show loading, empty, error and disconnected states where relevant
```

## 6. Màn hình 01 — Market Dashboard

### Mục đích

Cho phép người dùng theo dõi giá realtime trên 1–4 biểu đồ và chọn khung thời gian độc lập cho từng biểu đồ.

### Thành phần bắt buộc

- Bộ chọn số lượng biểu đồ: 1, 2 hoặc 4.
- Mỗi biểu đồ có pair, timeframe, giá hiện tại và trạng thái realtime riêng.
- Biểu đồ nến và volume.
- Bốn timeframe mặc định: 5m, 15m, 1h, 4h.
- Bộ chọn timeframe: 1m, 5m, 15m, 30m, 1h, 2h, 4h, 1d.
- Khi đổi timeframe của một chart, chỉ chart đó tải lại; các chart khác và toàn trang không reload.
- Khu vực chọn strategy đang hiển thị trên chart.
- Nút `Create Composite` và `Run Backtest`.
- Trạng thái loading, empty data, API error và Binance disconnected.

### Prompt tạo màn hình

```text
Using the existing Crypto StrategyLab design system, create the
"Market Dashboard" desktop screen.

Layout:
- Keep the shared sidebar and top header
- Add a toolbar with a trading pair selector and a 1, 2 or 4 chart layout selector
- Display four candlestick charts in a responsive 2x2 grid by default
- Include a compact strategy summary panel on the right

Each chart must include:
- Trading pair selector
- Independent timeframe selector with 1m, 5m, 15m, 30m, 1h, 2h, 4h and 1d
- Candlestick chart and volume
- Current price and percentage change
- Realtime connection indicator
- Strategy overlay controls for MA, Bollinger Bands, Support and Resistance
- Space for BUY, SELL, Entry and Exit markers

Interaction requirement:
- Changing the timeframe of one chart must update only that chart
- Other charts must keep their current data and timeframe
- Do not reload the full dashboard

The strategy summary panel must include:
- Selected strategies
- A "Create Composite" button
- A "Run Backtest" button

Use realistic BTC/USDT sample data.
Do not add trading order forms, wallet balances or authentication.
```

### Prompt tạo biến thể

```text
Create variants of this same Market Dashboard for:
1. One large chart
2. Two charts side by side
3. Four charts in a 2x2 grid
4. Loading market data
5. Empty market data
6. Binance disconnected
7. API error with a retry action

Keep all variants visually consistent. Do not create new application pages.
```

### Dữ liệu mẫu

```text
Pair: BTC/USDT
Current price: $115,420.50
Change: +2.35%
Default timeframes: 5m, 15m, 1h, 4h
Available timeframes: 1m, 5m, 15m, 30m, 1h, 2h, 4h, 1d
Strategies: MA Crossover, RSI, Bollinger Bands
Connection: Realtime connected
```

## 7. Màn hình 02 — Strategy Composer

### Mục đích

Cho phép chọn, cấu hình và kết hợp nhiều strategy thành Composite Strategy.

### Thành phần bắt buộc

- Danh sách MA, RSI, Bollinger Bands và Support/Resistance.
- Form tham số thay đổi theo strategy.
- Khu vực hiển thị các strategy đã chọn.
- Phương thức kết hợp `Majority Vote`.
- Tóm tắt BUY/SELL/HOLD.
- Validation khi tham số không hợp lệ.
- Nút lưu Composite, chạy Backtest và đưa các strategy đã chọn vào Search Space.

### Prompt tạo màn hình

```text
Using the same Crypto StrategyLab application shell and design system,
create the "Strategy Composer" desktop screen.

The screen must include:
- A strategy library containing MA Crossover, RSI, Bollinger Bands
  and Support/Resistance
- A clear selected state for each strategy
- A parameter form that changes based on the selected strategy
- A selected strategies panel
- A Composite Strategy section using Majority Vote
- A simple explanation of BUY, SELL and HOLD signal aggregation
- Buttons for "Save Composite", "Run Backtest" and "Add to Search Space"

Show realistic example parameters:
- MA Crossover: fast period 10, slow period 30
- RSI: period 14, oversold 30, overbought 70
- Bollinger Bands: period 20, standard deviation 2
- Support/Resistance: lookback 50

Include states for invalid parameters, no strategy selected,
strategy selected and composite successfully created.
Do not include strategy code editors or advanced genetic configuration.
```

## 8. Màn hình 03 — Backtest Results

### Mục đích

Cho phép cấu hình một lần Backtest và xem kết quả, metrics cũng như các giao dịch Entry/Exit.

### Thành phần bắt buộc

- Pair, timeframe, khoảng ngày và vốn ban đầu.
- Strategy hoặc Composite Strategy được chọn.
- Nút chạy Backtest và trạng thái tiến trình.
- Bốn metrics bắt buộc.
- Equity curve.
- Biểu đồ có Buy/Sell hoặc Entry/Exit marker.
- Bảng danh sách trade.
- Click một trade phải highlight đúng Entry và Exit trên chart.
- Chi tiết lần chạy gồm Experiment ID, strategy version, parameters, dataset, pair và timeframe.
- Các trạng thái chưa chạy, đang chạy, thành công, không có trade và thất bại.

### Prompt tạo màn hình

```text
Using the same Crypto StrategyLab application shell and design system,
create the "Backtest Results" desktop screen.

Add a compact backtest configuration section with:
- Trading pair
- Timeframe
- Start date and end date
- Initial capital
- Trading fee
- Selected strategy or composite strategy
- A primary "Run Backtest" button

Add a results section with exactly these four primary metric cards:
- Total Return
- Win Rate
- Max Drawdown
- Number of Trades

Below the metrics, show:
- An equity curve
- A candlestick chart with Entry and Exit markers
- A trade history table with entry time, exit time, entry price,
  exit price, profit/loss and return percentage
- Clicking a trade row must highlight its exact Entry and Exit points on the chart

Add a reproducibility details section with:
- Experiment ID
- Strategy or composite strategy version
- Exact strategy parameters
- Dataset identifier and historical date range
- Trading pair and timeframe
- Backtest execution time

Use sample values:
- Total Return: +12.5%
- Win Rate: 58%
- Max Drawdown: -7.2%
- Number of Trades: 42

Create states for before running, running with progress, successful result,
no trades generated and failed backtest with retry.
```

## 9. Màn hình 04 — Search & Leaderboard

### Mục đích

Cho phép chạy Random Search để sinh các tổ hợp strategy cùng tham số, Backtest từng candidate và xem Top-K kết quả.

### Thành phần bắt buộc

- Search Space gồm các strategy được phép tham gia: MA, RSI, Bollinger Bands và Support/Resistance.
- Số strategy tối thiểu và tối đa trong một Composite Strategy.
- Không gian tham số riêng của từng strategy.
- Điều kiện dừng theo số candidate, thời gian hoặc số vòng liên tiếp không cải thiện.
- Metric dùng để xếp hạng.
- Nút Start và Stop.
- Progress, candidate hiện tại, bước đang chạy, số candidate đã thử, số job lỗi và thời gian chạy.
- Top-K Leaderboard và hành động xem/chạy lại kết quả.
- Leaderboard tự cập nhật khi có candidate tốt hơn, không reload trang.
- Chi tiết Top-K có strategy version, parameters, dataset và Experiment ID.
- Các trạng thái cấu hình, đang chạy, dừng, hoàn thành và lỗi.

### Prompt tạo màn hình

```text
Using the same Crypto StrategyLab application shell and design system,
create the "Search & Leaderboard" desktop screen.

Create a Random Search configuration panel with:
- A search space selector containing MA, RSI, Bollinger Bands
  and Support/Resistance
- Minimum and maximum number of strategies in each composite candidate
- Parameter ranges for every selected strategy
- Trading pair and timeframe
- Ranking metric selector
- Maximum candidate count
- Maximum duration
- Stop after a configurable number of iterations without improvement
- Top-K value
- Primary "Start Search" button

When a search is running, show:
- Progress bar
- Candidates evaluated
- Current generated composite candidate, for example MA20 + RSI14 + Support/Resistance
- Current pipeline step: Generating, Backtesting, Evaluating or Ranking
- Elapsed time
- Failed job count
- Current best score
- A visible "Stop Search" button

Create a Top-K leaderboard table with:
- Rank
- Strategy and parameters
- Total Return
- Win Rate
- Max Drawdown
- Number of Trades
- Score
- Actions to view details or run backtest again

The leaderboard must update in realtime without a full page reload.
When the user clicks a Top-K result, open a detail drawer or modal showing:
- Experiment ID
- Strategy and composite version
- Exact parameters
- Dataset and historical date range
- Pair and timeframe
- Metrics and execution time
- Actions to visualize trades or run the backtest again

Use Random Search only. Do not add Genetic Algorithm controls.
Include configuration, running, stopped, completed and failed states.
```

## 10. Màn hình 05 — News Sentiment

### Mục đích

Cho phép theo dõi tin tức crypto và kết quả phân tích sentiment cơ bản mà không làm ảnh hưởng luồng Market Dashboard.

### Thành phần bắt buộc

- Danh sách tin gồm tiêu đề, nguồn và thời gian.
- Nhãn Positive, Neutral hoặc Negative.
- Điểm sentiment nếu cần.
- Filter theo sentiment và từ khóa.
- Khu vực xem chi tiết hoặc tóm tắt tin.
- Trạng thái loading, empty và News Service unavailable.
- Một hành động tùy chọn để thêm `SentimentStrategy` vào Search Space, dùng để minh họa khả năng mở rộng.

### Prompt tạo màn hình

```text
Using the same Crypto StrategyLab application shell and design system,
create the "News Sentiment" desktop screen.

Add a news toolbar with:
- Search input
- Sentiment filter for All, Positive, Neutral and Negative
- Source filter
- Refresh button

Display a clean list of cryptocurrency news items. Each item must show:
- Headline
- Source
- Published time
- Related trading symbol
- Short summary
- Positive, Neutral or Negative sentiment label
- Confidence and polarity score when available

Add a compact sentiment summary showing the number or percentage of
positive, neutral and negative news items.

Create states for loading, populated list, no matching news and
News Sentiment service unavailable. When the service is unavailable,
clearly state that Market Dashboard still works normally.

Add an optional "Use Sentiment in Strategy Search" action. This action is
only for demonstrating how SentimentStrategy could be added to the search space;
do not add advanced machine learning configuration controls.
```

## 11. Prompt chỉnh sửa dùng chung

Mỗi lần chỉ yêu cầu Stitch sửa một vấn đề. Không gộp quá nhiều thay đổi trong cùng prompt.

### Giảm giao diện rối

```text
Reduce unnecessary cards and decorative elements. Increase space for the
main task and keep the visual hierarchy clear.
```

### Đồng bộ với màn hình Market Dashboard

```text
Match this screen to the approved Market Dashboard design system.
Use exactly the same sidebar, header, colors, typography, spacing,
buttons and form controls.
```

### Làm rõ trạng thái lỗi

```text
Add clear loading, empty, validation error, service error and retry states.
Do not use only color to communicate status; include readable text labels.
```

### Kiểm tra đúng phạm vi MVP

```text
Remove features outside the MVP, including authentication, wallet balance,
real-money trading, order placement, Genetic Algorithm and mobile layout.
```

### Tạo bản dễ code

```text
Simplify the layout into reusable sections and components that can later be
implemented with Next.js and JSX. Keep spacing and component behavior consistent.
```

## 12. Luồng prototype cần nối

```text
Market Dashboard
  -> Strategy Composer
      -> Backtest Results
          -> Search & Leaderboard
      -> Search & Leaderboard
          -> Click Top-K result
              -> View strategy details
              -> Visualize chart and trades

Market Dashboard
  -> News Sentiment
      -> Add SentimentStrategy to Search Space (optional extension demo)
      -> Search & Leaderboard
```

Các nút cần nối tối thiểu:

| Từ màn hình | Nút hoặc hành động | Đến màn hình |
|---|---|---|
| Market Dashboard | Create Composite | Strategy Composer |
| Market Dashboard | Run Backtest | Backtest Results |
| Strategy Composer | Run Backtest | Backtest Results |
| Strategy Composer | Start Search | Search & Leaderboard |
| Backtest Results | Optimize Parameters | Search & Leaderboard |
| Search & Leaderboard | Click Top-K result | Mở chi tiết strategy/experiment |
| Chi tiết Top-K | Visualize Trades | Backtest Results với chart và trades tương ứng |
| Backtest Results | Click một trade | Highlight Entry/Exit trên chart |
| Sidebar | News Sentiment | News Sentiment |
| News Sentiment | Use Sentiment in Strategy Search | Search & Leaderboard |
| Sidebar | Market Dashboard | Market Dashboard |

## 13. Xuất và lưu kết quả

Chỉ xuất bản chính đã được nhóm duyệt:

```text
docs/ui/screens/
├── market-dashboard.png
├── strategy-composer.png
├── backtest-results.png
├── search-leaderboard.png
└── news-sentiment.png
```

Sau khi xuất:

1. Ghi link project Stitch vào `docs/ui/README.md`.
2. Điền người phụ trách và trạng thái từng màn hình.
3. Đổi trạng thái từ `Bản nháp` thành `Đã duyệt`.
4. Commit ảnh và tài liệu lên GitHub.
5. Dùng ảnh cùng prototype đã duyệt làm đầu vào viết feature spec.

## 14. Checklist duyệt giao diện

### Toàn bộ sản phẩm

- [ ] Có đủ năm màn hình chính.
- [ ] Các màn hình dùng cùng sidebar, header và design system.
- [ ] Không có tính năng ngoài phạm vi MVP.
- [ ] Có thể bấm qua toàn bộ luồng demo.
- [ ] Mỗi chart có thể đổi timeframe độc lập mà không reload toàn trang.
- [ ] Search sinh tổ hợp strategy, không chỉ tối ưu một strategy đơn lẻ.
- [ ] Click Top-K xem được thông tin strategy, experiment và trade visualization.
- [ ] Thuật ngữ Strategy, Backtest, Trade và Sentiment được dùng thống nhất.
- [ ] Nội dung quan trọng không chỉ được biểu đạt bằng màu sắc.

### Trạng thái hệ thống

- [ ] Có trạng thái đang tải.
- [ ] Có trạng thái không có dữ liệu.
- [ ] Có validation cho form.
- [ ] Có lỗi API hoặc service và hành động thử lại.
- [ ] Có trạng thái mất kết nối Binance.
- [ ] Search hiển thị candidate hiện tại và bước Generating/Backtesting/Evaluating/Ranking.
- [ ] Leaderboard có thể hiện trạng thái tự cập nhật realtime.
- [ ] Thể hiện News Service lỗi không làm Market Dashboard ngừng hoạt động.

### Khả năng tái lập kết quả

- [ ] Chi tiết kết quả có Experiment ID.
- [ ] Có strategy/composite version và đầy đủ parameters.
- [ ] Có dataset, khoảng lịch sử, pair và timeframe.
- [ ] Click trade highlight đúng Entry và Exit trên chart.

### Bàn giao

- [ ] Link Stitch đã được ghi trong `README.md`.
- [ ] Mỗi màn hình có người phụ trách.
- [ ] Năm ảnh chính đã được lưu trong `docs/ui/screens/`.
- [ ] Cả nhóm đã review và chốt phiên bản chính.
