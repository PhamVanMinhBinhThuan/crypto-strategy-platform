# Tasks: Web Foundation and Authentication

**Input**: Design documents from `/specs/011-web-foundation-auth/`

**Tests**: Tests are required by the plan and are written before their corresponding implementation.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

**Purpose**: Create the single project-approved Next.js application and verification toolchain.

- [X] T001 Initialize the Next.js App Router TypeScript project, remove obsolete `.gitkeep`, and verify no legacy `vite.config`, root `index.html`, Vite script or Vite dependency is introduced in `apps/web/package.json`, `apps/web/next.config.ts`, `apps/web/tsconfig.json`, `apps/web/app/layout.tsx`, and `apps/web/app/page.tsx`.
- [X] T002 Create and commit the npm lockfile and Node version declaration in `apps/web/package-lock.json` and `apps/web/.nvmrc`.
- [X] T003 [P] Configure lint, formatting, type-check, test and production-build scripts in `apps/web/package.json`, `apps/web/eslint.config.mjs`, and `apps/web/.prettierrc.json`.
- [X] T004 [P] Configure unit/component test setup with browser DOM matchers in `apps/web/vitest.config.ts` and `apps/web/tests/setup.ts`.
- [X] T005 [P] Configure Playwright desktop/mobile projects and local web server in `apps/web/playwright.config.ts`.
- [X] T006 [P] Add safe public environment examples and ignore rules in `apps/web/.env.example` and `apps/web/.gitignore`.
- [X] T007 Document setup commands and the prohibition on privileged/browser business credentials in `apps/web/README.md`.

---

## Phase 2: Foundational

**Purpose**: Establish shared types, configuration and visual primitives that block all stories.

**⚠️ CRITICAL**: Complete this phase before user-story implementation.

- [X] T008 Write failing environment-validation tests covering missing public Auth/API/WS values and forbidden service-role/database secrets in `apps/web/tests/foundation/environment.test.ts`.
- [X] T009 Implement typed public environment parsing with production fixture rejection in `apps/web/src/foundation/config/environment.ts`.
- [X] T010 [P] Define auth-session, safe-redirect and auth-operation contracts in `apps/web/src/foundation/auth/contracts.ts`.
- [X] T011 [P] Define normalized request, response and safe public-error contracts matching F-009 in `apps/web/src/foundation/http/contracts.ts`.
- [X] T012 [P] Define realtime connection, envelope and logical-subscription contracts matching F-009 in `apps/web/src/foundation/realtime/contracts.ts`.
- [X] T013 [P] Define loading, success, empty, error and degraded state contracts in `apps/web/src/foundation/ui/async-state.ts`.
- [X] T014 Write failing safe-redirect tests for internal, external, protocol-relative and login-loop targets in `apps/web/tests/foundation/safe-redirect.test.ts`.
- [X] T015 Implement safe redirect normalization in `apps/web/src/foundation/auth/safe-redirect.ts`.
- [X] T016 [P] Create dark-theme tokens, responsive breakpoints, focus styles and base typography in `apps/web/app/globals.css`.
- [X] T017 [P] Create reusable Button, Field, Card and Status components with accessible labels/focus in `apps/web/src/components/ui/Button.tsx`, `apps/web/src/components/ui/Field.tsx`, `apps/web/src/components/ui/Card.tsx`, and `apps/web/src/components/ui/StatusPanel.tsx`.
- [X] T018 Add a boundary rule test preventing direct Supabase business-table, Binance, internal Sentiment and legacy Vite imports in `apps/web/tests/architecture/foundation-boundaries.test.ts`.

**Checkpoint**: Shared types, safety rules and design primitives are available.

---

## Phase 3: User Story 1 - Secure Account Access (Priority: P1) 🎯 MVP

**Goal**: Register, confirm email, sign in and recover/reset a password through safe English UI.

**Independent Test**: A new browser user completes registration/confirmation/login/recovery flows without account enumeration or secret exposure.

### Tests for User Story 1

