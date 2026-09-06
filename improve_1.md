# Improve 1 — Experiment Monitor UI

## Mục tiêu

Tinh chỉnh giao diện `Results`, `Failed candidates` và `Candidate pipeline` để
người dùng đọc nhanh trạng thái backtest, hiểu vì sao candidate không được xếp
hạng, và không bị choáng bởi các ID kỹ thuật.

Các hạng mục trong tài liệu đã được triển khai vào workspace và đang chờ người
dùng kiểm tra trực tiếp trên web. Việc triển khai cũng giữ nguyên invariant đã sửa:
candidate có canonical `backtest_result` không bị hiển thị `FAILED` cùng metrics.

## Phạm vi

- Experiment Monitor của Search/Backtest.
- Ba tab `Results`, `Failed candidates`, `Candidate pipeline`.
- Bộ đếm tab, bảng candidate và `Technical details`.
- Không thay đổi thuật toán backtest, evaluation, score hoặc ranking.

## Baseline đã sửa — Canonical backtest result

- Nếu đã tồn tại immutable `backtest_result`, API coi backtest là `SUCCEEDED` và
  cho phép trả metrics, kể cả dữ liệu lịch sử từng bị một attempt trùng ghi Job
  thành `FAILED`.
- Candidate chỉ được coi là failed khi Job terminal `FAILED` và hoàn toàn không
  có canonical `backtest_result`.
- Worker chỉ được mở attempt mới khi Job đang `QUEUED`; delivery trùng khi Job đã
  `RUNNING` hoặc terminal được ACK và không tạo attempt thứ hai.
- Recovery không được chọn attempt thuộc một Job đã terminal.

Các cải thiện UI trong tài liệu phải giữ nguyên các invariant này, không tự suy
diễn lại trạng thái từ attempt mới nhất.

## P0 — Sửa tính nhất quán của bộ đếm

### Hiện trạng

Ảnh kiểm tra hiển thị:

```text
Results 34
Failed candidates 7
Candidate pipeline 40
```

Theo định nghĩa hiện tại, Results và Failed phải là hai nhóm không giao nhau,
nên `34 + 7 = 41` trong khi tổng chỉ có `40` là không hợp lý.

### Cần làm

- Xác định count đang tính theo candidate duy nhất hay theo job/attempt history.
- Mọi count phải dùng `candidateId` duy nhất, không bị nhân bản bởi SQL join.
- `RESULTS` chỉ tính candidate có evaluation thành công.
- `FAILED` chỉ tính candidate có pipeline terminal failure và không có evaluation
  thành công được chọn làm kết quả canonical.
- `ALL` tính toàn bộ candidate duy nhất thuộc experiment.
- Trả ba count từ cùng một database snapshot hoặc cùng một API response để tránh
  số liệu được cập nhật ở các thời điểm khác nhau.
- Realtime refresh phải cập nhật đồng thời cả ba badge.

### Tiêu chí nghiệm thu

- `resultCount + failedCount <= totalCount` ở mọi trạng thái.
- Experiment terminal không có candidate mất khỏi cả Results lẫn Failed.
- Retry/multiple attempts không làm tăng số lượng candidate.
- Chuyển tab hoặc refresh không làm badge nhảy về số cũ.

## P0 — Quy chuẩn giao diện chung cho ba tab

### Mục tiêu

`Results`, `Failed candidates` và `Candidate pipeline` phải trông như ba view của
cùng một sản phẩm, không phải ba bảng được thiết kế độc lập.

### Cấu trúc cột

| Tab | Cột đề xuất |
| --- | --- |
| Results | Ranking, Candidate, Score, Return, Win rate, Drawdown, Trades, Actions |
| Failed candidates | Candidate, Failed stage, Reason, Actions |
| Candidate pipeline | Candidate, Backtest, Evaluation, Ranking, Outcome, Actions |

### Quy chuẩn chung

- Candidate luôn hiển thị theo một component chung: tên strategy ở dòng chính,
  `Candidate #` one-based là metadata phụ và parameter summary đã humanize ở dòng
  dưới; không lặp lại tên strategy.
