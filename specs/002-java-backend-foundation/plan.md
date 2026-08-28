# Implementation Plan: Java Backend Foundation

**Branch**: `feature/002-java-backend-foundation` | **Feature ID**: `002-java-backend-foundation` | **Date**: 2026-08-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-java-backend-foundation/spec.md`

## Summary

Thiết lập Java backend foundation theo Modular Monolith đã chốt: một Gradle
multi-project build có hai Spring Boot composition root (`apps/api`, `apps/worker`), 13
capability skeleton và một architecture-test project. API cung cấp health, JWT resource
server, error/correlation boundary và read-only Supabase readiness; Worker chạy được ở
trạng thái idle, chưa kết nối queue. Build/test mặc định không cần database, Redis hoặc
provider; shared-development verification là task tách riêng dùng secret từ environment.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Spring Boot 3.5.16, Spring Security OAuth2 Resource Server/Jose,
Spring Boot Actuator, Spring JDBC/HikariCP, PostgreSQL JDBC, ArchUnit 1.5.0
**Build**: Gradle Wrapper 8.14.5, Kotlin DSL, version catalog, included `build-logic`
convention plugins
**Storage**: Supabase PostgreSQL 17 chỉ cho read-only health verification; không có
repository/business query trong feature
**Testing**: JUnit Platform/Spring Boot Test/Spring Security Test, ArchUnit; offline
`check` mặc định và task `supabaseIntegrationTest` tách riêng
**Target Platform**: JVM/Linux deployment và macOS/Linux development; shared development
Supabase tại `ap-southeast-1`
**Project Type**: Multi-project modular-monolith backend với hai runnable applications
**Performance Goals**: Clean build/check hoàn thành trong 10 phút; API và Worker trả
liveness trong 30 giây trên development target
**Constraints**: Giữ Spring Boot major 3 theo ADR; không Docker requirement; không Redis,
business endpoint/repository/schema change; không secret trong source/log; liveness không
phụ thuộc external system; readiness có database; applied migration bất biến
**Scale/Scope**: 2 applications, 13 capability subprojects, 1 architecture-test project,
test-only authentication controller và 4 thành viên phát triển song song

## Constitution Check

*GATE: Phải đạt trước Phase 0 và được kiểm tra lại sau Phase 1.*

| Nguyên tắc | Trạng thái | Bằng chứng |
|---|---|---|
| Specification-first / ADR governance | PASS cho planning; MERGE GATE còn mở | Spec có outcome/acceptance scenario. ADR-0011 đã Accepted; ADR-0001/0002/0006/0007 đang Proposed và phải Accepted trước khi implementation phụ thuộc được merge. |
| Module/data ownership | PASS | 13 capability project có owner; public/internal package và allowed dependency matrix theo ADR-0002. |
| Reproducibility | PASS | Feature không tạo Experiment; foundation chuẩn hóa Java 21, UTC/exact-decimal/identity convention và pinned wrapper. |
| Versioned contracts/provider isolation | PASS | Không thêm business endpoint; JWT/health/error contract rõ; provider và persistence không rò vào domain. |
| Security/reliability/testing | PASS | Resource Server validation, correlation/error redaction, liveness/readiness separation, offline test và ArchUnit evidence. |
| Database governance | PASS | Chỉ connection health read-only; không sửa migration hoặc business schema. |

**Kiểm tra lại sau Phase 1**: PASS cho planning. Contracts xác định rõ module boundary,
authentication, health và correlation; data model chỉ chứa foundation value objects,
không tạo business entity dùng chung. Merge gate về trạng thái ADR vẫn mở cho tới khi
ADR-0001/0002/0006/0007 được review và chuyển sang `Accepted`.

## Project Structure

### Documentation (this feature)

```text
specs/002-java-backend-foundation/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── tasks.md
├── verification-evidence.md        # created during implementation
├── contracts/
│   ├── authentication-boundary.md
│   ├── health-boundary.md
│   ├── module-boundaries.md
│   └── observability-boundary.md
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
gradlew
gradlew.bat
gradle/
├── libs.versions.toml
└── wrapper/

build-logic/
├── settings.gradle.kts
└── src/main/kotlin/
    ├── crypto.java-library-conventions.gradle.kts
    ├── crypto.spring-application-conventions.gradle.kts
    └── crypto.test-conventions.gradle.kts

apps/
├── api/
│   ├── build.gradle.kts
│   └── src/{main,test}/java/com/cryptostrategy/platform/api/
└── worker/
    ├── build.gradle.kts
    └── src/{main,test}/java/com/cryptostrategy/platform/worker/

modules/
├── domain/
├── contracts/
├── market-data/
├── strategy-core/
├── strategies/
├── combination/
├── backtesting/
├── evaluation/
├── experiment/
├── search/
├── leaderboard/
├── news/
└── persistence/
    ├── build.gradle.kts
    └── src/{main,test}/java/com/cryptostrategy/platform/<module>/

architecture-tests/
├── build.gradle.kts
└── src/test/java/com/cryptostrategy/platform/architecture/
```

**Structure Decision**: Dùng một Gradle multi-project build. `apps/*` là composition
root; `modules/*` là capability/library boundary; `architecture-tests` chỉ là verification
project, không phải capability runtime. `build-logic` tập trung convention để tránh 16
build script lệch nhau. `apps/web` và `apps/sentiment` giữ nguyên, ngoài phạm vi F-002.

## Phase 0: Research

Các quyết định về version, build layout, security, health, logging và test isolation được
ghi tại [research.md](./research.md). Không còn `NEEDS CLARIFICATION`.

## Phase 1: Design and Contracts

- Foundation value model: [data-model.md](./data-model.md)
- Module/dependency contract: [contracts/module-boundaries.md](./contracts/module-boundaries.md)
- Authentication contract: [contracts/authentication-boundary.md](./contracts/authentication-boundary.md)
- Health contract: [contracts/health-boundary.md](./contracts/health-boundary.md)
- Correlation/log contract: [contracts/observability-boundary.md](./contracts/observability-boundary.md)
- Validation guide: [quickstart.md](./quickstart.md)

## Complexity Tracking

Không có complexity violation cần biện minh. `architecture-tests` và `build-logic` là
build/verification infrastructure, không tạo thêm business capability hoặc runtime.
Trạng thái `Proposed` của ADR liên quan là merge gate quản trị, không phải lý do âm thầm
thay đổi hoặc supersede nội dung ADR trong feature này.
