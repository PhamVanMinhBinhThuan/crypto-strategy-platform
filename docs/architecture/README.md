# Architecture Documentation

**Status**: Implemented baseline — F014 release review in progress

**Last Updated**: 2026-09-04

**Owner**: Văn Minh

Thư mục này là bản đồ kiến trúc cấp hệ thống của **Crypto Strategy Lab**. Nội dung mô tả kiến trúc mục tiêu cho MVP; những thuộc tính chưa có source code hoặc phép đo được ghi rõ là `Planned verification`, không được hiểu là kết quả đã kiểm chứng.

## Thứ tự đọc

1. [Architecture Overview](architecture-overview.md) — mục tiêu, drivers, style và quyết định chính.
2. [System Context](system-context.md) — C4 Level 1: actor, hệ thống và external systems.
3. [Container View](container-view.md) — C4 Level 2: các application/runtime và data store.
4. [Module View](module-view.md) — C4 Level 3: module backend, public boundary và dependency.
5. [Data Flows](data-flows.md) — các runtime story quan trọng.
6. [Data Model Overview](data-model-overview.md) — ownership, identity, version và lifecycle.
7. [Deployment View](deployment-view.md) — Local, CI và Demo topology.
8. [Quality Attribute Scenarios](quality-attributes.md) — ASR theo S–S–E–A–R–M.
9. [Architecture Evidence](architecture-evidence.md) — truy vết requirement, ADR, test/demo và evidence.

## Phân ranh tài liệu

- Thư mục này mô tả kiến trúc cấp hệ thống, không thay thế feature specification.
- Quyết định và trade-off nằm trong [ADR](../adr/README.md).
- REST/WebSocket contract chi tiết nằm trong [API documentation](../api/README.md).
- Dữ liệu runtime, benchmark và kết quả demo chỉ được ghi sau khi có implementation thật.
- Sơ đồ đơn giản dùng Mermaid trực tiếp; quy ước source diagram nằm tại [diagrams/README.md](diagrams/README.md).

## Nguồn yêu cầu

- [Đề bài Crypto Strategy Lab](../Crypto%20Strategy%20Lab%20%E2%80%93%20%C4%90%E1%BB%93%20%C3%A1n%20cu%E1%BB%91i%20k%E1%BB%B3.pdf), đặc biệt §31–§45.
- [Slide Kiến trúc đồ án](../KienTrucDoAn_slide.pdf), đặc biệt C4/Dynamic View, Quality Scenarios, ATAM-lite và Architecture Proof.
