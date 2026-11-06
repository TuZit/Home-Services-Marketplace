#!/usr/bin/env bash
#
# Applies the ELK configuration for the Home Services Marketplace:
#  - the hsm-logs-policy ILM policy (30-day retention)
#  - the hsm-logs-* composable index template
#  - the Kibana data view, visualizations and dashboard
#
# Requires Elasticsearch and Kibana to be reachable (default localhost ports).
set -euo pipefail

ES_URL="${ES_URL:-http://localhost:9200}"
KIBANA_URL="${KIBANA_URL:-http://localhost:5601}"
POLICY_NAME="${POLICY_NAME:-hsm-logs-policy}"
TEMPLATE_NAME="${TEMPLATE_NAME:-hsm-logs-template}"

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

wait_for() {
  local url="$1" name="$2" attempts="${3:-60}"
  for _ in $(seq 1 "$attempts"); do
    if curl -sf -o /dev/null "$url"; then
      echo "OK: $name is reachable ($url)"
      return 0
    fi
    sleep 2
  done
  echo "ERROR: $name not reachable at $url" >&2
  return 1
}

echo "== Waiting for Elasticsearch =="
wait_for "$ES_URL/_cluster/health" "Elasticsearch"

echo "== Applying ILM policy '$POLICY_NAME' =="
curl -sf -X PUT "$ES_URL/_ilm/policy/$POLICY_NAME" \
  -H 'Content-Type: application/json' \
  -d @"$DIR/elasticsearch/ilm-policy.json" >/dev/null
echo "OK: ILM policy applied"

echo "== Applying index template '$TEMPLATE_NAME' =="
curl -sf -X PUT "$ES_URL/_index_template/$TEMPLATE_NAME" \
  -H 'Content-Type: application/json' \
  -d @"$DIR/elasticsearch/index-template.json" >/dev/null
echo "OK: index template applied"

echo "== Waiting for Kibana =="
wait_for "$KIBANA_URL/api/status" "Kibana"

echo "== Importing Kibana saved objects =="
curl -sf -X POST "$KIBANA_URL/api/saved_objects/_import?overwrite=true" \
  -H 'kbn-xsrf: true' \
  -F "file=@$DIR/kibana/saved-objects.ndjson" >/dev/null
echo "OK: saved objects imported"

echo "Done. Open $KIBANA_URL -> Analytics -> Discover (data view 'hsm-logs-*')."
