# Software Requirements Specification (SRS)
## Home Services Marketplace

**Version:** v0.2  
**Status:** Revised Draft  
**Implementation strategy:** R1 Core Marketplace first, R2 AI Enhancement later

## 1. Purpose
This SRS defines detailed requirements for Home Services Marketplace.

Requirements are organized into two releases:

| Release | Goal |
|---|---|
| R1 Core Marketplace | Complete the business workflow without AI dependency |
| R2 AI Enhancement | Introduce AI-assisted requirement understanding and provider recommendation |

**Release constraint:** R2 must not be required for successful execution of R1.

## 2. Functional Requirements
### 2.1 Identity & Access
- **FR-AUTH-001 Register Customer [R1]:** System shall allow a user to register a Customer account.
- **FR-AUTH-002 Register Provider [R1]:** System shall allow a user to register a Service Provider account.
- **FR-AUTH-003 Login [R1]:** System shall authenticate registered users.
- **FR-AUTH-004 Role Authorization [R1]:** System shall restrict protected operations based on user role.

### 2.2 Service Catalog
- **FR-SVC-001 Browse Categories [R1]:** Customer shall browse available service categories.
- **FR-SVC-002 Browse Services [R1]:** Customer shall view services belonging to a category.
- **FR-SVC-003 Service Detail [R1]:** Customer shall view service information.
- **FR-SVC-004 Manage Taxonomy [R1]:** Administrator shall create/update/deactivate categories and services.

### 2.3 Provider Management
- **FR-PRV-001 Provider Profile [R1]:** Provider shall maintain profile information.
- **FR-PRV-002 Supported Services [R1]:** Provider shall configure supported services.
- **FR-PRV-003 Service Area [R1]:** Provider shall configure geographic service areas.
- **FR-PRV-004 Pricing [R1]:** Provider shall maintain applicable pricing information.
- **FR-PRV-005 Availability [R1]:** Provider shall configure service availability.
- **FR-PRV-006 Verification [R1]:** Administrator shall approve/reject provider verification.
- **FR-PRV-007 Matching Eligibility [R1]:** Only active, verified providers shall be eligible for matching/discovery.

### 2.4 Manual Service Request
- **FR-REQ-001 Select Category [R1]:** Customer shall manually select a service category.
- **FR-REQ-002 Select Service [R1]:** Customer shall manually select a service.
- **FR-REQ-003 Problem Description [R1]:** Customer shall provide service-need description.
- **FR-REQ-004 Service Location [R1]:** Customer shall provide/select service location.
- **FR-REQ-005 Preferred Time [R1]:** Customer shall provide preferred service schedule.
- **FR-REQ-006 Budget [R1]:** Customer may provide a budget.
- **FR-REQ-007 Review Request [R1]:** Customer shall review request information before submission.
- **FR-REQ-008 Create Request [R1]:** System shall create a Service Request after successful validation.
- **FR-REQ-009 Request Status [R1]:** System shall track Service Request status.

**Critical constraint:** FR-REQ-001 through FR-REQ-009 must not depend on AI.

### 2.5 Manual Provider Discovery
- **FR-MATCH-001 Filter by Service [R1]:** System shall return providers supporting the requested service.
- **FR-MATCH-002 Filter by Provider State [R1]:** System shall exclude inactive or unverified providers.
- **FR-MATCH-003 Filter by Service Area [R1]:** System shall exclude providers outside the request service area.
- **FR-MATCH-004 Filter by Availability [R1]:** System shall support filtering candidates by availability.
- **FR-MATCH-005 Display Eligible Providers [R1]:** System shall display eligible providers to Customer.
- **FR-MATCH-006 Provider Detail [R1]:** Customer shall view provider details before selection.
- **FR-MATCH-007 Select Provider [R1]:** Customer shall explicitly choose the provider.
- **FR-MATCH-008 No Provider Found [R1]:** System shall provide an appropriate result when no provider satisfies mandatory criteria.

R1 matching/filtering is deterministic and shall not require AI ranking.

### 2.6 Quotation
- **FR-QUOTE-001 Request Quotation [R1]:** Selected provider shall be able to prepare a quotation.
- **FR-QUOTE-002 Submit Quotation [R1]:** Provider shall submit quotation information.
- **FR-QUOTE-003 View Quotation [R1]:** Customer shall view quotation details.
- **FR-QUOTE-004 Accept Quotation [R1]:** Customer shall accept quotation.
- **FR-QUOTE-005 Reject Quotation [R1]:** Customer shall reject quotation.
- **FR-QUOTE-006 Booking Trigger [R1]:** Accepted quotation shall enable booking confirmation.

### 2.7 Booking & Fulfillment
- **FR-BOOK-001 Create Booking [R1]:** System shall create/confirm booking after applicable conditions are satisfied.
- **FR-BOOK-002 View Booking [R1]:** Customer and Provider shall view applicable booking information.
- **FR-BOOK-003 Start Job [R1]:** Authorized Provider shall start a confirmed service.
- **FR-BOOK-004 Complete Job [R1]:** Authorized Provider shall complete an in-progress service.
- **FR-BOOK-005 Cancel Booking [R1]:** Eligible actor shall cancel according to cancellation rules.
- **FR-BOOK-006 Booking History [R1]:** Customer and Provider shall view relevant historical bookings.

