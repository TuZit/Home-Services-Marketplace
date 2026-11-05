# Feature Specification: Home Services Marketplace (R1 Core)

**Feature Branch**: `001-home-services-marketplace`

**Created**: 2026-11-04

**Status**: Draft

**Input**: User description: "Home Services Marketplace R1 core marketplace: customers create service requests, discover eligible verified providers, select a provider, receive and accept quotations, book and complete service, record payment and submit review, fully manual without AI."

## User Scenarios & Testing

### User Story 1 - Identity & Access (Priority: P1)

A Guest can register as a Customer or Service Provider, log in, and log out. Protected actions are restricted by role and resource ownership.

**Why this priority**: Everything downstream depends on knowing who the actor is and what they are allowed to do.

**Independent Test**: Register a Customer and a Provider, log in with each, and verify a Customer cannot access a Provider-only resource.

**Acceptance Scenarios**:

1. **Given** a new unique email and valid password, **When** a Guest registers, **Then** one active account of the chosen role is created.
2. **Given** an email differing only by case from an existing email, **When** registration is submitted, **Then** it is rejected as already registered.
3. **Given** a Customer is authenticated, **When** they attempt a Provider-only action, **Then** the system denies it with an authorization error.

---

### User Story 2 - Catalogue & Provider Supply (Priority: P1)

Administrators govern service categories and services. Providers maintain a capability profile, supported services, service areas, availability, and submit for verification. Customers maintain profile and address book.

**Why this priority**: Discovery and requests depend on a governed catalogue and verified provider supply.

**Independent Test**: Create a category and service, onboard a provider with area/availability, verify the provider, and confirm only verified active providers are eligible.

**Acceptance Scenarios**:

1. **Given** an Admin, **When** they create/activate a category and service, **Then** Guests and Customers can browse them.
2. **Given** a Provider selects active services, districts, and availability, **When** they submit for verification, **Then** an Admin can approve or reject with a reason.
3. **Given** a Customer saves an address, **When** a confirmed request uses it, **Then** the request keeps the original address snapshot even if the address is later edited.

---

### User Story 3 - Manual Service Request (Priority: P1)

A Customer manually selects an active category and service, enters a description, service address, preferred schedule and optional budget, then reviews and confirms an immutable request version.

**Why this priority**: The request is the anchor for discovery, quotation, booking, payment and review.

**Independent Test**: Create and confirm a request without any AI call and verify one immutable version exists.

**Acceptance Scenarios**:

1. **Given** a valid draft request, **When** the Customer confirms it with an idempotency key, **Then** exactly one immutable request version is created and the request becomes confirmed.
2. **Given** the same idempotency key and payload, **When** confirmation is retried, **Then** the original result is returned without creating a second version.

---

### User Story 4 - Provider Discovery & Selection (Priority: P1)

The system returns only providers that support the requested service, are active and verified, serve the request area, and satisfy the schedule policy. The Customer selects one eligible provider.

**Why this priority**: Deterministic eligibility is the core trust and correctness mechanism of the marketplace.

**Independent Test**: Given a confirmed request, verify the returned providers all satisfy every mandatory rule and the Customer can select exactly one.

**Acceptance Scenarios**:

1. **Given** a confirmed request, **When** discovery runs, **Then** every returned provider supports the service, is active and verified, serves the area, and has sufficient availability.
2. **Given** no provider remains after filtering, **Then** the system returns a machine-readable no-result reason.

---

### User Story 5 - Quotation & Booking (Priority: P1)

The selected Provider creates and submits a quotation. The Customer accepts it, atomically creating exactly one confirmed booking and one active slot reservation.

**Why this priority**: Acceptance is the highest-risk operation because it must prevent double booking.

**Independent Test**: Accept a quotation and verify one booking plus one active reservation; concurrently accept an overlapping quote and verify it is rejected.

**Acceptance Scenarios**:

1. **Given** a valid submitted unexpired quotation and a free provider slot, **When** the Customer accepts with an idempotency key, **Then** exactly one confirmed booking and one active reservation are created.
2. **Given** another booking reserves an overlapping slot first, **When** acceptance commits, **Then** the whole transaction rolls back and the Customer receives a slot-unavailable conflict.

---

### User Story 6 - Fulfillment (Priority: P2)

The selected Provider starts and completes a confirmed booking. Eligible actors cancel according to state rules, releasing the slot.

**Why this priority**: Service execution is the operational heart of the transaction after booking.

**Independent Test**: Start a confirmed booking, complete it, and verify invalid transitions are rejected.

**Acceptance Scenarios**:

1. **Given** a confirmed booking, **When** the selected Provider starts it, **Then** it becomes in-progress and start time is recorded.
2. **Given** an in-progress booking, **When** a Customer attempts to cancel, **Then** the transition is rejected.

