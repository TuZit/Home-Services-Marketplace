# ELK logging stack

Centralized log aggregation for the backend: JSON application logs are shipped from a
local file into a self-hosted ELK stack.

```
backend (JSON log file)  ->  Logstash (file input -> json)  ->  Elasticsearch (hsm-logs-*)  ->  Kibana
```

## Layout

| Path | Purpose |
|------|---------|
| `compose.yaml` (repo root, profile `elk`) | Elasticsearch, Logstash, Kibana services |
| `logstash/config/logstash.yml` | Logstash node settings |
| `logstash/config/pipelines.yml` | Registers the `hsm-logs` pipeline |
| `logstash/pipeline/hsm-logs.conf` | `file` input (JSON) → Elasticsearch output |
| `elasticsearch/ilm-policy.json` | 30-day delete retention (`hsm-logs-policy`) |
| `elasticsearch/index-template.json` | `hsm-logs-*` mappings + ILM binding |
| `kibana/saved-objects.ndjson` | Data view, visualizations, `HSM Observability` dashboard |
| `setup/apply-elk-config.sh` | Applies ILM + template and imports Kibana objects |
| `setup/verify-elk.sh` | End-to-end check: index present + `traceId` lookup |

## Run

```bash
# 1. start only the ELK services (profile "elk")
podman compose --profile elk up -d elasticsearch logstash kibana

# 2. apply ILM policy, index template and Kibana content
bash infra/elk/setup/apply-elk-config.sh

# 3. run the backend so it writes backend/logs/hsm-backend.json, then generate traffic
# 4. verify shipping + trace lookup
bash infra/elk/setup/verify-elk.sh
```

Kibana: <http://localhost:5601> → **Analytics → Discover** (data view `hsm-logs-*`), or the
**HSM Observability** dashboard.

## Retention

Indices matching `hsm-logs-*` are governed by `hsm-logs-policy`, which deletes them after
**30 days**. Change the window in `elasticsearch/ilm-policy.json` and re-run
`apply-elk-config.sh`.

## Security posture

For local development the Elasticsearch node runs with `xpack.security.enabled=false` and
the ports are published only on localhost. A shared or production environment MUST:

- enable TLS and authentication on Elasticsearch and Kibana, and
- restrict access to the Kibana UI to authorized operators.

Logs must never contain secrets or unnecessary personal data; the application is
responsible for redaction (see the backend logging configuration).
