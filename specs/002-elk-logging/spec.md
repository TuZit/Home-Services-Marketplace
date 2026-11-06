# Feature Specification: Centralized Log Aggregation & Observability (ELK)

**Feature Branch**: `002-elk-logging`

**Created**: 2026-11-05

**Status**: Draft

**Input**: User description: "Ghi log với ELK (Elasticsearch, Logstash, Kibana): thu thập, vận chuyển và trực quan hóa log của backend (bao gồm traceId tương quan giữa các request) lên stack ELK."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Centralized Log Collection (Priority: P1)

An operator can see every backend log line from every running instance in one central place instead of tailing separate server consoles. Structured application logs — including the request correlation identifier (traceId) and actor context — are collected and forwarded to a shared log store.

**Why this priority**: Without central collection there is nothing to search or visualize. This is the foundation every other story builds on and delivers value on its own (all logs in one place).

**Independent Test**: Run the backend, generate a few requests, and confirm the corresponding log lines appear in the central log store on a separate machine/host, each carrying its request's correlation identifier.

**Acceptance Scenarios**:

1. **Given** a running backend instance, **When** an HTTP request is handled, **Then** at least one structured log entry for that request is stored centrally, within a bounded delay.
2. **Given** two backend instances behind the same environment, **When** both handle traffic, **Then** logs from both instances appear centrally and are distinguishable by source.
3. **Given** the central log store is temporarily unreachable, **When** requests are still served, **Then** business responses are unaffected and log loss is bounded rather than blocking the application.

---

### User Story 2 - Request Trace Lookup (Priority: P1)

A developer or operator investigating an incident can take a single request correlation identifier and retrieve the full timeline of log entries for that request — access logs, authentication outcomes, handled errors, and state-changing operations — across the request lifecycle.

**Why this priority**: Fast, complete tracing is the primary reason to centralize logs; it is the core troubleshooting workflow.

**Independent Test**: Pick the correlation identifier of one request, query the central log store for it, and verify every entry produced by that request is returned in time order.

**Acceptance Scenarios**:

1. **Given** a request that succeeded, **When** the operator searches by its correlation identifier, **Then** all log entries for that request are returned.
2. **Given** a request rejected with 401/403 or a handled client error, **When** the operator searches by its correlation identifier, **Then** the rejection and its error code are retrievable.
3. **Given** a request that caused an unexpected server error, **When** the operator searches by its correlation identifier, **Then** the error entry with its stack trace is retrievable.

---

### User Story 3 - Operational Dashboards & Visualization (Priority: P2)

An operator can use prebuilt views to monitor overall health at a glance: request volume, response status distribution, error rate, and slowest endpoints, and to drill down into results.

**Why this priority**: Dashboards turn raw logs into ongoing situational awareness, but the feature is still usable (via search) without them.

**Independent Test**: Open the provided views, generate a controlled amount of traffic with known outcomes, and confirm the displayed counts and rates match the generated traffic.

**Acceptance Scenarios**:

1. **Given** a set of completed requests, **When** the operator opens the traffic dashboard, **Then** totals by status class match the observed requests within an acceptable tolerance.
2. **Given** a period containing errors, **When** the operator opens the error view, **Then** the error rate reflects the stored error entries.
3. **Given** a slow request, **When** the operator inspects the latency view, **Then** the slow endpoint is visible in a ranked list.

---

### User Story 4 - Safe Retention & Access Control (Priority: P2)

A security or operations owner can ensure stored logs follow a retention policy and are protected: no secrets or unnecessary personal data are stored, and only authorized operators can read logs.

**Why this priority**: Compliance and privacy must hold as soon as logs leave the application host, but the collection/tracing value can be demonstrated first.

**Independent Test**: Sample the stored logs and confirm no secrets/credentials/PII are present, confirm access is denied to an unauthorized caller, and confirm entries older than the retention window are removed.

**Acceptance Scenarios**:

1. **Given** stored logs from normal traffic, **When** sampled, **Then** no passwords, tokens, secrets, or unnecessary personal data are found.
2. **Given** an unauthorized caller, **When** it attempts to read the log store or its views, **Then** access is denied.
3. **Given** entries older than the retention window, **When** the retention cycle runs, **Then** those entries are no longer retrievable.

---

### Edge Cases

