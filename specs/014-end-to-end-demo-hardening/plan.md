# Implementation Plan: F-014 — End-to-End Demo and Hardening

**Branch**: `feature/014-end-to-end-demo-hardening` | **Date**: 2026-09-04 | **Spec**: [spec.md](./spec.md)

## Summary

Hoàn thiện và kiểm chứng hành trình Market → Strategy → Search → Backtest → Evaluation → Leaderboard → Result/Trades → News/Sentiment trên các boundary đã phát hành. Công việc ưu tiên tích hợp và hardening, bổ sung ba Strategy bắt buộc còn thiếu (RSI, Bollinger Bands, Support/Resistance) qua plugin contract hiện hữu, rồi cung cấp runbook, failure evidence, provenance/reproduction, security scan và benchmark có thể lặp lại. Fixture chỉ là fallback có nhãn rõ; số liệu Verified phải đến từ lần chạy thật.

## Technical Context

**Language/Version**: Java 21; TypeScript 5.9/Node.js 22; Python 3.11–3.12
**Primary Dependencies**: Spring Boot 3/Gradle; Next.js 16.3/React 19; FastAPI; Redis; PostgreSQL/Supabase adapters
**Storage**: PostgreSQL/Supabase là source of truth; Redis Streams/Redis cho queue, progress và cache có thể phục hồi
**Testing**: JUnit 5/Gradle; Vitest và Playwright; pytest
**Target Platform**: Local/demo web stack gồm API, Worker, Sentiment service và browser UI
**Project Type**: Multi-runtime web application trong modular monolith Java, Next.js frontend và Python model service
**Performance Goals**: Giữ gate Start Search p95 < 2 giây; tăng Worker từ 1 lên 3 đạt ít nhất 2x với workload cố định; chạy smoke tối thiểu 3 lần và công bố median cùng từng lần chạy
**Constraints**: Authentication/authorization; không lộ secret; bốn chart độc lập; bốn Strategy bắt buộc; evidence gắn commit/môi trường; fixture không giả làm live result
**Scale/Scope**: 4 runtime applications, 13 capability modules, 5 màn hình chính, 23 tiêu chí cốt lõi + 1 dòng mở rộng tùy chọn trong rubric và ít nhất 2 failure scenario

## Constitution Check

### Trước thiết kế

- **I — Spec/ADR**: PASS. Spec có outcome và acceptance criteria. F014 tuân ADR hiện có; không có quyết định kiến trúc dài hạn mới nên không cần ADR mới.
- **II — Ownership**: PASS. Strategy nằm trong `modules/strategies`; các capability tiếp tục cộng tác qua port/API/message đã có.
- **III — Reproducibility**: PASS. Result truy vết dataset checksum, strategy version/parameters, assumptions, candidate/attempt và software version; reproduction tạo run mới.
- **IV — Versioned contracts**: PASS. Ba Strategy mới implement contract hiện hành; UI chỉ dùng public F-011/F-013 boundary.
- **V — Safety/evidence**: PASS. Plan có failure isolation, duplicate safety, bounded retry, correlation, secret scan và benchmark.
- **UI reference**: PASS. Đã đọc bộ reference bắt buộc và mapping F-012/F-013; không sao chép prototype simulation.

### Sau implementation/hardening

PASS có điều kiện. Data model của F014 chỉ mô tả hồ sơ demo/evidence và không giành ownership
business data. Integration review phát hiện public contract còn thiếu đường đọc durable reproduction
verification và canonical provenance trên Result; remediation là thay đổi cộng thêm trong OpenAPI,
API owner-scoped controller/read port và Web adapter, có contract/integration/architecture tests.
Không có database migration mới trong F014; schema F006 thiếu trên shared environment được giữ là
external blocker thay vì sửa bảng thủ công. ADR mới không cần thiết vì lifecycle/ownership đã được
quyết định trong ADR-0009/0011/0016; public contract thay đổi phải được API/Search owner review ở
release gate T062.

## Project Structure

### Documentation (this feature)

```text
specs/014-end-to-end-demo-hardening/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── demo-profile-contract.md
│   ├── evidence-record-contract.md
│   ├── integrated-demo-contract.md
│   └── rubric-traceability.md
└── checklists/requirements.md
```

### Source Code (repository root)

```text
apps/
├── api/src/                 # public HTTP/WebSocket boundary và authorization
├── worker/src/              # durable orchestration, retry và recovery
├── sentiment/app/           # isolated FastAPI sentiment service
└── web/{app,src,tests}/     # production UI, client boundary và browser evidence
modules/
├── strategy-core/src/       # stable Strategy/StrategyPlugin contracts
├── strategies/src/          # MA cộng ba plugin bắt buộc còn thiếu
├── search/src/              # generator, stop condition, coordinator
├── backtesting/src/         # trades và execution assumptions
├── evaluation/src/          # bốn metrics bắt buộc
├── leaderboard/src/         # deterministic Top-K revisions
├── market-data/src/         # candles, reconnect/backfill, freshness
├── news/src/                # News/Sentiment ownership
├── experiment*/src/         # experiment/candidate/job/attempt lifecycle
└── persistence/src/         # PostgreSQL/Supabase và Redis adapters
docs/{architecture,adr,evidence}/
```

**Structure Decision**: Giữ multi-runtime modular monolith. F014 sửa theo capability owner và nối qua contract có sẵn; không tạo application hay shared module mới.

## Implementation Phases

1. **Baseline audit**: chạy gate hiện hữu, lập bảng 24 dòng rubric (23 cốt lõi + 1 mở rộng tùy chọn) và đánh dấu trạng thái bằng evidence thật.
2. **Strategy remediation**: thêm RSI, Bollinger Bands, Support/Resistance theo StrategyPlugin contract cùng deterministic/registry tests.
3. **Integration hardening**: nối và sửa gap của toàn hành trình; không tính toán nghiệp vụ trên browser.
4. **Failure/reproduction**: kiểm chứng sentiment isolation, worker/queue recovery, duplicate safety và reproduction không overwrite.
5. **Demo/readiness**: runbook, demo profile, accessibility/responsive, security scan, benchmark ba lần, README và architecture links.
6. **Release evidence**: đóng gate, ghi limitations/fallback/rollback; chỉ chuyển Verified khi có artifact thật.

## Complexity Tracking

Không có vi phạm Constitution cần biện minh.
