# 22. Modular Monolith hay Microservices? — Answer V2

## Trả lời vấn đáp

Nhóm chọn Modular Monolith cho backend Java cốt lõi: các capability nằm trong một codebase/deployable nhưng có public API và boundary được kiểm tra bằng package rule/ArchUnit. Với nhóm nhỏ, lựa chọn này giảm chi phí deploy, network failure, distributed tracing và data consistency so với tách microservices sớm. Nhóm chỉ tách process khi có driver rõ: Worker có tải CPU dài và cần scale ngang; Sentiment dùng Python/ML runtime khác. Vì vậy đây không phải monolith không cấu trúc, cũng không phải microservices toàn phần.

## Quy tắc quyết định

| Driver | Quyết định |
| --- | --- |
| Nhóm nhỏ, nghiệp vụ còn phát triển | Giữ Modular Monolith |
| Worker cần scale/resource profile riêng | Tách Worker process |
| Python/ML runtime và failure mode riêng | Tách Sentiment service |
| Không có ownership/deployment driver | Chưa tách thêm service |

## Nếu giảng viên hỏi sâu

- Modular Monolith vẫn có thể tách service sau nếu boundary/contract tốt.
- Shared database là rủi ro; ownership và cấm truy cập chéo phải được enforce.
- Distributed monolith là đã chịu network/ops cost nhưng vẫn coupling chặt.
- Microservices chỉ đáng dùng khi scale, isolation, ownership hoặc cadence tạo đủ lợi ích.

## Trạng thái và trade-off

Backend cốt lõi giữ cấu trúc module; Worker và Sentiment được tách theo driver. Lựa chọn này giảm chi phí vận hành nhưng yêu cầu architecture test và review để module không dính lại với nhau.

## Câu chốt

> Giữ phần chưa cần phân tán ở Modular Monolith; chỉ tách process khi driver biện minh được chi phí.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)
