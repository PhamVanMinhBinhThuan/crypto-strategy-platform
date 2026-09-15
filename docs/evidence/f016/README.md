# F016 Sentiment Strategy — Evidence Index

Thư mục này lưu minh chứng cho việc Sentiment đã vượt pipeline phân tích News cơ bản và trở thành
một Strategy có thể dùng trong Single, Composite, Search, Backtest và Reproduction.

## Quy tắc ghi nhận

- Mỗi record phải ghi commit, trạng thái working tree, UTC timestamp, môi trường và lệnh/thao tác.
- Phân biệt rõ `AUTOMATED`, `CONTROLLED_FIXTURE` và `LIVE`; fixture không thay thế minh chứng hạ tầng live.
- Screenshot/video phải che email, token, connection string và dữ liệu cá nhân không cần thiết.
- Không đánh dấu `VERIFIED` nếu test bắt buộc bị skip hoặc external dependency chưa được chạy.
- Kết quả generated/local không commit nếu chứa secret; chỉ link tương đối tới artifact đã redact.

## Evidence cần thu

| ID | Minh chứng | Cách chụp/quay | Trạng thái |
|---|---|---|---|
| EV-016-01 | Catalog có Strategy thứ năm và bốn tham số | Composer → chọn `Sentiment Polarity` → tab Parameters; chụp cả disclaimer | PLANNED |
| EV-016-02 | News dẫn sang Strategy | News Sentiment → `Use as Strategy`; quay lúc Composer tự chọn đúng plugin | PLANNED |
| EV-016-03 | Search dùng finite domains và một frozen snapshot | Search mới → chọn Sentiment; chụp domains, snapshot note và Candidate progress | PLANNED |
| EV-016-04 | Sentiment hoạt động trong Composite | Tạo/publish Composite có Sentiment; chụp policy, components và weights/vote | PLANNED |
| EV-016-05 | Result giải thích được tín hiệu | Result → Evidence; chụp snapshot/model/article count/score/threshold/reason | PLANNED |
| EV-016-06 | Không look-ahead và tái lập | Lưu automated report; nếu live, quay reproduction tới `MATCHED` | AUTOMATED VERIFIED |
| EV-016-07 | Cô lập lỗi | Dừng Python; quay banner `DEGRADED`, Result cũ và technical Backtest vẫn dùng được | PARTIAL |
| EV-016-08 | Responsive/keyboard | Browser report tại 360/768/1024/1440 và keyboard CTA | AUTOMATED VERIFIED |
| EV-016-09 | Java/Web quality gates | Ghi kết quả thật vào `verification.md` | AUTOMATED VERIFIED |
| EV-016-10 | Shared database migration | Xác nhận hai migration F016, trigger snapshot và Result column trên PostgreSQL nhóm | LIVE VERIFIED |

## Kịch bản quay ngắn

1. Mở News Sentiment, chỉ một tin đã `ANALYZED`, rồi nhấn `Use as Strategy`.
2. Trong Composer, chỉ plugin thứ năm, disclaimer và bốn tham số.
3. Mở Search, chọn riêng Sentiment hoặc ghép Composite, đặt Candidate limit nhỏ và Start.
4. Khi hoàn tất, mở Candidate/Result và chỉ provenance: cùng snapshot/model nhưng parameter khác nhau.
5. Bấm Reproduce và đợi kết quả; quay `MATCHED` nếu live flow đã chạy đủ.
6. Dừng Python Sentiment, refresh News để thấy `DEGRADED`, sau đó mở lại Result cũ hoặc chạy technical
   Backtest để chứng minh lỗi không lan sang phần còn lại.

## Lệnh kiểm tra

Chạy với Java 21 và Node 22 như yêu cầu trong
[`quickstart.md`](../../../specs/016-sentiment-strategy/quickstart.md). Record cuối cùng nằm tại
[`verification.md`](verification.md); lệnh nào chưa chạy được phải ghi `BLOCKED` kèm lý do cụ thể.

## Mẫu record thủ công

```markdown
### EV-016-XX — <tên>

- Status: PLANNED | BLOCKED | PARTIAL | VERIFIED
- Profile: LIVE | CONTROLLED_FIXTURE | AUTOMATED
- Commit/base SHA: <sha>
- Working-tree patch SHA-256: <sha hoặc clean>
- Captured at: <UTC ISO-8601>
- Environment: <OS, Java, Node, service profile>
- Action/command: <thao tác hoặc lệnh>
- Expected: <kết quả cần chứng minh>
- Observed: <kết quả thực tế>
- Artifact: <đường dẫn/video timestamp/result ID đã redact>
- Limitations: <none hoặc giới hạn>
```
