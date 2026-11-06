# Contract: Log Event & ELK Interfaces

This feature exposes no HTTP API. Its contracts are (a) the JSON log-event document the backend writes, and (b) the fixed names/configs that bind the app, Logstash, Elasticsearch, and Kibana together.

## C1. Log event document (producer: backend, consumer: Logstash)

One compact JSON object per line. Produced by Spring Boot structured logging (`logging.structured.format.file=logstash`).

```json
{"@timestamp":"2026-11-05T16:07:06.408123Z","@version":"1","message":"GET /api/v1/categories -> 200 (79 ms)","logger_name":"ptit.home_services_marketplace.access","thread_name":"http-nio-8080-exec-1","level":"INFO","level_value":20000,"traceId":"e05f575078224d33","userId":"","role":"","http_method":"GET","http_status":200,"http_duration_ms":79,"app":"hsm-backend","environment":"local"}
```

Required members: `@timestamp`, `@version`, `message`, `logger_name`, `level`, `level_value`, `app`, `environment`.
Optional members: `traceId`, `userId`, `role`, `http_method`, `http_status`, `http_duration_ms`, `thread_name`, `stack_trace`.

- **Traceability contract**: for any handled request, at least one line carries the same `traceId` value that the app returned in the `X-Request-Id` response header (existing `RequestTraceFilter`/`LogContext` behaviour). A trace lookup is a term query on `traceId`.
- **Aggregation contract**: access-log lines carry structured `http_method`, `http_status` and `http_duration_ms` members (emitted via the SLF4J fluent API) so status-distribution and latency views never parse the message text.
- **Redaction contract**: the document MUST NOT contain passwords, tokens, secrets, or unnecessary personal data (NFR-02, FR-006). Untrusted values are length-limited/sanitised before logging (existing `RequestTraceFilter` regex for `X-Request-Id` and control-character stripping + length cap for the logged request URI).

## C2. Index naming contract

- Indices are named `hsm-logs-YYYY.MM.dd` (UTC).
- A composable index template matching `hsm-logs-*` applies the mapping and binds the ILM policy.
- Kibana data view pattern: `hsm-logs-*`, time field `@timestamp`.

## C3. ILM policy contract

- Policy id: `hsm-logs-policy`.
- Delete phase after `30d` (configurable via `infra/elk/elasticsearch/ilm-policy.json`).
- Referenced by the index template as `index.lifecycle.name`.

## C4. Logstash pipeline contract

- Pipeline id/file: `hsm-logs` (`infra/elk/logstash/pipeline/hsm-logs.conf`).
- Input: `file` on `/var/log/hsm/hsm-backend.json`, `codec => json`, `sincedb` under `/usr/share/logstash/data`.
- Host path bind-mounted read-only: `./backend/logs` → `/var/log/hsm`.
- Output: Elasticsearch at `http://elasticsearch:9200`, `index => "hsm-logs-%{+YYYY.MM.dd}"`, `ilm_enabled => false` (ILM is applied by the index template, not the output plugin).

## C5. Application logging configuration contract

These Spring properties are the app-side interface (in `backend/src/main/resources/application.yaml`):

| Property | Value | Purpose |
|----------|-------|---------|
| `logging.file.name` | `${LOG_FILE:logs/hsm-backend.json}` | JSON log file the shipper reads. |
| `logging.structured.format.file` | `logstash` | Emit JSON lines + include MDC. |
| `logging.structured.json.add.app` | `hsm-backend` | Static source tag. |
| `logging.structured.json.add.environment` | `${APP_ENV:local}` | Environment tag. |
| `logging.pattern.console` | human pattern with `%X{traceId}` | Developer console output. |
| `logging.logback.rollingpolicy.*` | size/history caps | Bound local disk usage. |

## C6. Kibana content contract

- Data view name: `hsmlogs-dataview` over `hsm-logs-*`.
- Starter dashboard + visualizations are shipped in `infra/elk/kibana/saved-objects.ndjson` and imported via `POST /api/saved_objects/_import` (`overwrite=true`).