- Results có thêm cột `Ranking` vì cần phân biệt rank, `Not in Top-K` và
  `Ineligible`; không dùng rank thay cho Candidate #.
- Cột cuối luôn tên `Actions`, có cùng chiều rộng và cùng action `View details`.
- `View details` mở cùng một Candidate Detail drawer và giữ nguyên tab, cursor,
  trang cùng vị trí cuộn khi đóng.
- Không mở Technical details trực tiếp trong bảng. ID, fingerprint, version và raw
  enum chỉ xuất hiện trong drawer.
- Chiều cao hàng ở trạng thái đóng phải ổn định; mở chi tiết không đẩy các hàng
  còn lại hoặc làm lệch cột.
- Số liệu canh phải và dùng tabular numerals; text, badge và action canh giữa theo
  chiều dọc.
- Badge dùng cùng kích thước, radius và semantic color token ở cả ba tab; luôn có
  text, không truyền nghĩa chỉ bằng màu.
- Dùng title case cho nhãn người dùng (`Succeeded`, `Backtest`, `Not in Top-K`),
  giữ enum viết hoa trong Technical details nếu cần đối chiếu.
- Tabs dùng cùng active indicator, hover/focus state và count badge. Count badge
  không thay đổi kích thước khi số liệu realtime cập nhật.
- Cả ba tab dùng cùng loading skeleton, empty state, error state và pagination:
  10 dòng/trang, `Previous`, `Showing X–Y of Z`, `Next`.
- Header bảng có nền riêng nhưng không sticky vì mỗi trang chỉ có tối đa 10 dòng.
- Trên mobile, ưu tiên horizontal scroll cho bảng; drawer chuyển thành full-screen
  sheet và có nút đóng luôn nhìn thấy.

### Tiêu chí nghiệm thu

- Chuyển qua lại ba tab không tạo cảm giác font, spacing, badge hoặc action thay
  đổi bất thường.
- Cùng một candidate có cùng tên, parameter summary và số thứ tự ở mọi tab.
- Không tab nào tăng chiều cao hàng khi mở Technical details.
- Người dùng học cách mở/đóng details một lần và dùng được giống nhau ở cả ba tab.
- Pagination, focus và scroll position được giữ độc lập cho từng tab.

## P1 — Thu gọn Technical details

### Hiện trạng

Khi mở `Technical details`, nội dung ID và các nút Copy làm cả hàng cao lên rất
lớn. Các trạng thái Backtest/Evaluation/Ranking bị đặt giữa khoảng trống nên khó
đối chiếu với nhau.

### Cần làm

- Không bung Technical details trực tiếp bên trong cell làm thay đổi chiều cao
  toàn bộ hàng.
- Dùng một Candidate Detail drawer thống nhất cho cả ba tab: rộng khoảng
  `560–640px` trên desktop và chiếm toàn màn hình trên mobile.
- Đặt Technical details thành section đóng mặc định bên trong drawer; không tạo
  một cơ chế mở details khác nhau ở từng tab.
- Chỉ cho mở một candidate tại một thời điểm.
- Giữ đầy đủ các trường kỹ thuật:
  - Candidate ID.
  - Candidate fingerprint.
  - Job ID.
  - Backtest result ID.
  - Evaluation result ID.
  - Ranking version.
- Mỗi giá trị có nút Copy và thông báo `Copied` cho screen reader.
- Nút Copy không được co hẹp hoặc xuống dòng thành `Cop` / `y` ở cột nhỏ.
- Đóng details bằng nút `×` có accessible name, phím `Escape` và trả focus về nút
  `View details` đã mở drawer.

### Tiêu chí nghiệm thu

- Mở Technical details không làm các cột trạng thái lệch khỏi hàng candidate.
- Copy hoạt động với toàn bộ giá trị, kể cả hash dài.
- Điều hướng bằng bàn phím và focus order hợp lý.
- Trên màn hình nhỏ, details không làm trang tràn ngang.

## P1 — Làm đẹp bảng Results

### Hiện trạng

