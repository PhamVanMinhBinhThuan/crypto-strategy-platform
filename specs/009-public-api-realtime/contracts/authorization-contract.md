# Contract authorization F-009

- JWT subject hợp lệ được map thành authenticated UUID; token thiếu/sai/hết hạn trả một
  public authentication error không phân biệt chi tiết nguyên nhân.
- Experiment là owner root. Candidate, Job, Attempt, Result, Evaluation và Leaderboard
  authorize qua parent chain đã chốt trong F-005/F-006.
- User Strategy authorize trực tiếp theo owner; system Strategy, Market và News là shared
  data nhưng vẫn qua API boundary.
- Cross-owner và missing resource dùng cùng inaccessible response; identifier không cấp
  quyền.
- WebSocket subscription phải authorize trước confirmation/snapshot/event. Connection đóng
  và dừng phát private data tại thời điểm sớm hơn giữa JWT gốc hết hạn và maximum lifetime;
  client refresh session ngoài WebSocket, lấy ticket mới, reconnect và resubscribe.
- Internal sentiment audit và trusted worker operations không được gọi từ browser; chúng
  dùng boundary/credential riêng, không coi user JWT là service authority.
