# API Contracts — Home Services Marketplace (R1 Core)

Base path: `/api/v1`. JSON. All consequential writes accept an `Idempotency-Key` header.

## Auth

- `POST /auth/register` — register Customer or Provider.
- `POST /auth/login` — authenticate, returns access token + sets refresh cookie.
- `POST /auth/refresh` — rotate refresh token.
- `POST /auth/logout` — revoke refresh token.

## Catalogue

- `GET /categories` — public active categories.
- `GET /categories/{id}/services` — active services.
- `GET /services/{id}` — service detail.
- Admin: `POST/PUT/DELETE /admin/categories`, `POST/PUT/DELETE /admin/services` (activate/deactivate, no hard delete).

## Customer

- `GET/PUT /me` — profile.
- `GET/POST/PUT/DELETE /me/addresses` — address book.

## Provider

- `GET/PUT /provider/profile`
- `GET/PUT /provider/services`
- `GET/PUT /provider/areas`
- `GET/PUT /provider/availability`
- `POST /provider/submit-verification`

## Service Requests

- `POST /service-requests` — create draft.
- `PUT /service-requests/{id}` — update draft.
- `POST /service-requests/{id}/confirm` — confirm immutable version (idempotent).
- `POST /service-requests/{id}/cancel` — cancel.
- `GET /service-requests/{id}/eligible-providers` — deterministic discovery.
- `PUT /service-requests/{id}/provider-selection` — select provider.

## Quotation

- `POST /provider/requests/{id}/quotations` — create draft quotation.
- `POST /quotations/{id}/submit` — submit.
- `POST /quotations/{id}/withdraw` — withdraw.
- `POST /quotations/{id}/accept` — accept (idempotent, creates booking + reservation).
- `POST /quotations/{id}/reject` — reject.

## Booking / Fulfillment

- `GET /bookings/{id}`
- `POST /bookings/{id}/start`
- `POST /bookings/{id}/complete`
- `POST /bookings/{id}/cancel`

## Payment

- `POST /bookings/{id}/payments` — create payment attempt.
- `POST /payments/{id}/result` — simulator callback (idempotent).

## Review

- `POST /bookings/{id}/reviews` — submit review.

## Admin

- `GET /admin/users`, `POST /admin/users/{id}/suspend|reactivate`
- `POST /admin/providers/{id}/verify|reject|suspend`
- `GET /admin/requests`, `GET /admin/bookings`
- `POST /admin/reviews/{id}/hide|restore`
- `GET /admin/audit`, `GET /admin/dashboard`

## Error Semantics

- 400 malformed, 401 unauthenticated, 403 unauthorized, 404 not found, 409 conflict (state/concurrency/slot/idempotency), 422 business validation, 503 external unavailable.
