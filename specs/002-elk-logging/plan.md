# Implementation Plan: Centralized Log Aggregation & Observability (ELK)

**Branch**: `002-elk-logging` | **Date**: 2026-11-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-elk-logging/spec.md`

## Summary

Emit structured JSON application logs from the backend (each line carries the existing per-request `traceId`, `userId`, `role` MDC fields), ship that log file to a self-hosted ELK stack (Elasticsearch + Logstash + Kibana) via Podman Compose, index into daily `hsm-logs-*` indices, and expose Kibana data views/dashboards plus an ILM retention policy. Logging stays fail-open: the app only writes to a local file, so an ELK outage never affects business transactions.

## Technical Context

**Language/Version**: Java 21 (backend only; no frontend change)

**Primary Dependencies**: Spring Boot 4.1.2 native structured logging (Logback `StructuredLogEncoder`, SLF4J MDC) — no new Java dependency; Elastic Stack 8.15 (Elasticsearch, Logstash, Kibana) via Podman Compose

**Storage**: JSON log file on the host (`backend/logs/hsm-backend.json`, rotation-managed) shipped into Elasticsearch indices `hsm-logs-YYYY.MM.dd`

**Testing**: JUnit 5 (backend) — a logging test asserting the structured encoder output contains `traceId`/`userId`/`role`; end-to-end validation via `quickstart.md` and a shell verification script

**Target Platform**: Linux containers (Elastic Stack) on the local Podman machine; Spring Boot runs on the host JVM in development

**Project Type**: Web service (backend) + supporting infrastructure

**Performance Goals**: log entry visible in the central store within 60 s of being produced (SC-006); trace lookup under 5 s (SC-002)

**Constraints**: logging is fail-open (must not block/delay business transactions); no secrets or PII in logs; UTC timestamps; 30-day configurable retention; Elasticsearch single-node with a small heap to fit the Podman VM

**Scale/Scope**: graduation-project MVP; single-node Elasticsearch; backend application logs only

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Manual-First: logging is orthogonal to business flow and adds no AI dependency.
- [x] Eligibility Before Ranking: unaffected.
- [x] Human Decision Authority: unaffected.
- [x] Traceability & Auditability: strengthened — centralized logs join the existing DB audit trail via the shared `traceId`.
- [x] No Hidden Automation: logging never advances a transaction; it is observational only.
- [x] Historical Integrity: log retention is time-based; business history is unaffected.
- [x] Technical Constraints: Java 21 / Spring Boot / PostgreSQL unchanged; adds container infrastructure only.
- [x] Security: secrets remain externalized; logs must never emit secrets or PII (NFR-02, NFR-13).

No constitution violations requiring justification.

## Project Structure

### Documentation (this feature)

```text
specs/002-elk-logging/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── log-event-schema.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── src/main/resources/application.yaml     # structured JSON file logging + levels
├── src/test/java/.../shared/logging/       # encoder/MDC assertions
├── .gitignore                              # ignore logs/
└── logs/                                   # runtime JSON log output (gitignored)

infra/elk/
├── logstash/
│   ├── config/{logstash.yml,pipelines.yml}
│   └── pipeline/hsm-logs.conf              # file input (json) -> ES output
├── elasticsearch/
│   ├── ilm-policy.json                     # 30-day delete retention
│   └── index-template.json                 # hsm-logs-* mapping + ILM binding
├── kibana/saved-objects.ndjson             # data view + dashboard
├── setup/apply-elk-config.sh               # apply ILM/template/objects via API
└── README.md

compose.yaml                                # + elasticsearch/logstash/kibana (profile: elk)
```

**Structure Decision**: Web application (Option 2) — changes are confined to the existing `backend/` module plus a new `infra/elk/` infrastructure tree and an `elk`-profiled extension of the root `compose.yaml`. No frontend change.

## Complexity Tracking

No constitution violations requiring justification.

## Re-evaluation After Phase 1 Design

Constitution Check re-run after data-model, contracts, and quickstart were produced: all gates still pass. The design adds only observational infrastructure; no business entity, state transition, or authorization surface changes.
