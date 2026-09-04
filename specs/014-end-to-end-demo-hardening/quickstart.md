# Quickstart: F-014 End-to-End Demo

Đây là đường kiểm chứng dự kiến. Gate chưa chạy trên commit ứng viên phải giữ trạng thái Planned/Blocked, không ghi Pass.

## 1. Prerequisites

- JDK 21, Node.js 22/npm, Python 3.11 hoặc 3.12
- PostgreSQL/Supabase và Redis reachable
- Biến môi trường theo `.env.example`; credential thật chỉ inject local/CI
- Binance connectivity cho live profile; fixture chỉ là fallback có nhãn

## 2. Baseline quality gates

```bash
./gradlew clean check
cd apps/web
npm ci
npm run check
cd ../sentiment
python -m pytest
```

Nếu `npm run check` không có, dùng đúng các script format/lint/typecheck/test/build trong `apps/web/package.json` và ghi từng lệnh vào evidence.

## 3. Startup order

1. PostgreSQL/Supabase và Redis.
2. Sentiment service; nếu kiểm thử degraded mode thì ghi rõ đang tắt.
3. API application.
4. Worker application.
5. Next.js web application.
6. Kiểm tra health/readiness và xác nhận UI hiển thị đúng live hoặc fixture mode.

Lệnh startup chính xác sẽ được xác nhận từ build scripts trong implementation; runbook cuối không yêu cầu sửa source hay database thủ công.

## 4. Main demo flow

1. Đăng nhập, mở Market, hiển thị bốn chart và đổi timeframe độc lập.
2. Xác nhận catalog có MA, RSI, Bollinger Bands và Support/Resistance.
3. Chọn Strategy/composite, dataset, Random Search, search space và stop condition hữu hạn.
4. Start Search và quan sát progress tới terminal state.
5. Mở Top-K Leaderboard rồi chọn entry đầu.
6. Xác nhận Result có Entry/Exit, Trades, Return, Win Rate, Maximum Drawdown và Number of Trades.
7. Mở provenance và reproduction; run mới liên kết nguồn và trả `MATCHED` hoặc `MISMATCHED`.
8. Mở News/Sentiment và xác nhận provenance/degraded state trung thực.

Mục tiêu: người đã đăng nhập hoàn thành trong tối đa 10 phút.

## 5. Failure scenarios

- **Sentiment isolation**: dừng sentiment, xác nhận degradation được thấy nhưng Market và technical Backtest vẫn hoạt động; sau recovery reconcile từ nguồn authoritative.
- **Async recovery**: interrupt Worker hoặc queue path tại điểm xác định, khởi động lại/reclaim, xác nhận Job không mất và không có accepted Result trùng.

## 6. Performance, security and UI

- Chạy workload cố định ba lần; lưu mọi result, median, timeout/failure và environment profile.
- Scan tracked source, config, committed evidence và browser build artifact; yêu cầu 0 privileged credential thật.
- Chạy keyboard flow và viewport 360, 768, 1024, 1440 px; bảng rộng chỉ cuộn trong vùng bảng và trạng thái không phụ thuộc riêng vào màu.

## 7. Evidence and release

Lưu Evidence Record theo `contracts/evidence-record-contract.md`, ánh xạ đủ 24 tiêu chí. Release checklist ghi dependency, known limitation, fallback/rollback và mọi test skip. Chỉ dùng `VERIFIED` khi artifact thật trên đúng commit tồn tại.
