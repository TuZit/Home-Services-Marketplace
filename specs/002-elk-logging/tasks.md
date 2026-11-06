---

description: "Task list for Centralized Log Aggregation & Observability (ELK)"
---

# Tasks: Centralized Log Aggregation & Observability (ELK)

**Input**: Design documents from `/specs/002-elk-logging/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Optional per the workflow; one focused logging test is included because plan.md lists it under Testing.

**Organization**: Tasks are grouped by user story. Paths are relative to the repository root.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Every task names an exact file path

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Non-blocking scaffolding for the feature.

- [X] T001 [P] Add `logs/` to `backend/.gitignore` so runtime JSON log output is never committed
- [X] T002 [P] Create the `infra/elk/{logstash/config,logstash/pipeline,elasticsearch,kibana,setup}/` directory tree and a starter `infra/elk/README.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The ELK platform must exist before any story can be observed.

**⚠️ CRITICAL**: No user story validation can begin until this phase is complete.

- [X] T003 Add `elasticsearch`, `logstash`, and `kibana` services (compose profile `elk`) plus named volumes to `compose.yaml`, with single-node Elasticsearch and small heaps (`ES_JAVA_OPTS=-Xms512m -Xmx512m`, `LS_JAVA_OPTS=-Xms256m -Xmx256m`) per research R8, and `xpack.security.enabled=false` per research R6
- [X] T004 [P] Write `infra/elk/logstash/config/logstash.yml` (minimal settings: `http.host`, `xpack.monitoring.enabled: false`)
- [X] T005 [P] Write `infra/elk/logstash/config/pipelines.yml` registering pipeline id `hsm-logs` → `pipeline/hsm-logs.conf`

**Checkpoint**: `podman compose --profile elk up -d elasticsearch kibana` starts; Logstash starts once the pipeline exists (T008).

---

## Phase 3: User Story 1 - Centralized Log Collection (Priority: P1) 🎯 MVP

**Goal**: The backend emits structured JSON logs that carry the request `traceId`, and Logstash indexes them centrally into `hsm-logs-*`.

**Independent Test**: Run the backend, issue a request, and confirm a document for that request exists in Elasticsearch with the same `traceId` as the `X-Request-Id` response header.

- [X] T006 [P] [US1] Enable structured JSON file logging in `backend/src/main/resources/application.yaml`: `logging.file.name: ${LOG_FILE:logs/hsm-backend.json}`, `logging.structured.format.file: logstash`, `logging.structured.json.add.app: hsm-backend`, `logging.structured.json.add.environment: ${APP_ENV:local}`, and `logging.logback.rollingpolicy.{max-file-size,max-history,total-size-cap}` caps (contract C5)
- [X] T007 [P] [US1] Add `backend/src/test/java/ptit/home_services_marketplace/shared/logging/StructuredLoggingTest.java` asserting that a log event with MDC `traceId`/`userId`/`role` is serialised by the `logstash` structured formatter to JSON containing those three members (contract C1)
- [X] T008 [US1] Write `infra/elk/logstash/pipeline/hsm-logs.conf`: `file` input on `/var/log/hsm/hsm-backend.json` with `codec => json`, `sincedb_path` under `/usr/share/logstash/data`, and an `elasticsearch` output to `http://elasticsearch:9200` with `index => "hsm-logs-%{+YYYY.MM.dd}"` and `ilm_enabled => false` (contract C4)
- [X] T009 [US1] Add the `./backend/logs:/var/log/hsm:ro` bind mount to the `logstash` service in `compose.yaml` (same file as T003 — not parallel)
- [X] T010 [US1] Verify US1 independently: follow `specs/002-elk-logging/quickstart.md` steps 1 and 4 and confirm a `hsm-logs-*` index and a document with the request's `traceId` exist; record the observed `_cat/indices` and `_search` output

**Checkpoint**: A request produces at least one centrally stored log document. This is the MVP.

---

## Phase 4: User Story 2 - Request Trace Lookup (Priority: P1)

**Goal**: An operator can retrieve every log line for one request by its `traceId`.

**Independent Test**: Pick a `traceId` from the app log and query `hsm-logs-*/_search?q=traceId:<value>`; all of that request's lines are returned.

- [X] T011 [P] [US2] Author `infra/elk/elasticsearch/index-template.json`: composable template matching `hsm-logs-*`, mapping `@timestamp` as `date`, `traceId`/`userId`/`role`/`level`/`logger_name`/`app`/`environment` as `keyword`, and `message`/`stack_trace` as `text` (data-model.md)
- [X] T012 [P] [US2] Add the `hsm-logs-*` data view (time field `@timestamp`) to `infra/elk/kibana/saved-objects.ndjson` (contract C6)
- [X] T013 [US2] Write `infra/elk/setup/apply-elk-config.sh`: wait for Elasticsearch, `PUT` the index template (and ILM policy once T017 exists) and `POST /api/saved_objects/_import?overwrite=true` for the Kibana objects; fail non-zero on error
- [X] T014 [US2] Write `infra/elk/setup/verify-elk.sh`: assert a `hsm-logs-*` index exists and that a `traceId` term query returns at least one hit, exiting non-zero otherwise