---

### User Story 7 - Payment & Review (Priority: P2)

After completion, the platform records a demonstration payment idempotently. The owning Customer submits one review for a completed and paid booking, and provider reputation is recalculated.

**Why this priority**: Payment and review close the transaction and build provider trust.

**Independent Test**: Record a payment and submit a review; verify a second review for the same booking is rejected.

**Acceptance Scenarios**:

1. **Given** a completed booking with a recorded payment and no review, **When** the Customer submits a rating, **Then** one published review is created.
2. **Given** a review already exists for the booking, **When** the Customer submits again, **Then** it is rejected.

---

### User Story 8 - Administration & Audit (Priority: P2)

Administrators manage accounts, verify/suspend providers, govern the catalogue, monitor transactions, moderate reviews, and inspect immutable audit events and basic dashboard trends.

**Why this priority**: Governance and auditability are required for a trustworthy platform and for graduation demonstration.

**Independent Test**: Suspend a provider and verify it disappears from new discovery results, and that a review moderation leaves an audit record.

**Acceptance Scenarios**:

1. **Given** a verified provider, **When** an Admin suspends it, **Then** it is excluded from new discovery results.
2. **Given** a sensitive Admin action, **When** performed, **Then** an immutable audit event records actor, action, entity and timestamp.

---

### Edge Cases

- Duplicate quotation acceptance must create at most one booking.
- Payment callback repeated must not create a second successful record.
- A provider suspended after discovery must be rejected at selection/acceptance time.
- AI is out of scope for R1; the full journey must work without any AI call.

## Requirements

### Functional Requirements

- **FR-ACC**: Register Customer/Provider, authenticate, enforce role and ownership, suspend/reactivate accounts, password recovery.
- **FR-CUS**: Customer profile and address book, address snapshot on confirmed requests, transaction history.
- **FR-PRO**: Provider profile, supported services, service areas, availability, indicative pricing, verification submission and decision.
- **FR-CAT**: Public catalogue browsing and category/service governance with historical integrity (no hard delete).
- **FR-SRQ**: Manual classification, structured request data, draft/resume, review and confirmation, immutable version, cancellation.
- **FR-DIS**: Deterministic eligibility (service, status, area, schedule), eligible-provider results, provider comparison, customer selection, no-result recovery.
- **FR-QUO**: Quotation preparation, submission, customer decision, validity control, booking trigger.
- **FR-BKG**: Booking detail, start, complete, cancellation, concurrency integrity, lifecycle history.
- **FR-PAY**: Payment initiation, idempotent result recording, failure retry, scope boundary (no real settlement).
- **FR-REV**: Review eligibility (completed + recorded payment), one review per booking, reputation calculation, moderation.
- **FR-ADM**: User management, provider verification, catalogue management, transaction monitoring, review moderation, audit, dashboard.

### Key Entities

- **User Account**: email, password hash, role (Customer/Provider/Admin), status.
- **Service Category / Service**: governed catalogue taxonomy.
- **Provider Profile / Service / Area / Availability**: provider capability and verification status.
- **Service Request / Request Version**: confirmed immutable requirement with address and schedule snapshot.
- **Provider Selection**: first-class link between a request and a chosen provider.
- **Quotation / Quotation Item**: provider offer with scope, price, schedule, validity.
- **Booking / Slot Reservation**: confirmed service commitment with non-overlapping schedule.
- **Payment Record**: idempotent demonstration payment status.
- **Review**: eligible rating/comment tied to a booking.
- **Audit Event**: immutable sensitive-action history.

## Success Criteria

- **SC-001**: A customer completes request → provider selection → quotation → booking → completed service → payment record → review without any AI call.
- **SC-002**: No inactive, unverified, suspended, unsupported-area, or unsupported-service provider appears in eligible results.
- **SC-003**: Exactly one booking is created from one accepted quotation, including retries and concurrent requests.
- **SC-004**: Invalid booking state transitions are rejected and valid transitions retain actor identity and timestamps.
- **SC-005**: Only an eligible completed and paid transaction can create one customer review.
- **SC-006**: Administrative verification, catalogue governance, moderation, and sensitive actions are auditable.
- **SC-007**: No password, token, payment secret, or unnecessary personal data appears in logs.

## Assumptions

- The platform is a graduation-project MVP, not a production legal or financial platform.
- Provider verification is a simplified administrative review; payment is demonstration-compatible without real settlement.
- R1 uses a governed province/district `area_code` model and `Asia/Ho_Chi_Minh` timezone; VND currency.
- R2 AI enhancement is out of scope for this feature and must never be a dependency of the R1 flow.
- Real-time chat, mobile apps, bidding, auto-dispatch, dynamic pricing, disputes/refunds, and fraud detection are out of scope.
