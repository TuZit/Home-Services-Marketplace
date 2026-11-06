# Phase 0 Research: Centralized Log Aggregation & Observability (ELK)

All Technical Context items were resolvable with reasonable defaults (recorded in the spec Assumptions); no `NEEDS CLARIFICATION` remained.

## R1. Structured JSON log emission

- **Decision**: Use Spring Boot 4's built-in structured logging — `logging.structured.format.file=logstash` with `logging.file.name` — keeping the human-readable console pattern for developers.
- **Rationale**: Zero new Java dependencies; the official Logstash JSON format emits one JSON object per line and, per the Spring Boot reference, "adds every key value pair contained in the MDC to the JSON object" — so the existing `traceId`, `userId`, `role` MDC values ship automatically. Configurable via `logging.structured.json.add` (e.g. `app`, `environment`).
- **Alternatives considered**: `net.logstash.logback:logstash-logback-encoder` (more encoder features and custom fields, but an extra dependency and a hand-written `logback-spring.xml`); switching to Log4j2 (unnecessary framework change).

## R2. Log shipping mechanism

- **Decision**: The application writes JSON lines to a local file; **Logstash** reads that file through a bind-mounted directory (`file` input, `codec => json`).
- **Rationale**: Decouples the application from the log infrastructure, making the fail-open guarantee trivial (the app only touches the local filesystem). Avoids adding a network appender dependency. One fewer moving part than inserting Filebeat.
- **Alternatives considered**: `LogstashTcpSocketAppender`/`SocketAppender` from the app (app becomes coupled to Logstash availability; needs the encoder dependency and careful async/wait strategies); Filebeat sidecar (resilient and standard, but an additional service and another config surface for an MVP).

## R3. Index naming & document routing

- **Decision**: Logstash writes to daily indices `hsm-logs-YYYY.MM.dd` using the Elasticsearch output's `%{+YYYY.MM.dd}` index expansion.
- **Rationale**: Simple, time-partitioned, and directly compatible with index lifecycle management.
- **Alternatives considered**: Elasticsearch data streams (cleaner rollover and ILM integration, but extra bootstrap — component templates, a data stream name, and `data_stream` output settings — for little MVP benefit).

## R4. Retention

- **Decision**: An Elasticsearch ILM policy (`hsm-logs-policy`) with a single hot phase and a **delete** phase after **30 days**, bound to `hsm-logs-*` through a composable index template.
- **Rationale**: Native, server-side, requires no application involvement; satisfies FR-008/SC-007. The window is parameterised in one JSON file so it stays configurable.
- **Alternatives considered**: `elasticsearch-curator` (extra dependency/container); a cron job issuing delete-by-query (fragile and heavier).

## R5. Kibana content provisioning

- **Decision**: Provide a `saved-objects.ndjson` (a data view `hsm-logs-*` plus a starter dashboard) applied through the Kibana Saved Objects API from `infra/elk/setup/apply-elk-config.sh`.
- **Rationale**: Reproducible, version-controllable, and scriptable — no manual clicking required to reach SC-003.
- **Alternatives considered**: Manual dashboard creation (not reproducible); Terraform/Ansible (heavyweight for a graduation MVP).

## R6. Local security posture

- **Decision**: Run the Elasticsearch node with `xpack.security.enabled=false` for the local MVP only, and document that a shared/production environment must enable TLS + authentication and restrict Kibana access (FR-009).
- **Rationale**: Keeps local setup friction low while the scope explicitly targets a local MVP; the security requirement remains documented and is enforced by network scoping (containers are not exposed publicly beyond localhost).
- **Alternatives considered**: Full TLS + built-in users out of the box (significant certificate/secret management for a local demo).

## R7. Time handling

- **Decision**: All log timestamps are emitted in UTC (`logging.pattern.dateformat` unaffected; the structured encoder writes ISO-8601 `@timestamp`).
- **Rationale**: Consistent cross-instance ordering (edge case in the spec); matches the backend's existing UTC storage convention.

## R8. Resource sizing for the local Podman VM

- **Decision**: Cap heaps for the local stack — Elasticsearch `-Xms512m -Xmx512m`, Logstash `-Xms256m -Xmx256m` — and run Elasticsearch as a single node (`discovery.type=single-node`) with `bootstrap.memory_lock=false`.
- **Rationale**: The available Podman VM has ~3.7 GiB RAM; the three JVM services must coexist with PostgreSQL.
- **Alternatives considered**: Default heaps (Elasticsearch defaults to ~2 GiB and can get the node OOM-killed in the VM).