- Technical details bung ngay trong cột Action làm hàng đầu tiên cao bất thường và
  khiến metrics của candidate khó đối chiếu với header.
- `View details` chỉ là text link nhỏ; cột đang ghi `Action` nhưng nội dung có nhiều
  thao tác và trạng thái mở rộng.
- Tên strategy bị lặp ở dòng chính và dòng parameter summary.
- Các metric có nhiều kiểu độ chính xác; rank, score và phần trăm chưa có phân cấp
  thị giác rõ ràng.

### Cần làm

- Đổi `Rank` thành `Ranking` để hỗ trợ ba trạng thái:
  - `#1`, `#2`, ... với candidate đã xếp hạng.
  - `Not in Top-K` với evaluation hợp lệ nhưng ngoài Top-K.
  - `Ineligible` với evaluation không đủ điều kiện xếp hạng.
- Giữ score là chỉ số chính, dùng 4 chữ số thập phân; Return, Win rate và Drawdown
  dùng 2 chữ số thập phân kèm `%`; Trades là số nguyên.
- Canh phải toàn bộ cột số và dùng tabular numerals để các hàng dễ so sánh.
- Return dương/âm có màu semantic nhẹ nhưng vẫn giữ dấu `+`/`−`, không dựa riêng
  vào màu.
- Có thể nhấn nhẹ Top 3 bằng rank badge; không tô nền toàn hàng hoặc làm các kết
  quả còn lại trông như bị vô hiệu hóa.
- Dùng Candidate component chung, ví dụ:

```text
Bollinger Bands · Candidate #39
Period 40 · Mean reversion · Deviation 2.25
```

- Đổi `Action` thành `Actions`; hiển thị `View details` dạng secondary button hoặc
  link-button có vùng bấm tối thiểu `44px`, focus ring rõ và không xuống dòng.
- Bỏ control Technical details khỏi từng hàng; nội dung kỹ thuật nằm trong drawer
  được mở bởi `View details`.
- Giữ row hover/focus nhẹ để người dùng theo dõi một hàng ngang qua nhiều metric.
- Khi không có metric, dùng `—`; không hiển thị `0` nếu giá trị thực sự chưa có.

### Tiêu chí nghiệm thu

- Mọi hàng Results có chiều cao đồng đều và metrics thẳng cột.
- Người dùng phân biệt được ranked, ngoài Top-K và ineligible ngay ở cột Ranking.
- Không còn strategy name lặp hoặc raw parameter name như `standardDeviation`.
- Action chính nhìn thấy ngay mà không cạnh tranh với dữ liệu kỹ thuật.

## P1 — Làm rõ lý do INELIGIBLE

### Hiện trạng

Candidate có thể Backtest và Evaluation đều `SUCCEEDED`, có score, nhưng Ranking
chỉ ghi `INELIGIBLE`. Người dùng không biết nguyên nhân.

### Cần làm

- Hiển thị lý do ngay cạnh badge, ví dụ:

```text
INELIGIBLE · 2/5 trades
```

- API/presentation model nên cung cấp eligibility reason có cấu trúc thay vì UI
  suy luận từ chuỗi lỗi.
- Với rule hiện tại, ghi rõ yêu cầu tối thiểu `5 trades`.
- Tooltip hoặc helper text giải thích candidate vẫn có metrics/score nhưng không
  được đưa vào Top-K.

### Tiêu chí nghiệm thu

- Người dùng hiểu lý do không đủ điều kiện mà không cần mở Technical details.
- Results vẫn chứa evaluation thành công nhưng ineligible.
- Candidate ineligible không được hiển thị rank hoặc `NOT_IN_TOP_K`.

## P1 — Làm đẹp bảng Failed candidates

### Hiện trạng

Dòng thất bại chủ yếu hiển thị enum `EXECUTION_FAILED`; Evaluation và Ranking là
`NOT STARTED`. Người dùng phải mở details hoặc xem terminal Worker để hiểu lỗi.
Dòng tổng hợp `This page: 7 × EXECUTION_FAILED` lặp lại số lượng trên badge tab,
đồng thời gom các nguyên nhân khác nhau vào cùng một error code quá chung chung.

