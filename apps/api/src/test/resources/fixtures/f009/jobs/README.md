# Fixture trạng thái Job F-009

Các fixture khóa public representation cho `QUEUED`, `RUNNING`, `RETRY_SCHEDULED`,
`CANCELLED`, `FAILED` và `SUCCEEDED`. Từ “completed” trong task tương ứng với
`SUCCEEDED` ở lifecycle Job; `COMPLETED` chỉ dùng cho Experiment/Backtest Result.

Failure lịch sử nằm trong resource trả về với HTTP `200`, không bị biến thành lỗi đọc.
