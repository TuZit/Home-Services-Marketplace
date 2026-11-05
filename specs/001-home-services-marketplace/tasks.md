# Tasks: Home Services Marketplace (R1 Core)

**Input**: Design documents from `/specs/001-home-services-marketplace/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story this task belongs to (US1..US8)

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 Create `compose.yaml` at repo root with PostgreSQL 16 service (db name `hsm`, user `hsm`, password `hsm`, port `5432`).
- [X] T002 Verify `backend/build.gradle` dependencies and `backend/src/main/resources/application.yaml` point to PostgreSQL + Flyway.
- [X] T003 [P] Add frontend deps to `frontend/package.json`: `react-router-dom`, `react-hook-form`, `zod`, `@hookform/resolvers`.
- [X] T004 [P] Configure Tailwind CSS and axios client in `frontend/src/api/client.ts`.
- [X] T005 [P] Create frontend TypeScript types in `frontend/src/types/` from API contracts.

## Phase 2: Foundational (Blocking Prerequisites)

- [X] T006 Create Flyway migration `backend/src/main/resources/db/migration/V1__init.sql` with the full DDL from Data Model v0.1.
- [X] T007 [P] Implement shared error model `backend/src/main/java/ptit/home_services_marketplace/shared/error/` (ApiException, ErrorCode, GlobalExceptionHandler).
- [X] T008 [P] Implement shared time/currency helpers `backend/src/main/java/ptit/home_services_marketplace/shared/time/` (UTC + Asia/Ho_Chi_Minh).
- [X] T009 Implement security: JWT service, BCrypt password encoder, `SecurityConfig`, `JwtAuthFilter` in `shared/security/`.
- [X] T010 Implement idempotency service `shared/idempotency/IdempotencyService.java` + `IdempotencyRecord` entity.
- [X] T011 Implement audit service `shared/audit/AuditService.java` + `AuditEvent` entity.
- [X] T012 [P] Configure frontend auth store (zustand) and TanStack Query provider in `frontend/src/store/` and `frontend/src/main.tsx`.
- [X] T013 [P] Configure React Router routes skeleton in `frontend/src/App.tsx`.

**Checkpoint**: Foundation ready — DB, security, idempotency, audit, error handling, frontend shell.

## Phase 3: User Story 1 - Identity & Access (Priority: P1)

**Independent Test**: Register Customer/Provider, login, verify role/ownership enforcement.

- [X] T014 [P] [US1] Create `user_account` and `refresh_token` JPA entities in `backend/.../identity/`.
- [X] T015 [US1] Implement `AuthService` (register/login/refresh/logout) in `backend/.../identity/`.
- [X] T016 [US1] Implement `AuthController` + DTOs in `backend/.../identity/`.
- [X] T017 [P] [US1] Create frontend login/register pages + auth API in `frontend/src/pages/auth/` and `frontend/src/api/auth.ts`.
- [X] T018 [US1] Wire auth store to login/refresh flow with in-memory access token.

## Phase 4: User Story 2 - Catalogue & Provider Supply (Priority: P1)

**Independent Test**: Admin creates catalogue; Provider onboards + submits verification; Admin verifies.

- [X] T019 [P] [US2] Create catalogue entities (`service_category`, `service`, `geo_area`) in `backend/.../catalog/`.
- [X] T020 [P] [US2] Create provider entities (`provider_profile`, `provider_service`, `provider_area`, `provider_availability`) in `backend/.../provider/`.
- [X] T021 [P] [US2] Create customer entities (`customer_profile`, `customer_address`) in `backend/.../customer/`.
- [X] T022 [US2] Implement catalogue service + public/Admin catalogue controllers in `backend/.../catalog/`.
- [X] T023 [US2] Implement provider profile/capability service + controller in `backend/.../provider/`.
- [X] T024 [US2] Implement customer profile/address service + controller in `backend/.../customer/`.
- [X] T025 [US2] Implement admin provider verification decision in `backend/.../admin/`.
- [X] T026 [P] [US2] Frontend: catalogue browse + provider onboarding + customer address pages.

## Phase 5: User Story 3 - Manual Service Request (Priority: P1)

**Independent Test**: Create and confirm a request, verify one immutable version.

- [X] T027 [P] [US3] Create `service_request` and `request_version` entities in `backend/.../request/`.
- [X] T028 [US3] Implement `RequestService` (createDraft/updateDraft/confirm/cancel) in `backend/.../request/`.
- [X] T029 [US3] Implement request controller + DTOs in `backend/.../request/`.
- [X] T030 [US3] Wire request confirmation through idempotency service.
- [X] T031 [P] [US3] Frontend: create-request form + request detail pages.

## Phase 6: User Story 4 - Provider Discovery & Selection (Priority: P1)

**Independent Test**: Run discovery, verify all mandatory filters, select a provider.

- [X] T032 [P] [US4] Create discovery entities (`discovery_run`, `discovery_candidate`, `provider_selection`) in `backend/.../discovery/`.
- [X] T033 [US4] Implement `EligibilityEngine` (service → status → area → availability) in `backend/.../discovery/`.
- [X] T034 [US4] Implement discovery + selection service + controller in `backend/.../discovery/`.
- [X] T035 [US4] Revalidate eligibility at selection time.
- [X] T036 [P] [US4] Frontend: eligible-provider list + provider selection page.

## Phase 7: User Story 5 - Quotation & Booking (Priority: P1)

**Independent Test**: Submit and accept quotation; verify one booking + one reservation; reject overlapping.

- [X] T037 [P] [US5] Create quotation entities (`quotation`, `quotation_item`) in `backend/.../quotation/`.
- [X] T038 [P] [US5] Create booking + slot reservation entities in `backend/.../scheduling/` and `backend/.../booking/`.
- [X] T039 [US5] Implement `QuotationService` (create/submit/withdraw) in `backend/.../quotation/`.
- [X] T040 [US5] Implement `BookingService` accept flow: lock quote → reserve slot → create booking (transactional).
- [X] T041 [US5] Implement quotation + booking controllers + DTOs.
- [X] T042 [US5] Map exclusion-constraint violation to `SLOT_UNAVAILABLE` 409.
- [X] T043 [P] [US5] Frontend: quotation review/accept + booking confirmation pages.

## Phase 8: User Story 6 - Fulfillment (Priority: P2)

**Independent Test**: Start/complete/cancel booking with state validation.

- [X] T044 [US6] Implement booking state machine (start/complete/cancel) in `backend/.../booking/`.
- [X] T045 [US6] Release slot reservation on completion/cancellation.
- [X] T046 [P] [US6] Frontend: booking status/action pages.

## Phase 9: User Story 7 - Payment & Review (Priority: P2)

**Independent Test**: Record payment and submit review; reject duplicate review.

- [X] T047 [P] [US7] Create `payment_record` and `review` entities in `backend/.../payment/` and `backend/.../review/`.
- [X] T048 [US7] Implement payment attempt + idempotent result recording.
- [X] T049 [US7] Implement review submission + reputation calculation.
- [X] T050 [P] [US7] Frontend: payment + review pages.

## Phase 10: User Story 8 - Administration & Audit (Priority: P2)

**Independent Test**: Suspend provider, moderate review, inspect audit/dashboard.

- [X] T051 [US8] Implement admin user/account suspend+reactivate in `backend/.../admin/`.
- [X] T052 [US8] Implement review moderation (hide/restore) + audit query.
- [X] T053 [US8] Implement basic dashboard counts (requests/bookings/completion).
- [X] T054 [P] [US8] Frontend: admin pages (providers, catalogue, moderation, dashboard).

## Phase 11: Polish & Cross-Cutting Concerns

- [ ] T055 [P] Add backend integration tests for eligibility, booking accept, idempotency, review eligibility.
- [ ] T056 [P] Add backend unit tests for state machines.
- [X] T057 Run `quickstart.md` end-to-end smoke validation.
- [X] T058 [P] Frontend lint/build pass; backend test suite pass.

## Dependencies & Execution Order

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: blocks all user stories.
- **US1..US8**: after Foundational; US1-US5 are the MVP path (P1), US6-US8 are P2.
- **Polish (Phase 11)**: after all stories.

## MVP Scope

US1 + US2 + US3 + US4 + US5 (Identity → Catalogue/Provider → Request → Discovery/Selection → Quotation/Booking) is the minimum viable marketplace transaction path.