### Cần làm

- Hiển thị failure stage và thông báo thân thiện ngay trong cột Result.
- Giữ error code gốc trong Technical details.
- Bỏ dòng `This page: N × EXECUTION_FAILED`.
- Không thêm failure summary thay thế; badge của tab đã cung cấp tổng số và mỗi dòng
  hiển thị nguyên nhân thân thiện, cụ thể hơn `EXECUTION_FAILED`.
- Ánh xạ các lỗi thường gặp, ví dụ:
  - Insufficient lookback.
  - Dataset unavailable/integrity failure.
  - Invalid strategy parameters.
  - Worker timeout/retry exhausted.
- Hiển thị stage theo title case (`Backtest`, `Evaluation`, `Ranking`) thay cho enum
  viết hoa toàn bộ.
- Đổi tên cột `Details` thành `Actions` nếu cột chứa thao tác mở chi tiết.
- Không lặp lại tên strategy ở cả tiêu đề và dòng mô tả. Ưu tiên dạng gọn như
  `Moving Average Crossover · Fast 10 · Slow 20`.
- Giới hạn reason ở tối đa hai dòng trong bảng; hiển thị đầy đủ trong Candidate
  Detail drawer và dùng tooltip chỉ như hỗ trợ bổ sung.
- Dùng cùng Candidate component, row height, Actions button và drawer với Results
  và Candidate pipeline.
- Nếu lỗi có thể retry, hiển thị trạng thái retry và lần thử hiện tại.
- Không hiển thị mâu thuẫn kiểu Backtest `FAILED` nhưng summary ghi `0 failed`.

### Tiêu chí nghiệm thu

- Một dòng failed cho biết candidate lỗi ở stage nào và vì sao.
- Số lượng trên badge tab và failure summary không lặp lại cùng một thông tin.
- Nếu có nhiều nguyên nhân cùng dùng `EXECUTION_FAILED`, summary vẫn tách chúng
  thành các nhóm dễ hiểu.
- Summary đại diện cho toàn bộ tập Failed candidates hoặc được ghi nhãn phạm vi
  rõ ràng; không khiến người dùng hiểu số liệu của một trang là tổng số.
- Thông báo công khai không lộ stack trace, SQL hoặc credential.
- Error code và correlation/job ID vẫn có thể Copy trong Technical details.

## P1 — Làm rõ bảng Candidate pipeline

### Hiện trạng

Pipeline cần cho người dùng đọc tiến trình `Backtest → Evaluation → Ranking`, nhưng
cột Result hiện trộn score, error và action. Score và liên kết còn có thể dính nhau,
ví dụ:

```text
Score 0.2153View details
```

### Cần làm

- Giữ ba stage thành ba cột riêng `Backtest`, `Evaluation`, `Ranking`, dùng chung
  badge component và vocabulary với hai tab còn lại.
- Đổi cột `Result` thành `Outcome`: chỉ hiển thị score hoặc failure summary ngắn,
  không đặt action trong cùng cell.
- Score canh phải và dùng cùng format 4 chữ số thập phân như Results.
- Thêm cột `Actions` riêng với cùng nút `View details` và cùng Candidate Detail
  drawer như Results và Failed candidates.
- Khi chưa có outcome, hiển thị `—`; khi đang xử lý, stage hiện tại dùng `Running`
  và các stage sau dùng `Not started`, không dùng trạng thái mơ hồ.
- Hash/ID không xuất hiện trực tiếp trong Outcome hoặc Actions.
- Pipeline giữ thứ tự Candidate # ổn định; realtime chỉ cập nhật badge/outcome tại
  chỗ, không tự đổi trang hoặc thay đổi thứ tự hàng.

### Tiêu chí nghiệm thu

- Không còn text/link dính nhau ở mọi breakpoint.
- Người dùng đọc được stage hiện tại và stage gây lỗi theo chiều trái sang phải.
- Outcome và Actions không cạnh tranh trong cùng một cell.
- Status, Candidate và action trông giống component tương ứng ở hai tab còn lại.