- [x] T019 [P] [US1] Write failing auth-adapter contract tests for sign-up, sign-in, recovery and reset outcomes in `apps/web/tests/auth/auth-adapter.contract.test.ts`.
- [X] T020 [P] [US1] Write failing form tests for validation, password confirmation, duplicate submit, safe errors and English-only user-facing text across every auth page in `apps/web/tests/auth/auth-forms.test.tsx`.
- [x] T021 [P] [US1] Write failing callback tests for valid, expired, reused and tampered confirmation/recovery links in `apps/web/tests/auth/auth-callback.test.ts`.
- [x] T022 [P] [US1] Write failing browser journey tests with a controllable auth/callback fixture for registration, mandatory confirmation, login and password reset, asserting registration guidance completes under 2 minutes and valid login reaches `/market` under 10 seconds in `apps/web/tests/e2e/authentication.spec.ts`.

### Implementation for User Story 1

- [X] T023 [US1] Implement browser/server Supabase client factories using public configuration only in `apps/web/src/foundation/auth/supabase-browser.ts` and `apps/web/src/foundation/auth/supabase-server.ts`.
- [X] T024 [US1] Implement the Supabase auth adapter with neutral enumeration-safe outcomes in `apps/web/src/foundation/auth/supabase-auth-adapter.ts`.
- [X] T025 [P] [US1] Implement English Login form/page in `apps/web/src/components/auth/LoginForm.tsx` and `apps/web/app/(auth)/login/page.tsx`.
- [X] T026 [P] [US1] Implement Register form/page with email, password and confirmation only in `apps/web/src/components/auth/RegisterForm.tsx` and `apps/web/app/(auth)/register/page.tsx`.
- [X] T027 [P] [US1] Implement Forgot Password form/page with neutral response in `apps/web/src/components/auth/ForgotPasswordForm.tsx` and `apps/web/app/(auth)/forgot-password/page.tsx`.
- [X] T028 [P] [US1] Implement Reset Password form/page that ends recovery session and redirects to Login in `apps/web/src/components/auth/ResetPasswordForm.tsx` and `apps/web/app/(auth)/reset-password/page.tsx`.
- [X] T029 [US1] Implement confirmation/recovery callback validation and safe status UI in `apps/web/app/auth/callback/route.ts` and `apps/web/app/(auth)/auth-status/page.tsx`.
- [X] T030 [US1] Add dark responsive Auth layout, branding and status presentation based on approved sketches in `apps/web/app/(auth)/layout.tsx` and `apps/web/src/components/auth/AuthShell.tsx`.
- [x] T031 [US1] Run and pass the US1 unit/component/browser tests defined in `apps/web/tests/auth/` and `apps/web/tests/e2e/authentication.spec.ts`.

**Checkpoint**: Account access works independently before business screens exist.

---

## Phase 4: User Story 2 - Consistent Session and Logout (Priority: P1)

**Goal**: Restore/refresh sessions, protect every business route and remove private state on logout.

**Independent Test**: Reload, expiry, refresh failure, cross-tab logout and Back navigation never expose protected content without a valid session.

### Tests for User Story 2

- [X] T032 [P] [US2] Write failing session bootstrap/refresh/expiry/cross-tab contract tests in `apps/web/tests/auth/session-lifecycle.test.ts`.
- [x] T033 [P] [US2] Write failing protected-route and safe-return-path tests in `apps/web/tests/auth/route-protection.test.ts`.
- [X] T034 [P] [US2] Write failing logout cleanup registry tests using abstract private-cache and realtime cleanup callbacks in `apps/web/tests/auth/logout-cleanup.test.ts`.
- [x] T035 [P] [US2] Write failing browser tests for reload, refresh, logout and Back navigation in `apps/web/tests/e2e/session.spec.ts`.

### Implementation for User Story 2

- [X] T036 [US2] Implement the single session provider/store and auth change observer in `apps/web/src/foundation/auth/SessionProvider.tsx`.
- [X] T037 [US2] Implement server-aware protected routing and internal return-path handling in `apps/web/src/foundation/auth/require-session.ts` and the Next.js 16 `apps/web/proxy.ts` request boundary.
- [x] T038 [US2] Implement coordinated refresh and authentication-failure handling in `apps/web/src/foundation/auth/session-lifecycle.ts`.
- [X] T039 [US2] Implement a transport-independent logout cleanup registry for registered private-cache and realtime cleanup callbacks in `apps/web/src/foundation/auth/logout.ts`.
- [X] T040 [US2] Implement account menu showing email and Logout in `apps/web/src/components/shell/AccountMenu.tsx`.
- [X] T041 [US2] Add authenticated-user redirects away from auth pages and default `/market` navigation in the Next.js 16 `apps/web/proxy.ts` request boundary and `apps/web/app/page.tsx`.
- [x] T042 [US2] Run and pass the US2 tests in `apps/web/tests/auth/` and `apps/web/tests/e2e/session.spec.ts`.

