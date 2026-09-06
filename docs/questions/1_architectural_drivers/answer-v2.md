# 1. Architectural drivers là gì? — Answer V2

## Trả lời vấn đáp

Architectural drivers là những yêu cầu có ảnh hưởng lớn và **buộc kiến trúc phải có hình dạng nhất định**, chứ không phải toàn bộ yêu cầu chức năng. Crypto Strategy Lab có tám driver chính: dễ thêm Strategy, scale số lượng Backtest, hiệu năng, realtime, độ tin cậy, dễ thay đổi thuật toán, quan sát được và tái tạo được kết quả. Từ đó nhóm mới chọn module boundary, Plugin/Registry, provider adapter, Queue–Worker, idempotency và frozen provenance. Nhóm không chọn pattern hay công nghệ chỉ vì chúng phổ biến.

## Minh họa logic thiết kế

```text
Driver → Quyết định kiến trúc → Cơ chế kiểm chứng
Scale Backtest → Queue + Worker → throughput/queue-lag benchmark
Dễ thêm Strategy → Plugin + Registry → architecture/unit test
Tái tạo kết quả → Frozen Manifest → reproduction test
```

## Nếu giảng viên hỏi sâu

- **Modifiability:** thêm MACD hay Genetic Search mà không sửa Backtester.
- **Scalability:** đưa tác vụ dài qua queue và scale ngang Worker.
- **Reliability:** retry có giới hạn, recovery và chống xử lý trùng.
- **Reproducibility:** lưu version, seed, dataset và fingerprint.

## Trạng thái và trade-off

Các cơ chế kiến trúc chính đã có code/test. Những mục tiêu định lượng như p95 realtime hoặc throughput khi tăng Worker vẫn phải benchmark; không được xem số mục tiêu là kết quả đã đạt. Kiến trúc thêm contract và metadata, đổi lại hệ thống dễ thay đổi và truy vết hơn.

## Câu chốt

> Driver trả lời câu hỏi “vì sao kiến trúc phải được thiết kế như vậy?”.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)
