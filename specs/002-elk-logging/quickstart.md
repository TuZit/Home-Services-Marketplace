# Quickstart & Validation: Centralized Log Aggregation & Observability (ELK)

Validates the feature end-to-end: backend JSON logs carry `traceId`, they reach Elasticsearch, are searchable by `traceId` in Kibana, and retention is configured.

## Prerequisites

- Podman machine running (`podman machine start` if needed) and the `podman compose` provider available.
- Java 21 and the backend buildable (`./gradlew build`).
- Free ports: `9200` (Elasticsearch), `5000` (Logstash), `5601` (Kibana), `8080` or a free port for the backend.

## 1. Enable structured JSON logging in the backend

`backend/src/main/resources/application.yaml` must contain (already implemented):

```yaml
logging:
  file:
    name: ${LOG_FILE:logs/hsm-backend.json}
  structured:
    format:
      file: logstash
    json:
      add:
        app: hsm-backend
        environment: ${APP_ENV:local}
```

Run backend unit tests (fast, no containers):

```bash
cd backend && ./gradlew test --tests "*Logging*"
```

**Expected**: the log-formatting test passes, proving the file encoder emits `traceId`, `userId`, and `role`.

Prove the file is real (no containers needed):

```bash
cd backend
./gradlew bootRun --args='--server.port=8099' &   # or your usual run command
curl -s -D - -o /dev/null http://localhost:8099/api/v1/categories | grep -i x-request-id
grep traceId logs/hsm-backend.json | tail -1
```

**Expected**: the `X-Request-Id` header value appears as the `traceId` member on a line in `logs/hsm-backend.json`.

## 2. Start the ELK stack

```bash
# from the repository root; only the elk-profile services start
podman compose --profile elk up -d elasticsearch logstash kibana
```

Wait for health (Elasticsearch takes ~30–60 s):

```bash
until curl -sf http://localhost:9200/_cluster/health >/dev/null; do sleep 2; done
curl -s 'http://localhost:9200/_cluster/health?pretty' | grep -E '"status"|"number_of_nodes"'
```

**Expected**: a JSON health document with `"number_of_nodes": 1` (status may be `yellow` for a single node — acceptable).

## 3. Apply ILM policy, index template, and Kibana content

```bash
bash infra/elk/setup/apply-elk-config.sh
```

**Expected**: the script reports the ILM policy and index template created (HTTP 200), and the Kibana saved objects imported (`"success": true`).

## 4. Generate traffic and verify shipping

With the backend running (step 1) and writing `backend/logs/hsm-backend.json`:

```bash
for i in $(seq 1 5); do curl -s -o /dev/null http://localhost:8099/api/v1/categories; done
curl -s -o /dev/null http://localhost:8099/api/v1/me/requests   # 401 path
```

Give Logstash up to ~60 s (SC-006), then:

```bash
# indices created by Logstash
curl -s 'http://localhost:9200/_cat/indices/hsm-logs-*?v'

# fetch the most recent traceId seen in the app log, then look it up centrally
TRACE=$(grep -o '"traceId":"[^"]*"' backend/logs/hsm-backend.json | tail -1 | cut -d'"' -f4)
curl -s "http://localhost:9200/hsm-logs-*/_search?q=traceId:$TRACE&pretty" | grep -E '"total"' | head -1
```

**Expected**: at least one `hsm-logs-*` index exists, and the query for `traceId` returns `"total": { "value": <n> ... }` with `n >= 1` (User Story 2, SC-001/SC-002).

## 5. Inspect in Kibana

Open <http://localhost:5601>, then **Analytics → Discover** and select the `hsm-logs-*` data view. Filter `traceId : "<value from step 4>"`.

**Expected**: all log lines for that request are listed in time order (User Story 2). Open the **HSM Observability** dashboard for the status/volume panels (User Story 3).

## 6. Verify fail-open behaviour (FR-007 / SC-005)

```bash
podman compose --profile elk stop logstash elasticsearch
for i in $(seq 1 5); do curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8099/api/v1/categories; done
```

**Expected**: all requests still return `200`; the app keeps writing the local JSON file; no request fails because the log pipeline is down.

## 7. Teardown

```bash
podman compose --profile elk down
```

## Automated check

`infra/elk/setup/verify-elk.sh` performs steps 2–4 (health, index presence, traceId lookup) and exits non-zero on failure — use it as the repeatable validation harness.