- The central log store or forwarding agent is unavailable: the application must keep serving requests and must not lose drafts or corrupt transactions.
- A log field contains untrusted input (e.g. header, path, query string) with newlines or control characters: entries must not be forged or split (log injection), and oversized values must be truncated.
- Multiline content such as stack traces must remain associated with the correct correlation identifier.
- Duplicate delivery: a log entry may be stored more than once; the design must tolerate at-least-once delivery without breaking trace lookups.
- High-volume bursts: forwarding must apply backpressure or bounded buffering rather than exhausting application memory.
- Timestamps must be normalized to a single time zone (UTC) so ordering is consistent across instances.
- An operator queries an unknown or malformed correlation identifier: the system returns an empty result, not an error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST emit structured application log entries for backend request handling and significant background events, including timestamp, severity, logger name, message, request correlation identifier, and actor context where available.
- **FR-002**: System MUST forward log entries from every backend instance to a single central log store.
- **FR-003**: System MUST preserve the request correlation identifier end-to-end so that all entries produced while handling one request share the same identifier and can be grouped.
- **FR-004**: System MUST make stored logs searchable and filterable by at least time range, severity, correlation identifier, and actor.
- **FR-005**: System MUST provide visualization views covering request volume, response status distribution, error rate, and endpoint latency, plus a trace lookup view.
- **FR-006**: System MUST exclude secrets, credentials, tokens, passwords, payment secrets, and unnecessary personal data from stored logs.
- **FR-007**: A failure of the log pipeline MUST NOT block, delay beyond a bounded limit, or alter the outcome of a business transaction; logging is fail-open.
- **FR-008**: System MUST enforce a configurable retention window after which log entries are purged.
- **FR-009**: System MUST restrict read access to stored logs and visualization views to authorized operators only.
- **FR-010**: System MUST sanitize and length-limit untrusted values before storing them, preventing log injection and oversized entries.
- **FR-011**: System MUST retain sufficient context to reconstruct the lifecycle of a single request (request intake, authentication outcome, handled errors, and state-changing operations) from stored logs.
- **FR-012**: System MUST tag log entries with their source (application/instance/environment) so entries from different deployments are distinguishable.
- **FR-013**: System MUST be deployable together with the existing containerized environment and configurable per environment (local development vs. shared environment).

### Key Entities *(include if feature involves data)*

- **Log Event**: a single structured record describing something that happened — timestamp, severity, logger, message, correlation identifier, actor, HTTP context (method, path, status, duration), and exception detail when present.
- **Correlation Identifier (traceId)**: the shared key that links all log events produced while handling one request.
- **Log Pipeline / Shipper**: the mechanism that collects events from the application and delivers them to the central store.
- **Central Log Store**: searchable storage for log events, organized for time-based queries and retention.
- **Dashboard / Saved View**: a reusable visualization composed of queries over stored log events.
- **Retention Policy**: the configurable rule set defining how long log events are kept and when they are purged.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of handled backend requests produce at least one stored log entry carrying that request's correlation identifier.
- **SC-002**: An operator can retrieve all log entries for a given correlation identifier in under 5 seconds.
- **SC-003**: For a controlled sample of requests with known outcomes, dashboard totals and error rate match the observed traffic within 5% tolerance.
- **SC-004**: Audit sampling of stored logs finds zero occurrences of passwords, tokens, secrets, or unnecessary personal data.
- **SC-005**: Inducing a log-pipeline outage causes no change in business transaction success rate and no request rejected because of logging.
- **SC-006**: Log entries from all running instances appear in the central store within 60 seconds of being produced.
- **SC-007**: Entries older than the configured retention window are not retrievable after the retention cycle runs.

## Assumptions

- The chosen stack is ELK — Elasticsearch (store), Logstash (shipping/processing), Kibana (search and dashboards) — self-hosted for this MVP; a lighter local setup may be used in development.
- The existing backend request correlation (a per-request `traceId` exposed in logs and echoed as `X-Request-Id`) is the shared identifier this feature builds on.
- Scope is backend application logs only; frontend and infrastructure/container logs are out of scope for this feature.
- Log volume is that of a graduation-project MVP; a single-node log store is sufficient and high-availability clustering is out of scope.
- Default retention window is 30 days (no explicit requirement given); it must be configurable.
- The immutable business audit trail remains in the primary database; this feature concerns diagnostic/operational logs and does not replace audit events.
- Log access is restricted to operators/admins of the environment; the log store is not exposed publicly.
- Existing logging and security constraints apply: secrets remain externalized and must never appear in logs (NFR-02, NFR-13).
