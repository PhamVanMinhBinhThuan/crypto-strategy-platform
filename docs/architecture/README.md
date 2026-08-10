# Architecture Documentation

Thư mục này mô tả kiến trúc cấp hệ thống: boundaries, thành phần, luồng dữ liệu, deployment và quality attributes.

## Danh sách file

| File | Mục đích |
| --- | --- |
| `architecture-overview.md` | Tóm tắt kiến trúc và các nguyên tắc chính |
| `system-context.md` | Người dùng, hệ thống và external systems |
| `container-view.md` | Web, API, Worker, Sentiment, Database và Redis |
| `module-view.md` | Trách nhiệm và dependency giữa các module |
| `data-flows.md` | Historical, realtime, backtest, search và news flows |
| `deployment-view.md` | Cách các thành phần được chạy/deploy |
| `quality-attributes.md` | Các scenario về thay đổi, scale và reliability |
| `data-model-overview.md` | Dữ liệu khái niệm cấp hệ thống |
| `diagrams/README.md` | Quy ước lưu source diagram |

## Quan hệ với Spec Kit

- Thư mục này chỉ mô tả kiến trúc cấp hệ thống.
- Thiết kế chi tiết từng feature nằm trong `specs/<feature>/plan.md`.
- API/event contract chi tiết nằm trong `specs/<feature>/contracts/` và `docs/api/`.
- Quyết định quan trọng phải liên kết tới ADR trong `docs/adr/`.

