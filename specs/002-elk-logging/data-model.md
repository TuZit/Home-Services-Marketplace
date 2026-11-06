# Data Model: Centralized Log Aggregation & Observability (ELK)

This feature introduces no relational tables. Its "data" is the structured log stream and the Elasticsearch/Kibana artifacts that store and present it.

## Log Event (JSON document)

A single JSON object per log line, produced by the backend and stored as one Elasticsearch document in `hsm-logs-*`.

| Field | Type | Required | Source | Notes |
|-------|------|----------|--------|-------|
| `@timestamp` | date (ISO-8601, UTC) | yes | structured encoder | Event time; used for the index date and time-range queries. |
| `@version` | string | yes | structured encoder | Logstash format version (`"1"`). |
| `message` | text | yes | log call | Human message. |
| `logger_name` | keyword | yes | Logback | Emitting logger (e.g. `...shared.logging.RequestTraceFilter`). |
| `thread_name` | keyword | yes | Logback | Emitting thread. |
| `level` | keyword | yes | Logback | `INFO`, `WARN`, `ERROR`, ... |
| `level_value` | integer | yes | structured encoder | Numeric level. |
| `traceId` | keyword | no | MDC (`LogContext.TRACE_ID`) | Correlation id; blank for non-request logs. |
| `userId` | keyword | no | MDC (`LogContext.USER_ID`) | Actor id; blank when unauthenticated. |
| `role` | keyword | no | MDC (`LogContext.ROLE`) | Actor role (`CUSTOMER`/`PROVIDER`/`ADMIN`). |
| `http_method` | keyword | no | access log (SLF4J fluent API) | HTTP method; present on access-log entries. |
| `http_status` | integer | no | access log (SLF4J fluent API) | HTTP response status; enables status-distribution views. |
| `http_duration_ms` | long | no | access log (SLF4J fluent API) | Request handling duration in ms; enables latency views. |
| `app` | keyword | yes | `logging.structured.json.add.app` | Static application tag (`hsm-backend`). |
| `environment` | keyword | yes | `logging.structured.json.add.environment` | Deployment tag (`local`, `staging`, ...). |
| `stack_trace` | text | no | encoder | Present when the log call includes an exception. |

Field mapping rules: identifiers (`traceId`, `userId`, `role`, `level`, `logger_name`, `app`, `environment`, `http_method`) are stored as `keyword` (exact-match filtering and aggregation); `message` and `stack_trace` as `text` (analyzed search); `@timestamp` as `date`; `level_value`, `http_status` as `integer`; `http_duration_ms` as `long`.

## Elasticsearch Index

- **Name pattern**: `hsm-logs-YYYY.MM.dd` (one index per UTC day, created on demand by Logstash).
- **Purpose**: time-partitioned storage that ILM can retire a day at a time.
- **Mapping source**: `infra/elk/elasticsearch/index-template.json` (composable template matching `hsm-logs-*`).

## Index Lifecycle Policy

- **Name**: `hsm-logs-policy`.
- **Phases**: `hot` (immediate, rollover not required for MVP) → `delete` after `30d` (configurable).
- **Bound to**: every index matching `hsm-logs-*` via the index template's `lifecycle.name`.
- **Satisfies**: FR-008, SC-007.

## Kibana Data View & Dashboard

- **Data view**: `hsm-logs-*` (time field `@timestamp`) — the queryable surface for FR-004.
- **Dashboard**: starter panels covering request volume, status/level distribution, error rate, and slow-logger/latency views (FR-005, SC-003).
- **Storage**: `infra/elk/kibana/saved-objects.ndjson`, imported via the Saved Objects API.

## Relationships

```text
Log Event --stored in--> Elasticsearch Index (hsm-logs-YYYY.MM.dd)
Elasticsearch Index --governed by--> ILM Policy (hsm-logs-policy)
Elasticsearch Index --matched by--> Index Template (hsm-logs-*)
Kibana Data View --queries--> Elasticsearch Indices (hsm-logs-*)
Kibana Dashboard --composed of--> Visualizations over the Data View
```

## State / Lifecycle of a Log Event

1. Emitted in-process and written to the host JSON file (application side).
2. Read by Logstash from the bind-mounted file and parsed (`codec => json`).
3. Indexed into the current day's `hsm-logs-*` index.
4. Queryable in Kibana until the ILM `delete` phase removes the index after the retention window.
