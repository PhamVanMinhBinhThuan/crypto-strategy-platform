# F014 Advanced Evidence Decision

## Kết luận T050

- Claim status: **NO_CLAIM**
- Lý do: repository có nhiều nền tảng kỹ thuật tốt, nhưng tại dry run hiện tại chưa có đủ cả ba phần
  **implementation + demo thật + measurement vượt yêu cầu cốt lõi**. Vì vậy không tự điền 5 điểm mở
  rộng chỉ dựa trên tên công nghệ.

## Ứng viên Machine Learning

- Core baseline already required: Sentiment Analysis là dòng cốt lõi số 17.
- Increment hiện có: Keras model bundle, frozen vocabulary, manifest checksum, model version,
  readiness gate và Python service boundary độc lập.
- Implementation links: `apps/sentiment/app/model`, `apps/sentiment/artifacts/active_release`,
  `docs/adr/0008-sentiment-service-boundary.md`.
- Evidence hiện có: 10 Python tests pass; retry/isolation Java và UI controlled pass.
- Thiếu để claim nâng cao: external TensorFlow runtime `READY`, inference/replacement demo và một phép
  đo phù hợp như quality evaluation, cold start hoặc inference latency. Sentiment cơ bản không tự trở
  thành mục nâng cao chỉ vì dùng model ML.

## Ứng viên Redis/Worker recovery

- Core baseline already required: reliability, queue/retry và failure scenario thuộc dòng 6/23.
- Increment hiện có: Redis Streams consumer group, pending reclaim, durable dual-layer dedup,
  transactional outbox và stable Job identity; test dùng Redis thật pass.
- Implementation links: `apps/worker`, `modules/persistence`, `docs/adr/0006-queue-worker-backtesting.md`,
  `docs/adr/0007-postgresql-redis-ownership.md`.
- Measurement mới: `performance.md` đo compute concurrency 1 so với 3 trong cùng JVM, median 2.298×,
  không timeout/duplicate. Đây là evidence performance cốt lõi, chưa phải scaling nhiều Worker process.
- Thiếu để claim nâng cao: benchmark nhiều Worker/process với queue và durable storage thật hoặc phép
  đo recovery time chứng minh increment vượt yêu cầu reliability cơ bản.

## Ứng viên Loop Engineering/Search orchestration

- Core baseline already required: Random Search và finite stop condition thuộc dòng 13.
- Increment hiện có: bounded in-flight allocation, durable decisions/version fence, recovery sweeper,
  deterministic Top-K và linked reproduction verification.
- Implementation links: `docs/adr/0016-search-coordinator-durable-orchestration.md`,
  `modules/experiment-execution`, `modules/search`.
- Thiếu để claim nâng cao: live run/recovery trên schema đúng migration và measurement so sánh hành vi
  hoặc hiệu năng với baseline tối thiểu.

## Điều kiện để đổi sang VERIFIED_ADVANCED

Chỉ chọn **một** ứng viên mạnh nhất, ghi architecture problem/trade-off, quay scenario thật và đính
kèm measurement có thể chạy lại trên commit sạch. Nếu không hoàn tất trước khi nộp, giữ nguyên
`NO_CLAIM`; phần implementation vẫn có thể dùng để giải thích kiến trúc nhưng không tự chấm điểm.