**Checkpoint**: traceId lookup works from Elasticsearch and Kibana Discover.

---

## Phase 5: User Story 3 - Operational Dashboards (Priority: P2)

**Goal**: Prebuilt Kibana views show volume, status/level distribution, error rate, and latency.

**Independent Test**: Generate known traffic and confirm the dashboard counts/rates match within tolerance.

- [X] T015 [US3] Add visualization objects (request volume, level/status distribution, error rate over time, slowest loggers) to `infra/elk/kibana/saved-objects.ndjson`, each referencing the `hsmlogs-dataview` data view
- [X] T016 [US3] Add a starter dashboard object named `HSM Observability` to `infra/elk/kibana/saved-objects.ndjson` referencing the T015 visualizations (same file as T015 — sequential)

**Checkpoint**: the dashboard renders against real traffic.

---

## Phase 6: User Story 4 - Safe Retention & Access Control (Priority: P2)

**Goal**: Logs follow a 30-day retention policy, contain no secrets/PII, and are not openly accessible.

**Independent Test**: Confirm the ILM policy is attached to `hsm-logs-*`, that purge occurs after the window, and that sampled documents contain no secrets.

- [X] T017 [P] [US4] Author `infra/elk/elasticsearch/ilm-policy.json` with a `hot` phase and a `delete` phase after `30d` (contract C3, FR-008)
- [X] T018 [US4] Bind the ILM policy to the indices by adding `index.lifecycle.name: hsm-logs-policy` to `infra/elk/elasticsearch/index-template.json` (same file as T011 — depends on T011)
- [X] T019 [P] [US4] Document the access-control posture in `infra/elk/README.md`: local runs disable xpack security; a shared environment must enable TLS/auth and restrict Kibana access (FR-009)
- [X] T020 [US4] Verify redaction: sample `backend/logs/hsm-backend.json` and confirm no passwords, tokens, secrets, or tokens appear (FR-006, quickstart step 6)

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T021 [P] Validate the compose file: `podman compose --profile elk config` succeeds with no schema errors
- [X] T022 [P] Document ELK run/verify steps in `docs/` (or `infra/elk/README.md`) and link from `specs/002-elk-logging/quickstart.md`
- [X] T023 Run the end-to-end validation (`bash infra/elk/setup/verify-elk.sh`) and record the observed results

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: depends on Setup; blocks all stories
- **US1 (Phase 3)**: depends on Foundational
- **US2 (Phase 4)**: depends on Foundational; integrates with US1's shipped logs
- **US3 (Phase 5)**: depends on US2 (data view) 
- **US4 (Phase 6)**: depends on US2 (index template)
- **Polish (Phase 7)**: depends on desired stories complete

### Key File Dependencies (avoid conflicts)

- `compose.yaml`: T003 → T009 (sequential)
- `infra/elk/logstash/pipeline/hsm-logs.conf`: T008
- `infra/elk/elasticsearch/index-template.json`: T011 → T018 (sequential)
- `infra/elk/kibana/saved-objects.ndjson`: T012 → T015 → T016 (sequential)

### Parallel Opportunities

- T001, T002 run in parallel
- T004, T005 run in parallel
- T006, T007 run in parallel
- T011, T012 run in parallel
- T017, T019 run in parallel (T017 before T013 uses it)

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 → Phase 2 → Phase 3 (US1)
2. **STOP and VALIDATE**: confirm a request's log document lands in Elasticsearch
3. That delivers the core value: centralized, correlated logs

### Incremental Delivery

1. US1 → centralized collection (MVP)
2. US2 → trace lookup (searchability)
3. US3 → dashboards
4. US4 → retention & access hardening

---

## Analysis Remediation (from /speckit-analyze)

- **C1 (HIGH)** — Access-log entries now emit structured `http_method` / `http_status` / `http_duration_ms` via the SLF4J fluent API (T006–T008) so status and latency views aggregate without parsing the message text. `data-model.md` and `contracts/log-event-schema.md` updated to match.
- **E1 (MEDIUM)** — Fail-open is explicitly verified (T010): with Logstash stopped, requests still returned 200/401 and the local JSON log kept growing.
- **E3 (LOW)** — The logged request URI is now control-character stripped and length-capped in `RequestTraceFilter` (T006).

## Validation Evidence

- `./gradlew test` — BUILD SUCCESSFUL (includes `home_services_marketplace` context load, `RequestTraceFilterTest`, `StructuredLoggingTest`).
- `podman compose --profile elk config` — COMPOSE CONFIG OK.
- `bash infra/elk/setup/apply-elk-config.sh` — ILM policy + index template applied, Kibana saved objects imported (dashboard `HSM Observability`, 4 visualizations, data view `hsm-logs-*`).
- `bash infra/elk/setup/verify-elk.sh` — PASS: index `hsm-logs-2026.11.05` present and a `traceId` from the app log retrievable centrally.
