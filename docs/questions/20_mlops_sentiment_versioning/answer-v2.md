# 20. Kết quả Sentiment truy vết model thế nào? — Answer V2

## Trả lời vấn đáp

Mỗi `SentimentResult` lưu `newsId`, nhãn sentiment, score, `modelName`, `modelVersion`, `inputVersion` và `createdAt`. Metadata này cho biết prediction được tạo bởi model và pipeline tiền xử lý nào. Nếu model v3 có regression, nhóm có thể lọc đúng kết quả do v3 tạo, so sánh trên cùng tập tin, rollback hoặc chạy lại chỉ phần bị ảnh hưởng. Ngoài version, hệ thống cần monitor inference failure, latency, input error và distribution drift.

## Chuỗi truy vết

```text
NewsItem + inputVersion
        → modelName/modelVersion
        → SentimentResult + score + createdAt
        → downstream Strategy/Experiment
```

## Nếu giảng viên hỏi sâu

- Model version mà thiếu preprocessing/input version vẫn chưa đủ tái lập.
- Cần log version đang deploy và correlation ID từ NewsItem đến prediction.
- Versioning hỗ trợ audit, canary comparison, rollback và selective reprocessing.
- Drift không chỉ là lỗi kỹ thuật; phân phối input/output có thể thay đổi dù service vẫn trả 200.

## Trạng thái và trade-off

Schema metadata/versioning đã có; automated drift detection và alerting pipeline còn Planned. Lưu version tăng metadata nhưng là điều kiện tối thiểu để quản trị prediction trong production.

## Câu chốt

> Một prediction phải trả lời được: tin nào, model nào, input version nào và được tạo lúc nào.

[Xem câu trả lời và bằng chứng chi tiết](answer.md)
