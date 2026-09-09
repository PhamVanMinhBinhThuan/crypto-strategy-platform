# 12. Trade-off quan trọng nhất là gì? — Answer V2

## Trả lời vấn đáp

Trade-off lớn nhất là giữa khả năng scale/độ tin cậy với độ phức tạp. Nhóm chọn Queue–Worker bất đồng bộ để API phản hồi nhanh, retry được và scale ngang, nhưng phải chấp nhận eventual consistency, duplicate message, tracing khó hơn và cần idempotency. Ở cấp deployment, nhóm chọn Modular Monolith cho backend cốt lõi để giữ build, test và vận hành đơn giản; đổi lại phải dùng architecture test để boundary không suy thoái. Các quyết định được ghi trong ADR cùng lý do và hệ quả.

## Bảng trade-off

| Quyết định | Lợi ích | Chi phí |
| --- | --- | --- |
| Queue–Worker | Scale, retry, API không block | Eventual consistency, duplicate, tracing |
| Modular Monolith | Đơn giản cho nhóm nhỏ | Cần kỷ luật giữ boundary |
| Canonical model | Thay provider dễ | Có thể mất field đặc thù |
| Frozen provenance | Audit và tái lập | Tăng storage và metadata |

## Nếu giảng viên hỏi sâu

- Kiến trúc không tối ưu mọi quality attribute cùng lúc.
- Mỗi quyết định phải nêu được context, alternative và consequence.
- Nếu quy mô/team/driver đổi, quyết định hợp lý hôm nay có thể cần xem lại.

## Câu chốt

> Không có kiến trúc tốt tuyệt đối; quyết định tốt là quyết định phù hợp driver và nói rõ cái giá phải trả.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)