## P1 — Thiết kế lại Candidate Detail

### Hiện trạng

- `View authoritative Backtest` dùng thuật ngữ kỹ thuật, không nói rõ người dùng
  sẽ xem nội dung gì.
- `Close detail` không cho biết đây là đóng panel hay quay lại danh sách.
- Cả hai chỉ là text link ít nổi bật; action xem backtest còn bị đặt dưới một khối
  JSON dài nên rất khó tìm thấy.
- Metrics hiển thị số thập phân thô, ví dụ `-0.0359631706`, khó đọc nhanh.
- `Immutable strategy definition` hiển thị JSON dài ngay trong nội dung chính.
- Dataset, trạng thái và metrics nằm thành một cột dài với nhiều khoảng trống.
- Dữ liệu lịch sử từng có thể hiển thị `Backtest FAILED` cùng metrics từ một
  attempt thành công; UI mới phải giữ invariant canonical đã được backend sửa.

### Cần làm

- `View details` từ cả ba tab mở cùng một drawer; không render một panel rộng bằng
  toàn bộ màn hình phía dưới bảng.
- Header drawer gồm Candidate #, tên strategy, status/ranking badge và nút `×` có
  accessible name `Close candidate details`.
- Thay `Close detail` bằng nút `×` có border/hover/focus state rõ ràng và luôn nhìn
  thấy trong sticky drawer header. Nếu dùng route detail riêng thay vì drawer thì
  dùng `← Back to results`; không hiển thị đồng thời hai pattern.
- Đổi `View authoritative Backtest` thành `View full backtest result` hoặc
  `View trades and equity curve`; tuyệt đối không đưa thuật ngữ `authoritative`
  ra UI người dùng.
- Hiển thị `View full backtest result` thành primary button ở gần header hoặc ngay
  sau metric summary; không đặt sau JSON ở cuối trang.
- Chỉ hiện action xem backtest khi có canonical backtest result.
- Action hierarchy trong drawer: xem full backtest là primary, Copy/Technical
  details là secondary, đóng drawer là icon button.
- Format metrics cho người đọc:

```text
Total return      -3.60%
Win rate          68.18%
Maximum drawdown  16.19%
Trades            22
```

- Hiển thị strategy theo dạng dễ đọc, ví dụ
  `Bollinger Bands · Period 40 · Deviation 1.75`.
- Trình bày bốn metrics thành grid card `2 × 2` trên desktop và một cột trên mobile;
  số lớn, label nhỏ hơn và cùng format với bảng Results.
- Chuyển JSON immutable, ID, fingerprint, checksum và version vào Technical
  details có Copy; không để JSON chiếm nội dung chính.
- Đổi `Immutable strategy definition` thành `Strategy configuration` ở nội dung
  chính; hiển thị parameter đã humanize. Raw immutable JSON chỉ nằm trong Technical
  details và có nút Copy JSON.
- Gom dataset thành một dòng gọn như `BTC/USDT · 1h · 2,208 candles`; UTC range nằm
  ở dòng phụ và dùng format ngày giờ thân thiện nhưng vẫn ghi rõ `UTC`.
- Chia drawer thành các section `Overview`, `Performance`, `Strategy` và
  `Technical details`, dùng spacing nhất quán và không để khoảng trống lớn vô ích.
- Nếu cần trình bày retry, tách rõ canonical outcome và Attempt history; không
  trộn status của attempt thất bại với metrics của result thành công.
- Candidate failed không có canonical result phải hiển thị failure stage,
  failure message và retry state thay cho metrics.

### Tiêu chí nghiệm thu

- Người dùng hiểu hai action mà không cần biết thuật ngữ `authoritative`.
- Nút đóng luôn nhìn thấy; primary action xem backtest xuất hiện trước Technical
  details và không cần cuộn qua JSON để tìm.
- Không thể xuất hiện tổ hợp canonical `FAILED` cùng metrics hợp lệ.
- Metrics có định dạng phần trăm nhất quán và vẫn có raw value trong Technical
  details nếu cần đối chiếu.
