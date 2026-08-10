# Container View

**Status**: Draft  
**Owners**: [Tên/vai trò]

## Purpose

[Mô tả các application/process/database chính]

## Container Diagram

```mermaid
flowchart TB
    WEB[React Web]
    API[Spring API]
    WORKER[Worker]
    SENTIMENT[Sentiment Service]
    DB[(PostgreSQL)]
    REDIS[(Redis)]

    WEB --> API
    API --> DB
    API --> REDIS
    REDIS --> WORKER
    WORKER --> DB
    WORKER --> SENTIMENT
```

## Container Catalog

| Container | Trách nhiệm | Công nghệ | Interface | Owner |
| --- | --- | --- | --- | --- |
| [Điền] | [Điền] | [Điền] | [Điền] | [Điền] |

## Communication

| From | To | Protocol | Dữ liệu | Sync/Async |
| --- | --- | --- | --- | --- |
| [Điền] | [Điền] | [Điền] | [Điền] | [Điền] |

## Data Ownership

| Container | Dữ liệu sở hữu/đọc | Ghi chú |
| --- | --- | --- |
| [Điền] | [Điền] | [Điền] |

## Failure Isolation

| Container lỗi | Thành phần bị ảnh hưởng | Expected behavior |
| --- | --- | --- |
| [Điền] | [Điền] | [Điền] |

## Open Questions

- [Câu hỏi chưa chốt]

