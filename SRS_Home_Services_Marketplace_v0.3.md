# Software Requirements Specification (SRS)
## Home Services Marketplace

**Version:** v0.3
**Status:** Revised Draft
**Baseline:** HLR v0.2 + Architecture/Design v0.2
**Implementation strategy:** R1 Core Marketplace first, R2 AI Enhancement later

## Change History

| Version | Date | Summary |
|---|---|---|
| 0.1 | 2026-11-02 | Initial SRS draft. |
| 0.2 | 2026-11-03 | R1/R2 release split; manual-first workflow. |
| 0.3 | 2026-11-03 | Re-aligned requirement IDs to HLR groups (FR-ACC, FR-CUS, FR-PRO, FR-CAT, FR-SRQ, FR-DIS, FR-QUO, FR-BKG, FR-PAY, FR-REV, FR-ADM, FR-AI, FR-AIM). Removed Service Request state `SEARCHING_PROVIDERS` and Booking state `PENDING` per design baseline. Performance NFR deferred until implementation. Review eligibility clarified to COMPLETED + RECORDED payment. |

## 1. Purpose

This SRS defines detailed requirements for Home Services Marketplace. Requirements are organized into two releases:

| Release | Goal |
|---|---|
| R1 Core Marketplace | Complete the business workflow without AI dependency. |
| R2 AI Enhancement | Introduce AI-assisted requirement understanding and provider recommendation. |

**Release constraint:** R2 must not be required for successful execution of R1.

## 2. Functional Requirements

Priority legend: **M** = Must have · **S** = Should have · **O** = Optional.

### 2.1 FR-ACC — Account and Access

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-ACC-01 | Account registration | Guests can register as Customer or begin Provider onboarding with a unique email. | M | R1 |
| FR-ACC-02 | Authentication | Registered users can log in and log out securely. | M | R1 |
| FR-ACC-03 | Role and ownership control | Protected actions are restricted by role and resource ownership. | M | R1 |
| FR-ACC-04 | Account status | Administrator can suspend/reactivate accounts with reason and audit. | M | R1 |
| FR-ACC-05 | Password recovery | A user can reset a password using a time-limited, single-use process. | S | R1 |

### 2.2 FR-CUS — Customer Profile and Address

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-CUS-01 | Customer profile | Customer can maintain permitted personal and contact information. | M | R1 |
| FR-CUS-02 | Address book | Customer can create, edit, soft-delete and select a default address. | M | R1 |
| FR-CUS-03 | Address snapshot | Confirmed requests and bookings retain the service-address snapshot used for fulfillment. | M | R1 |
| FR-CUS-04 | Transaction history | Customer can view owned requests, quotations, bookings, payments and reviews. | M | R1 |

### 2.3 FR-PRO — Provider Management

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-PRO-01 | Provider profile | Provider maintains identity, description, contact and experience summary. | M | R1 |
| FR-PRO-02 | Supported services | Provider selects active services and records capability/experience. | M | R1 |
| FR-PRO-03 | Service areas | Provider defines coverage using a model that supports deterministic containment checking. | M | R1 |
| FR-PRO-04 | Availability | Provider maintains available/unavailable intervals using a consistent timezone. | M | R1 |
| FR-PRO-05 | Indicative pricing | Provider may maintain starting price or range; official price remains the quotation. | S | R1 |
| FR-PRO-06 | Verification submission | Provider submits a complete profile for administrative review. | M | R1 |
| FR-PRO-07 | Verification decision | Administrator approves/rejects/suspends with reason; provider sees the customer-safe result. | M | R1 |

### 2.4 FR-CAT — Service Catalogue

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-CAT-01 | Public catalogue | Users can browse active categories and services. | M | R1 |
| FR-CAT-02 | Category governance | Administrator creates, updates, activates and deactivates categories. | M | R1 |
| FR-CAT-03 | Service governance | Administrator creates, updates, activates and deactivates services. | M | R1 |
| FR-CAT-04 | Historical integrity | Referenced catalogue records are not hard-deleted. | M | R1 |

### 2.5 FR-SRQ — Manual Service Request

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-SRQ-01 | Manual classification | Customer selects active category and service without AI. | M | R1 |
| FR-SRQ-02 | Structured request data | Customer enters description, address, preferred time and optional budget. | M | R1 |
| FR-SRQ-03 | Draft and resume | Customer can save and resume a draft request. | M | R1 |
| FR-SRQ-04 | Review and confirmation | Customer reviews all structured values before confirmation. | M | R1 |
| FR-SRQ-05 | Request version | Confirmation produces one immutable request version used by downstream steps. | M | R1 |
| FR-SRQ-06 | Material change | Material changes after confirmation invalidate stale provider results. | S | R1 |
| FR-SRQ-07 | Cancellation | Customer or authorised administrator can cancel under allowed states with audit. | M | R1 |