### 2.8 Payment
- **FR-PAY-001 Payment Amount [R1]:** System shall determine applicable payment amount from the accepted transaction.
- **FR-PAY-002 Record Payment [R1]:** System shall support the selected MVP payment mechanism.
- **FR-PAY-003 Payment Status [R1]:** System shall store payment status.
- **FR-PAY-004 Payment Failure [R1]:** System shall handle unsuccessful payment states where applicable.

### 2.9 Review
- **FR-REV-001 Review Eligibility [R1]:** Only eligible completed transactions shall allow review.
- **FR-REV-002 Submit Rating [R1]:** Customer shall submit a rating.
- **FR-REV-003 Submit Comment [R1]:** Customer may submit a review comment.
- **FR-REV-004 Provider Rating [R1]:** System shall calculate/display provider reputation from eligible reviews.
- **FR-REV-005 Review Moderation [R1]:** Administrator shall support applicable moderation operations.

### 2.10 Administration
- **FR-ADM-001 Customer Management [R1]**
- **FR-ADM-002 Provider Management [R1]**
- **FR-ADM-003 Provider Verification [R1]**
- **FR-ADM-004 Service Catalog Management [R1]**
- **FR-ADM-005 Booking Monitoring [R1]**
- **FR-ADM-006 Review Moderation [R1]**
- **FR-ADM-007 Basic Dashboard [R1]**

## 3. AI Enhancement Requirements
### 3.1 AI-Assisted Service Request
- **FR-AI-001 Natural Language Service Request [R2]:** Customer shall be able to enter a natural-language problem description.
- **FR-AI-002 Requirement Extraction [R2]:** System shall derive candidate structured information from natural language.
- **FR-AI-003 Service Suggestion [R2]:** System shall suggest service/category classifications.
- **FR-AI-004 Confidence Handling [R2]:** Insufficient-confidence AI results shall not be silently treated as authoritative.
- **FR-AI-005 Customer Confirmation [R2]:** Customer shall confirm/modify AI-produced structured information.
- **FR-AI-006 Fallback to Manual Flow [R2]:** Customer shall always be able to use the R1 manual request workflow.

### 3.2 AI-Assisted Provider Matching
- **FR-AIM-001 Candidate Input [R2]:** AI matching shall receive candidates produced by R1 eligibility filtering and shall not bypass mandatory eligibility rules.
- **FR-AIM-002 Provider Ranking [R2]:** System shall rank eligible candidates using matching signals.
- **FR-AIM-003 Top-N Recommendation [R2]:** System shall present a configurable Top-N set of recommended providers.
- **FR-AIM-004 Recommendation Score [R2]:** System shall provide an understandable representation of recommendation relevance where used.
- **FR-AIM-005 Recommendation Explanation [R2]:** System shall explain relevant factors behind recommendation.
- **FR-AIM-006 Customer Final Decision [R2]:** Customer remains responsible for provider selection.
- **FR-AIM-007 Fallback [R2]:** If AI ranking is unavailable, R1 manual provider discovery shall remain operational.

## 4. Business Rules
- **BR-001:** Customer must be able to complete the business journey without AI.
- **BR-002:** Customer manually selects category/service in R1.
- **BR-003:** Only active and verified providers are eligible candidates.
- **BR-004:** Provider must support requested service.
- **BR-005:** Provider must serve requested location.
- **BR-006:** Availability may limit provider eligibility.
- **BR-007:** Customer makes the final provider selection.
- **BR-008:** Recommendation shall not automatically create a booking.
- **BR-009:** Booking requires acceptance of applicable quotation.
- **BR-010:** Only valid booking/job lifecycle transitions are permitted.
- **BR-011:** Only eligible completed transactions may generate reviews.
- **BR-012:** AI-generated requirement information remains a suggestion until customer confirms it.
- **BR-013:** AI failure must not prevent access to manual marketplace functionality.
- **BR-014:** Mandatory eligibility rules execute before AI ranking.
- **BR-015:** AI is an enhancement of the baseline workflow, not a replacement.

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
10. Customer submits.
11. System creates Service Request.

**Postcondition:** Valid Service Request exists.

### UC-02 Find and Select Provider [R1]
1. System receives Service Request.
2. System retrieves providers supporting requested service.
3. System removes inactive/unverified providers.
4. System checks service area.
5. System applies availability filtering where applicable.
6. System displays eligible providers.
7. Customer reviews provider profiles.
8. Customer selects a provider.

**Constraint:** No AI dependency.

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

## 6. Release Traceability
| Capability | R1 Core | R2 AI |
|---|---|---|
| Account | Yes | - |
| Service Catalog | Yes | - |
| Provider Management | Yes | - |
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

## 7. AI Entry Criteria
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

## 8. Key Architecture/Requirement Principle
**AI depends on Core Marketplace. Core Marketplace must not depend on AI.**

Target dependency direction:

`Core Marketplace <- AI Enhancement`

The AI layer consumes `ServiceRequest + EligibleProviders` and may perform `Understand / Rank / Recommend / Explain`, while deterministic eligibility filtering and the complete transaction lifecycle remain operational independently.
