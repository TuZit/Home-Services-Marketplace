# Implementation Plan: Home Services Marketplace (R1 Core)

**Branch**: `001-home-services-marketplace` | **Date**: 2026-11-04 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-home-services-marketplace/spec.md`

## Summary

Implement the R1 Core Marketplace as a modular monolith backend (Spring Boot, Java 21, PostgreSQL) and a React SPA frontend. Customers create manual service requests, discover eligible verified providers, select one, receive and accept quotations, book and complete service, record a demonstration payment, and submit a review. R1 is fully manual with no AI dependency.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript + React 19 (frontend)

**Primary Dependencies**: Spring Boot 4.1.2, Spring Data JPA, Spring Security, Flyway, springdoc-openapi; React 19, Vite, TanStack Query, Zustand, React Router, React Hook Form + Zod, Tailwind CSS

**Storage**: PostgreSQL 16 (via Podman Compose); Flyway migrations

**Testing**: JUnit 5 + Testcontainers (backend); Vitest + React Testing Library (frontend, optional)

**Target Platform**: Linux server / local Podman VM; browser (desktop)

**Project Type**: Web application (backend + frontend)

**Performance Goals**: Deferred — performance targets are out of scope until an implementation load plan exists (per SRS NFR-06).

**Constraints**: Manual-first (no AI in R1); deterministic eligibility before any ranking; database-enforced integrity (unique/exclusion constraints); idempotent consequential commands; soft-delete historical data.

**Scale/Scope**: 8 user stories, ~24 entities, single PostgreSQL database, graduation-project MVP.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Manual-First: R1 flow has no AI dependency.
- [x] Eligibility Before Ranking: discovery is deterministic only.
- [x] Human Decision Authority: customer selects provider and confirms requests.
- [x] Traceability & Auditability: audit events + request/quotation snapshots.
- [x] No Hidden Automation: explicit state machine transitions.
- [x] Historical Integrity: soft-delete/deactivate referenced records.
- [x] Technical Constraints: Java 21/Spring/PostgreSQL + React/TS, UUID/UTC/VND.
- [x] Integrity: idempotency records, optimistic locking, exclusion constraint for slots.

## Project Structure

### Documentation (this feature)

```text
specs/001-home-services-marketplace/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── api-contracts.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── src/main/java/ptit/home_services_marketplace/
│   ├── shared/          # security, error, idempotency, audit, time
│   ├── identity/
│   ├── customer/
│   ├── provider/
│   ├── catalog/
│   ├── request/
│   ├── discovery/
│   ├── quotation/
│   ├── scheduling/
│   ├── booking/
│   ├── payment/
│   ├── review/
│   └── admin/
└── src/main/resources/
    ├── application.yaml
    └── db/migration/    # Flyway migrations

frontend/
├── src/
│   ├── api/             # axios client + typed endpoints
│   ├── components/
│   ├── pages/
│   ├── store/           # zustand
│   └── types/
└── src/main.tsx, App.tsx
```

**Structure Decision**: Web application (Option 2) — `backend/` modular monolith by bounded context, `frontend/` React SPA by page/feature.

## Complexity Tracking

No constitution violations requiring justification.
