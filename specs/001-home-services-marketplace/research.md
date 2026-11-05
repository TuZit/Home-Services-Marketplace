# Research & Decisions — Home Services Marketplace (R1 Core)

## Architecture

- **Decision**: R1 modular monolith (single Spring Boot application organized by bounded context).
- **Rationale**: One transactional boundary and low operational overhead for a student team.
- **Alternatives**: Microservices — rejected for unnecessary distribution complexity.

## Data & Integrity

- **Decision**: PostgreSQL as source of truth; Flyway migrations; UUID PKs; UTC `timestamptz`; VND `NUMERIC(14,0)`.
- **Decision**: Slot reservation uses a PostgreSQL `EXCLUDE USING gist` constraint on active reservations to prevent overlapping bookings.
- **Decision**: Persistent `idempotency_record` with unique `(actor_id, operation, idem_key)` for consequential commands.
- **Decision**: Payment success constrained by a partial unique index (one RECORDED payment per booking).
- **Decision**: One review per booking via `booking_id UNIQUE`.

## Security

- **Decision**: JWT access token in memory; rotating refresh token in Secure HttpOnly SameSite cookie; CSRF/origin check on cookie-auth endpoints.
- **Decision**: BCrypt password hashing; server-side RBAC plus resource ownership checks.

## Eligibility

- **Decision**: Deterministic eligibility engine ordering: service → provider status (active + verified) → area → availability.
- **Decision**: R1 area model = governed province/district `area_code`; no geospatial radius in R1.

## Locale

- **Decision**: Canonical timezone `Asia/Ho_Chi_Minh`, persistence in UTC; currency VND.

## State Model

- Service Request: `DRAFT → CONFIRMED → PROVIDERS_FOUND → PROVIDER_SELECTED → CANCELLED` (no `SEARCHING_PROVIDERS`).
- Booking: `CONFIRMED → IN_PROGRESS → COMPLETED`; `CONFIRMED/IN_PROGRESS → CANCELLED` (no `PENDING`).
- Cancellation matrix: CONFIRMED → Customer/Provider/Admin; IN_PROGRESS → Admin only; COMPLETED → none.

## Frontend

- **Decision**: React + TypeScript (Vite), TanStack Query for server state, Zustand for client auth/session state, React Router, React Hook Form + Zod for validation, Tailwind CSS.
- **Decision**: Access token kept in memory only; refresh via credentialed cookie call.