**Checkpoint**: Protected navigation and logout are independently secure.

---

## Phase 5: User Story 3 - Attractive and Usable Application Shell (Priority: P2)

**Goal**: Deliver the shared dark responsive shell and understandable UI states.

**Independent Test**: Navigation and state panels work by keyboard without overflow at 360px and 1440px.

### Tests for User Story 3

- [X] T043 [P] [US3] Write failing component tests for desktop/mobile navigation, active route and account menu in `apps/web/tests/shell/application-shell.test.tsx`.
- [X] T044 [P] [US3] Write failing tests for loading, empty, validation, unavailable and degraded panels in `apps/web/tests/ui/shared-states.test.tsx`.
- [x] T045 [P] [US3] Write failing viewport and keyboard-navigation browser tests in `apps/web/tests/e2e/application-shell.spec.ts`.

### Implementation for User Story 3

- [X] T046 [US3] Implement responsive Sidebar, Header and mobile navigation in `apps/web/src/components/shell/Sidebar.tsx`, `apps/web/src/components/shell/Header.tsx`, and `apps/web/src/components/shell/MobileNavigation.tsx`.
- [X] T047 [US3] Compose the protected application shell in `apps/web/app/(protected)/layout.tsx` and `apps/web/src/components/shell/ApplicationShell.tsx`.
- [X] T048 [P] [US3] Implement Loading, Empty, Error and Degraded reusable panels in `apps/web/src/components/states/LoadingState.tsx`, `apps/web/src/components/states/EmptyState.tsx`, `apps/web/src/components/states/ErrorState.tsx`, and `apps/web/src/components/states/DegradedState.tsx`.
- [X] T049 [US3] Add route metadata and active-navigation configuration in `apps/web/src/foundation/navigation/routes.ts`.
- [x] T050 [US3] Run and pass the US3 component/browser/accessibility checks in `apps/web/tests/shell/`, `apps/web/tests/ui/`, and `apps/web/tests/e2e/application-shell.spec.ts`.

**Checkpoint**: The visual shell is ready for both downstream UI features.

---

## Phase 6: User Story 4 - Shared Foundation for UI Teams (Priority: P2)

**Goal**: Publish stable real/mock HTTP and realtime boundaries plus protected route shells.

**Independent Test**: A sample consumer switches from fixtures to F-009-compatible adapters without changing its component contract.

### Tests for User Story 4

- [X] T051 [P] [US4] Write failing F-009 HTTP/error mapping contract tests from documented fixtures in `apps/web/tests/contracts/http-client.contract.test.ts`.
- [X] T052 [P] [US4] Write failing WebSocket ticket/envelope/reconnect/resubscribe tests in `apps/web/tests/contracts/realtime-client.contract.test.ts`.
- [X] T053 [P] [US4] Write failing real-versus-mock substitutability tests in `apps/web/tests/contracts/adapter-substitutability.test.tsx`.
- [X] T054 [P] [US4] Write failing production fixture-safety and visible development-marker tests in `apps/web/tests/foundation/fixture-safety.test.ts`.

### Implementation for User Story 4

