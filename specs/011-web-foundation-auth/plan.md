# Implementation Plan: Web Foundation and Authentication

**Branch**: `feature/011-web-foundation-auth` | **Date**: 2026-09-03 | **Spec**: [spec.md](spec.md)

## Summary

Khởi tạo `apps/web` bằng Next.js App Router/TypeScript, xây dark responsive shell, Supabase
email/password authentication, protected routes và typed REST/WebSocket/mock boundaries cho F-012/F-013.

## Technical Context

**Stack**: Next.js App Router, TypeScript, Supabase browser/SSR client  
**Testing**: unit/component, browser E2E, accessibility và contract fixtures  
**Contracts**: F-009 REST `/api/v1`, WebSocket `/ws`, Supabase Auth  
**Constraints**: English-only dark theme; 360–1440px; không browser business-table access;
mock tắt mặc định ở production; không làm business UI F-012/F-013.

## Constitution Check

| Gate | Result | Evidence |
|---|---|---|
| Spec-first | Pass | `spec.md`, checklist 16/16 |
| Next.js duy nhất | Pass | Không tạo/nhập Vite SPA cũ |
| Security/authorization | Pass | Supabase giữ credential; Java API vẫn authorize business data |
| Contract isolation | Pass | Typed adapter bao F-009; không gọi table/service nội bộ |
| Evidence | Pass | Chỉ Verified sau khi test thật pass |
| ADR | Pass | Không phát sinh quyết định kiến trúc mới |

## Architecture and structure

```text
Route/UI -> published client ports -> real adapters -> F-009 REST/WebSocket
                                `----> mock adapters (development/test only)
Auth pages -> auth boundary -> Supabase Auth
```

```text
apps/web/
├── app/                       # public auth/protected route groups
├── src/components/            # shell, auth forms, shared states
├── src/foundation/auth/       # session contract/adapter
├── src/foundation/http/       # request/result/error adapter
├── src/foundation/realtime/   # ticket/connection/subscription lifecycle
├── src/foundation/testing/    # explicit fixtures
└── tests/                     # unit, component, contract, E2E
```

Một session boundary sở hữu bootstrap/refresh/logout; một HTTP client sở hữu bearer/error mapping;
một realtime client sở hữu ticket/reconnect/resubscribe. F-012/F-013 chỉ import public foundation.

## Phases

1. Bootstrap Next.js/TypeScript, scripts và environment validation.
2. Tạo design tokens, responsive shell và accessible shared states.
3. Làm signup/confirmation/login/forgot/reset/logout và protected navigation.
4. Làm typed REST, error và WebSocket lifecycle với real/mock adapters.
5. Tạo route shells và consumer tests cho F-012/F-013.
6. Chạy unit/component/E2E/accessibility/build/secret/mock-safety checks.

`/search` là shell chung cho Search và Leaderboard. Browser E2E tự động dùng controllable auth
adapter/callback fixture; luồng email thật của Supabase test project là acceptance thủ công có
bằng chứng. Development handoff được phép sau contract tests; verified handoff chỉ có sau toàn bộ
production build và remote-auth acceptance thành công.

## Post-design Constitution Check

Pass. Thiết kế dùng một Next.js app, không lưu credential, giữ authorization ở Java và cung cấp
boundary ổn định để hai feature UI tiếp theo phát triển song song.