**Critical constraint:** FR-SRQ-01 through FR-SRQ-07 must not depend on AI.

### 2.6 FR-DIS — Provider Discovery and Selection

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-DIS-01 | Service eligibility | Provider must support the requested service. | M | R1 |
| FR-DIS-02 | Status eligibility | Inactive, unverified and suspended providers are excluded. | M | R1 |
| FR-DIS-03 | Area eligibility | Provider must serve the request location. | M | R1 |
| FR-DIS-04 | Schedule eligibility | Provider must satisfy the agreed availability policy. | M | R1 |
| FR-DIS-05 | Eligible-provider results | Customer sees eligible providers without mandatory AI score or ranking. | M | R1 |
| FR-DIS-06 | Provider comparison | Customer can inspect public profile, services, rating, indicative price and availability summary. | M | R1 |
| FR-DIS-07 | Customer selection | Customer explicitly selects one currently eligible provider. | M | R1 |
| FR-DIS-08 | No-result recovery | Customer receives a clear no-result state and can adjust/retry. | M | R1 |

R1 discovery/filtering is deterministic and shall not require AI ranking.

### 2.7 FR-QUO — Quotation

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-QUO-01 | Quotation preparation | Selected provider creates service description, cost breakdown, total, schedule and notes. | M | R1 |
| FR-QUO-02 | Submission | Submitted quotation is versioned/snapshotted and visible to Customer. | M | R1 |
| FR-QUO-03 | Customer decision | Customer accepts or rejects the active quotation. | M | R1 |
| FR-QUO-04 | Validity control | Expired or withdrawn quotations cannot be accepted. | S | R1 |
| FR-QUO-05 | Booking trigger | Accepted quotation creates at most one linked booking. | M | R1 |

### 2.8 FR-BKG — Booking and Fulfillment

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-BKG-01 | Booking detail | Customer and selected provider can view permitted booking details. | M | R1 |
| FR-BKG-02 | Start job | Selected provider starts a confirmed booking. | M | R1 |
| FR-BKG-03 | Complete job | Selected provider completes an in-progress booking. | M | R1 |
| FR-BKG-04 | Cancellation | Eligible actor can cancel according to state-based rules. | M | R1 |
| FR-BKG-05 | Concurrency integrity | Conflicting state changes do not create inconsistent outcomes. | M | R1 |
| FR-BKG-06 | History | Customer, provider and administrator can view authorised lifecycle history. | M | R1 |

### 2.9 FR-PAY — Payment Record

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-PAY-01 | Payment initiation | Platform creates a payment record for an eligible booking. | M | R1 |
| FR-PAY-02 | Payment result | Adapter/simulator outcome is stored idempotently with correlation reference. | M | R1 |
| FR-PAY-03 | Failure handling | Failed payment may be retried without duplicating successful records. | M | R1 |
| FR-PAY-04 | Scope boundary | Payment recording does not imply production settlement, wallet or commission support. | M | R1 |

### 2.10 FR-REV — Review and Reputation

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-REV-01 | Review eligibility | Only the owning customer of an eligible completed booking (COMPLETED + RECORDED payment) can review. | M | R1 |
| FR-REV-02 | One review | At most one active review exists per eligible booking. | M | R1 |
| FR-REV-03 | Reputation | Provider rating is calculated from eligible published reviews. | M | R1 |
| FR-REV-04 | Moderation | Administrator can hide/restore review with a reason and audit. | S | R1 |

### 2.11 FR-ADM — Administration and Audit

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-ADM-01 | User management | Administrator searches, views, suspends and reactivates accounts. | M | R1 |
| FR-ADM-02 | Provider verification | Administrator reviews submitted provider snapshots and records decisions. | M | R1 |
| FR-ADM-03 | Catalogue management | Administrator governs categories and services. | M | R1 |
| FR-ADM-04 | Transaction monitoring | Administrator searches and inspects requests/bookings according to permission. | S | R1 |
| FR-ADM-05 | Review moderation | Administrator moderates inappropriate reviews. | S | R1 |
| FR-ADM-06 | Audit | Sensitive actions create immutable audit events with actor and timestamp. | M | R1 |
| FR-ADM-07 | Dashboard | Administrator views basic counts and trends for requests, bookings and completion. | S | R1 |

### 2.12 FR-AI — AI-Assisted Requirement Understanding (R2)

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-AI-01 | Natural-language input | Customer may enter an unstructured service problem description. | M | R2 |
| FR-AI-02 | Structured suggestions | AI suggests category, service, problem details and approved structured fields. | M | R2 |
| FR-AI-03 | Confidence handling | Uncertain output is clearly identified and requires manual review. | M | R2 |
| FR-AI-04 | Human control | Customer can apply, edit or ignore each suggestion before confirmation. | M | R2 |
| FR-AI-05 | Manual fallback | AI error, timeout or unavailability preserves input and returns to R1 manual flow. | M | R2 |
| FR-AI-06 | Traceability | AI run records relevant model/prompt/config identifiers where feasible. | S | R2 |

