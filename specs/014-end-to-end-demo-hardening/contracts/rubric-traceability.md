# Rubric Traceability Contract

F014 duy trì đúng 24 dòng chấm điểm từ file đánh giá: 23 tiêu chí cốt lõi và 1 dòng mở rộng có giá trị. Bảng evidence phải có một dòng cho từng tiêu chí, nhưng dòng mở rộng có thể ghi không tuyên bố điểm thay vì xây thêm chức năng.

| Nhóm | Tiêu chí phải có evidence |
|---|---|
| Architecture | Plugin extensibility; decoupling; replaceable components; scale/performance; realtime 4 timeframes; reliability/observability; reproducibility |
| Functional | Binance candles; MA/RSI/Bollinger/S-R; composite; trades; 4 metrics; Random Search + stop; Top-K; Entry/Exit/indicators; News; Sentiment |
| Documentation | README/runbook; architecture/ADR links; rubric evidence map |
| Demo | Full flow; ít nhất hai architecture/failure scenarios |
| Mở rộng tùy chọn | Chỉ khai phần thực sự vượt yêu cầu và có code/demo/measurement |

Mỗi dòng phải ghi criterion ID, owner, requirements, status, evidence link và gap/remediation. “Có code” không tự động là `VERIFIED`; phải có kết quả chạy tương xứng.
