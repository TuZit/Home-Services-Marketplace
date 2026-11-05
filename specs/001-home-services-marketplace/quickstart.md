# Quickstart — Home Services Marketplace (R1 Core)

## Prerequisites

- Podman (machine running) + docker-compose
- Java 21, Gradle 9
- Node 24, pnpm 11

## Start PostgreSQL

```bash
export DOCKER_HOST='unix:///var/folders/fc/d427gzyx1d91jg260mnl0z6r0000gn/T/podman/podman-machine-default-api.sock'
podman compose -f compose.yaml up -d postgres
```

Wait for healthy, then run Flyway migrations (backend startup applies them).

## Backend

```bash
cd backend
./gradlew bootRun
```

Health: `http://localhost:8080/api/v1/...` (OpenAPI at `/swagger-ui.html`).

## Frontend

```bash
cd frontend
pnpm install
pnpm dev
```

Open `http://localhost:5173`.

## Smoke Validation

1. Register a Customer and a Provider.
2. Admin creates a category + service, then verifies the Provider.
3. Customer creates and confirms a request.
4. Customer discovers and selects the Provider.
5. Provider submits a quotation; Customer accepts it.
6. Provider starts and completes the booking.
7. Customer records payment and submits a review.