### 2.13 FR-AIM — AI-Assisted Provider Recommendation (R2)

| ID | Requirement | Business Description | Priority | Release |
|---|---|---|:---:|:---:|
| FR-AIM-01 | Eligible input only | AI receives only candidates that passed all mandatory R1 filters. | M | R2 |
| FR-AIM-02 | Versioned ranking | Eligible providers are ranked using configurable/versioned signals. | M | R2 |
| FR-AIM-03 | Top-N | Platform presents up to the configured N providers using documented tie-break rules. | M | R2 |
| FR-AIM-04 | Explanation | Explanation uses stored request/provider factors and avoids unsupported claims. | M | R2 |
| FR-AIM-05 | Customer decision | AI cannot auto-select provider or create booking. | M | R2 |
| FR-AIM-06 | Discovery fallback | R1 eligible-provider results remain usable if AI is unavailable. | M | R2 |
| FR-AIM-07 | Evaluation | Recommendation quality is measured on a separate dataset and compared with baseline. | M | R2 |

## 3. State Model

### Service Request

```
DRAFT → CONFIRMED → PROVIDERS_FOUND → PROVIDER_SELECTED → CANCELLED
```

- `CONFIRMED` means an immutable request version exists.
- `PROVIDERS_FOUND` means a discovery run completed with at least one eligible provider.
- `CONFIRMED` remains unchanged when discovery returns no eligible provider.
- `SEARCHING_PROVIDERS` is removed: R1 discovery is a synchronous query, not a persistent workflow stage.

### Booking

```
CONFIRMED → IN_PROGRESS → COMPLETED
CONFIRMED → CANCELLED
IN_PROGRESS → CANCELLED (according to policy)
```

- Booking is created directly in `CONFIRMED` after successful quotation acceptance and slot reservation.
- `PENDING` is removed.

### Other States

- Provider: `DRAFT → PENDING_VERIFICATION → VERIFIED | REJECTED`; `VERIFIED → SUSPENDED`.
- Quotation: `DRAFT → SUBMITTED → ACCEPTED | REJECTED | WITHDRAWN | EXPIRED`.
- Payment Record: `PENDING → RECORDED | FAILED`.
- Review: `PUBLISHED ↔ HIDDEN`.

## 4. Business Rules

| ID | Rule |
|---|---|
| BR-SYS-01 | A customer must be able to complete the full R1 journey without any AI call. |
| BR-SYS-02 | Only active and verified providers may enter discovery results. |
| BR-SYS-03 | Provider must support the requested service, area and applicable schedule. |
| BR-SYS-04 | Mandatory deterministic eligibility filtering executes before any R2 ranking. |
| BR-SYS-05 | Customer makes the final provider selection. |
| BR-SYS-06 | AI output is advisory until customer confirmation. |
| BR-SYS-07 | AI may not create booking, accept quotation, record payment or publish review. |
| BR-SYS-08 | AI failure must not block the manual marketplace flow. |
| BR-SYS-09 | Booking requires acceptance of one active quotation. |
| BR-SYS-10 | Only valid lifecycle transitions are allowed. |
| BR-SYS-11 | Historical transaction entities are soft-deleted/deactivated, not hard-deleted. |
| BR-SYS-12 | Only eligible completed bookings can produce customer reviews. |
| BR-SYS-13 | Sensitive administrative and transactional actions are auditable. |
| BR-SYS-14 | Confirmed request and accepted quotation values are retained as snapshots for traceability. |
| BR-SYS-15 | A stale eligibility or recommendation result must be revalidated before provider selection. |
| BR-SYS-16 | Notification delivery failure does not roll back a successfully committed business transaction. |
| BR-SYS-17 | Payment callback/result processing is idempotent. |
| BR-SYS-18 | R2 regression testing must include all critical R1 journeys. |

## 5. Core Use Cases

### UC-01 Create Service Request Manually [R1]

**Actor:** Customer

**Preconditions:** Customer is authenticated; Service Catalog is available.

**Main Flow:**
1. Customer opens Create Service Request.
2. Customer selects Category.
3. Customer selects Service.
4. Customer provides problem description.
5. Customer selects service location.
6. Customer provides preferred time.
7. Customer optionally enters budget.
8. System validates input.
9. Customer reviews information.
10. Customer submits with an idempotency key.
11. System creates an immutable request version and the request becomes CONFIRMED.

