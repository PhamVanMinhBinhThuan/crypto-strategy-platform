# Implementation Plan: Database Baseline

**Branch**: `feature/database-setup` | **Feature ID**: `001-database-baseline` | **Date**: 2026-08-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-database-baseline/spec.md`

## Summary

Thiết lập database baseline có version cho Supabase hosted bằng một forward migration PostgreSQL. Baseline tạo 5 ownership schema và 24 bảng, liên kết ownership với `auth.users`, bảo vệ identity/deduplication/reproducibility bằng constraint và index, đồng thời chặn `anon`/`authenticated` truy cập trực tiếp business data. Migration chỉ được apply lên shared development sau một bước duyệt riêng; sau đó database được lint và kiểm thử bằng SQL assertions chạy trong transaction có rollback.

## Technical Context

**Language/Version**: SQL, PostgreSQL 17  
**Primary Dependencies**: Supabase hosted, Supabase CLI 2.115+, Supabase Auth schema<br>
**Storage**: Supabase PostgreSQL 17; Redis và transient cache/queue ngoài phạm vi  
**Testing**: migration dry-run, linked database lint, SQL integration/constraint tests qua `supabase db query --linked --file` trong transaction rollback<br>
**Target Platform**: Supabase shared development project tại `ap-southeast-1`  
**Project Type**: Database infrastructure trong modular monolith repository  
**Performance Goals**: Không đặt runtime throughput SLA trong feature này; các access path đã chốt phải có index và toàn bộ verification baseline hoàn thành trong 5 phút  
**Constraints**: Không Docker/local Supabase stack; không lưu secret trong repository; không cấp direct business-table access cho `anon`/`authenticated`; remote apply cần approval rõ ràng; migration đã apply không được sửa lại  
**Scale/Scope**: MVP/demo, 5 schema, 24 bảng, 8 timeframe canonical, ownership theo một user cho mỗi Experiment

## Constitution Check

*GATE: Phải đạt trước Phase 0 và được kiểm tra lại sau Phase 1.*

| Nguyên tắc | Trạng thái | Bằng chứng |
|---|---|---|
| ADR-first | PASS | ADR-0003, ADR-0007, ADR-0009 và ADR-0011 đã chốt các quyết định liên quan. |
| Module/data ownership | PASS | Năm schema ánh xạ trực tiếp Market, Strategy, Experiment, News và Platform; foreign key không cấp quyền ghi. |
| Reproducibility/immutability | PASS | Dataset membership, strategy snapshot, manifest, candidate, result và leaderboard revision được lưu bền vững. |
| Versioned contracts/provider isolation | PASS | Thay đổi qua ordered migration; provider được lưu như dữ liệu, không rò abstraction vào schema khác. |
| Security/reliability/testing | PASS có điều kiện | Quyền client bị revoke; Phase 1 phải định nghĩa verification cho constraint, permission và recovery records. |
| Remote mutation approval | PASS | Plan tách dry-run khỏi apply và yêu cầu approval trước shared-development mutation. |

**Kiểm tra lại sau Phase 1**: PASS. `data-model.md` phân biệt rõ invariant thuộc database và application; verification contract bao phủ schema, constraint, ownership, permission và durable recovery. Không có ngoại lệ constitution cần biện minh.

## Project Structure

### Documentation (this feature)

```text
specs/001-database-baseline/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── database-verification.md
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
supabase/
├── config.toml
├── migrations/
│   └── 20260827000100_create_database_baseline.sql
└── tests/
    └── database/
        └── 001_database_baseline_test.sql    # planned by this feature

docs/
├── adr/
└── database/
    └── decisions.md
```

**Structure Decision**: Feature này chỉ thay đổi database infrastructure và tài liệu. Không thêm persistence code vào `apps/*` trong phạm vi baseline. Verification SQL nằm cùng Supabase project để được version cùng migration.

## Phase 0: Research

Các quyết định và phương án bị loại được ghi tại [research.md](./research.md). Không còn `NEEDS CLARIFICATION`.

## Phase 1: Design and Contracts

- Mô hình logic và invariant: [data-model.md](./data-model.md)
- Verification contract: [contracts/database-verification.md](./contracts/database-verification.md)
- Quy trình chạy an toàn: [quickstart.md](./quickstart.md)
- Sau thiết kế, Constitution Check vẫn PASS và không phát sinh complexity violation.

## Complexity Tracking

Không có vi phạm constitution cần biện minh.
