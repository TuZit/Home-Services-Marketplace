# Data Model — Home Services Marketplace (R1 Core)

See the canonical full DDL in [`Data_Model_Home_Services_Marketplace_v0.1.md`](../../../Data_Model_Home_Services_Marketplace_v0.1.md). This file summarizes entities and relationships for task planning.

## Entities

| Module | Entities |
|---|---|
| Identity | `user_account`, `refresh_token` |
| Customer | `customer_profile`, `customer_address` |
| Provider | `provider_profile`, `provider_service`, `provider_area`, `provider_availability` |
| Catalogue | `geo_area`, `service_category`, `service` |
| Request | `service_request`, `request_version` |
| Discovery | `discovery_run`, `discovery_candidate`, `provider_selection` |
| Quotation | `quotation`, `quotation_item` |
| Scheduling/Booking | `booking`, `provider_slot_reservation` |
| Payment | `payment_record` |
| Review | `review` |
| Audit/Idempotency | `audit_event`, `idempotency_record` |

## Key Relationships

- `service_category 1—N service`
- `user_account 1—1 customer_profile` and `user_account 1—1 provider_profile`
- `provider_profile 1—N provider_service / provider_area / provider_availability`
- `service_request 1—N request_version`
- `service_request 1—N discovery_run 1—N discovery_candidate`
- `service_request 1—N provider_selection N—1 provider_profile`
- `provider_selection 1—N quotation 1—N quotation_item`
- `quotation 1—1 booking`
- `booking 1—1 provider_slot_reservation`
- `booking 1—N payment_record`
- `booking 1—1 review`

## Critical Constraints

- Active slot reservation overlap is prevented by a GiST exclusion constraint.
- One active SUBMITTED quotation per selection.
- One RECORDED payment per booking (partial unique index).
- One review per booking (`booking_id UNIQUE`).
- Idempotency unique `(actor_id, operation, idem_key)`.