**Postcondition:** Valid CONFIRMED Service Request exists without any AI call.

### UC-02 Find and Select Provider [R1]

1. System receives a CONFIRMED Service Request.
2. System retrieves providers supporting the requested service.
3. System removes inactive, unverified and suspended providers.
4. System checks service area.
5. System applies availability filtering where applicable.
6. System returns eligible providers (or a machine-readable no-result reason).
7. Customer reviews provider profiles.
8. Customer selects a provider.
9. System revalidates eligibility at selection time and creates an ACTIVE provider selection.

**Constraint:** No AI dependency. Discovery is a synchronous query with no persistent SEARCHING state.

### UC-AI-01 Create Service Request Using AI [R2]

**Precondition:** UC-01 is already operational.

1. Customer enters natural-language description.
2. AI analyzes input.
3. System proposes structured Service Request data.
4. Customer reviews generated information.
5. Customer corrects information if necessary.
6. Customer confirms.
7. Existing Service Request flow creates the request.

**Alternative:** If AI is unavailable/fails, system falls back to UC-01.

## 6. Non-Functional Requirements

| ID | Area | Requirement | Release |
|---|---|---|:---:|
| NFR-01 | Security | Protected APIs enforce server-side RBAC and resource-ownership checks. | R1 |
| NFR-02 | Credential Security | Passwords use an adaptive industry-standard hash; secrets are externalized and not logged. | R1 |
| NFR-03 | Privacy | User, address and transaction data are exposed only to authorised actors and minimised for AI processing. | R1/R2 |
| NFR-04 | Reliability | Quotation acceptance, booking creation and payment result handling are atomic/idempotent. | R1 |
| NFR-05 | Resilience | External adapter or AI failure must not lose user drafts or corrupt transaction state. | R1/R2 |
| NFR-06 | Performance | **Deferred.** Exact p95 targets will be defined with the load plan once implementation exists. | TBC |
| NFR-07 | Availability | The manual Core Marketplace remains available independently of the R2 AI service. | R1/R2 |
| NFR-08 | Usability | Key workflows are responsive and preserve entered data after recoverable validation/errors. | R1 |
| NFR-09 | Accessibility | Key web journeys target WCAG 2.1 AA-oriented practices for labels, keyboard access, focus and contrast. | R1 |
| NFR-10 | Traceability | State-changing operations retain actor, timestamp, entity and correlation context. | R1 |
| NFR-11 | Maintainability | Critical rules and transitions have automated unit/integration tests; core journey has E2E coverage. | R1 |
| NFR-12 | Deployability | The application supports reproducible containerized deployment and environment-separated configuration. | R1 |
| NFR-13 | Observability | Logs and metrics support investigation without exposing secrets or unnecessary personal data. | R1 |
| NFR-14 | AI Quality | AI is evaluated on a dataset separated from demonstration examples. | R2 |
| NFR-15 | AI Safety | AI output requires human confirmation where it can affect a business request or decision. | R2 |
| NFR-16 | AI Explanation | Explanations are grounded in stored factors and do not infer protected or unavailable attributes. | R2 |
| NFR-17 | AI Traceability | Recommendation/analysis runs retain configuration identifiers and input/output references where feasible. | R2 |

## 7. Release Traceability

| Capability | R1 Core | R2 AI |
|---|---|---|
| Account and Access | Yes | - |
| Customer Profile and Address | Yes | - |
| Provider Management | Yes | - |
| Service Catalogue | Yes | - |
| Service Request | Manual | AI-assisted |
| Provider Filtering | Deterministic rules | - |
| Provider Discovery | Manual | - |
| Provider Ranking | - | AI-assisted |
| Recommendation Explanation | - | Yes |
| Quotation | Yes | - |
| Booking | Yes | - |
| Fulfillment | Yes | - |
| Payment | Yes | - |
| Review | Yes | - |
| Administration | Yes | - |
| AI Requirement Understanding | - | Yes |

## 8. AI Entry Criteria

R2 AI implementation shall start only after the following R1 functions have been implemented and validated:

- Manual Service Request end-to-end.
- Deterministic provider filtering.
- Provider discovery and selection.
- Quotation.
- Booking lifecycle.
- Service fulfillment.
- Payment/payment recording.
- Rating/review.
- Critical R1 automated/acceptance tests pass.

## 9. Key Architecture/Requirement Principle

**AI depends on Core Marketplace. Core Marketplace must not depend on AI.**

Target dependency direction:

`Core Marketplace <- AI Enhancement`

The AI layer consumes `ServiceRequest + EligibleProviders` and may perform `Understand / Rank / Recommend / Explain`, while deterministic eligibility filtering and the complete transaction lifecycle remain operational independently.

---

**End of Document — Version 0.3 — Home Services Marketplace — 2026-11-03**