- JSON kỹ thuật mặc định được ẩn.
- Quay lại danh sách giữ nguyên tab, trang và cursor trước đó.
- Panel/dialog dùng được bằng bàn phím, `Escape` và có quản lý focus đúng.
- Drawer từ Results, Failed candidates và Candidate pipeline có cùng layout; chỉ
  khác nội dung theo trạng thái candidate.

## P2 — Đổi nhãn Generation cho đúng Random Search

### Hiện trạng

`Generation 0`, `Generation 1` dễ khiến người dùng hiểu đây là thuật toán tiến
hóa. Random Search hiện chỉ sinh candidate tuần tự.

### Cần làm

- Đổi nhãn hiển thị thành một trong các phương án:
  - `Candidate #1`, `Candidate #2`, ... (ưu tiên cho người dùng phổ thông).
  - `Generated #0`, `Generated #1`, ... nếu cần giữ zero-based index.
- Có thể vẫn giữ `generationIndex` trong API và Technical details để tương thích.
- Dùng chỉ số one-based trên UI nếu chọn `Candidate #`.

### Tiêu chí nghiệm thu

- Nhãn không ngụ ý có evolutionary generation.
- Thứ tự pipeline vẫn ổn định và cursor pagination không thay đổi.

## P2 — Khả năng đọc và responsive

### Cần làm

- Tăng kích thước phần mô tả strategy/parameters nếu đang quá nhỏ.
- Tăng độ tương phản cho parameter summary và failure message; nội dung quan trọng
  không dùng font quá nhỏ hoặc màu quá mờ.
- Truncate hợp lý, có tooltip/title cho danh sách parameters dài.
- Không dùng sticky table header trong phiên bản này vì phân trang giới hạn 10 dòng.
- Trên màn hình nhỏ, dùng horizontal scroll có nhãn vùng rõ ràng; không ép cột
  thành những dòng chữ quá hẹp.
- Badge luôn có chữ, không chỉ phân biệt bằng màu.
- Bảo đảm contrast, focus state và thứ tự tab theo WCAG.

## Thứ tự triển khai đề xuất

1. Sửa count và truy vấn candidate canonical.
2. Tạo shared Candidate cell, Status badge, Actions và pagination cho cả ba tab.
3. Tạo Candidate Detail drawer dùng chung; chuyển Technical details và raw JSON
   vào drawer.
4. Chuẩn hóa bảng Results, Failed candidates và Candidate pipeline theo cấu trúc
   cột, typography và format dữ liệu đã chốt.
5. Bổ sung eligibility/failure reason có cấu trúc vào response nếu còn thiếu.
6. Hoàn thiện responsive, keyboard và screen-reader behavior.

## Checklist kiểm tra trên web

- Experiment đang chạy, hoàn tất và dừng giữa chừng.
- Candidate queued, running, succeeded, failed và retrying.
- Evaluation succeeded với cả eligible và ineligible.
- Ranking `RANKED`, `NOT_IN_TOP_K`, `INELIGIBLE`, `NOT_STARTED`.
- 0, 1, 10, 11 và hơn 20 candidate để kiểm tra pagination.
- Retry tạo nhiều attempt nhưng mỗi candidate chỉ được đếm một lần.
- Cùng một candidate hiển thị cùng Candidate #, strategy name và parameter summary
  trong cả ba tab.
- Mọi hàng giữ chiều cao ổn định; không có Technical details bung trực tiếp trong
  bảng.
- Mở cùng Candidate Detail drawer từ cả ba tab; đóng bằng `×`, `Escape` và kiểm tra
  focus quay lại đúng nút `View details`.
- Không còn nhãn `View authoritative Backtest` hoặc `Close detail`; primary action
  mới nhìn thấy trước khi mở Technical details.
- Results format Score 4 số thập phân, metric phần trăm 2 số thập phân và canh phải
  nhất quán.
- Copy từng ID và Copy JSON; nút Copy không xuống dòng ở desktop/mobile.
- Desktop, tablet và mobile width.
- Realtime update không xáo trộn trang người dùng đang xem.