- [X] T055 [US4] Implement bearer injection, correlation propagation and normalized F-009 errors in `apps/web/src/foundation/http/api-client.ts` and `apps/web/src/foundation/http/error-mapper.ts`.
- [X] T056 [US4] Implement one-time ticket acquisition and native WebSocket connection lifecycle in `apps/web/src/foundation/realtime/realtime-client.ts`.
- [X] T057 [US4] Implement bounded reconnect, resubscription and REST-recovery notification, then register realtime cleanup with the T039 logout registry in `apps/web/src/foundation/realtime/reconnect-policy.ts` and `apps/web/src/foundation/realtime/subscription-registry.ts`.
- [X] T058 [P] [US4] Implement explicitly enabled mock HTTP/realtime adapters and visible fixture badge in `apps/web/src/foundation/testing/mock-api-client.ts`, `apps/web/src/foundation/testing/mock-realtime-client.ts`, and `apps/web/src/components/states/FixtureModeBadge.tsx`.
- [X] T059 [US4] Implement dependency composition selecting real adapters by default and rejecting production mocks in `apps/web/src/foundation/composition/client-provider.tsx`.
- [X] T060 [P] [US4] Add protected placeholder pages for downstream ownership in `apps/web/app/(protected)/market/page.tsx`, `apps/web/app/(protected)/strategies/page.tsx`, and `apps/web/app/(protected)/news/page.tsx`.
- [X] T061 [P] [US4] Add protected placeholder pages for downstream ownership in `apps/web/app/(protected)/backtests/page.tsx` and `apps/web/app/(protected)/search/page.tsx`.
- [X] T062 [US4] Add a sample consumer proving real/mock adapter substitution in `apps/web/src/foundation/testing/FoundationConsumerProbe.tsx`.
- [X] T063 [US4] Document the handoff rules, imports and route ownership for F-012/F-013 in `apps/web/docs/frontend-foundation-handoff.md`.
- [X] T064 [US4] Run and pass all contract, substitutability, fixture-safety, logout/realtime integration and architecture tests in `apps/web/tests/contracts/`, `apps/web/tests/foundation/`, `apps/web/tests/auth/logout-cleanup.test.ts`, and `apps/web/tests/architecture/`; this permits development handoff only.

**Checkpoint**: F-012 and F-013 can branch from this foundation and work in parallel.

---

## Phase 7: Polish and Cross-Cutting Verification

- [x] T065 [P] Add automated accessibility assertions for auth pages and shell in `apps/web/tests/accessibility/foundation-accessibility.test.tsx`.
- [x] T066 [P] Add mock-adapter fallback rejection tests ensuring production builds do not include mock data branches in `apps/web/tests/architecture/mock-safety.test.ts`.
- [x] T067 [P] Add documentation parity tests against F-009 REST/WebSocket contracts in `apps/web/tests/contracts/f009-documentation-parity.test.ts`.
- [x] T068 Configure CI to run install, format check, lint, type-check, tests and production build in `.github/workflows/web-foundation.yml`.
- [x] T069 Run all `apps/web` quality scripts and production build defined in `apps/web/package.json`, recording only real results in `specs/011-web-foundation-auth/quickstart.md`.
- [x] T070 Run manual Supabase non-production acceptance with real confirmation and password-recovery emails, recording command/steps, commit, project ref, timing and sanitized evidence in `specs/011-web-foundation-auth/quickstart.md`; after T069 and T070 pass, mark the foundation as verified for F-012/F-013 handoff.
- [x] T071 Run `git diff --check` to ensure no whitespace or formatting errors. F-012/F-013 business screen, database migration, OAuth, light theme or privileged credential was introduced.

---

## Dependencies and Execution Order

- Phase 1 → Phase 2 → user-story phases.
- US1 and US2 are P1; implement US1 before US2 because session protection needs working auth.
- US3 can begin after Phase 2 and integrate the US2 Account Menu later.
- US4 can begin after Phase 2; final composition depends on US2 and US3.
- Polish requires all selected stories complete.

```text
Setup -> Foundation -> US1 Auth -> US2 Session --------┐
                    ├-----------> US3 Shell -----------┼-> US4 final composition -> Polish
                    └-----------> US4 client contracts ┘
```

## Parallel Opportunities

- T003–T006, T010–T013 and T016–T017 can run in parallel where files do not overlap.
- US1 test files T019–T022 can be written in parallel before implementation.
- US3 shell and shared-state work can proceed alongside US2 after foundation stabilizes.
- US4 HTTP, realtime and fixture contract tests can proceed in parallel.
- T060 and T061 deliberately split F-012/F-013 placeholder ownership into different files.

## Implementation Strategy

1. Complete Setup and Foundation.
2. Deliver US1 as the first demonstrable MVP slice.
3. Add US2 security/session behavior.
4. Complete US3 and US4, then publish the handoff contract.
5. F-012/F-013 may begin development after T064 passes; claim verified foundation only after
   T069 and T070 also pass. Both features may then work in parallel.
6. Never mark verification tasks complete without the corresponding successful command/evidence.
