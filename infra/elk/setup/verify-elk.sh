#!/usr/bin/env bash
#
# Verifies the ELK logging pipeline end to end:
#  1. a hsm-logs-* index exists in Elasticsearch
#  2. a traceId taken from the backend JSON log file is retrievable centrally
#
# Exits non-zero on failure so it can be used as a validation harness.
set -euo pipefail

ES_URL="${ES_URL:-http://localhost:9200}"
APP_LOG="${APP_LOG:-backend/logs/hsm-backend.json}"

echo "== Checking hsm-logs-* indices =="
if ! curl -sf "$ES_URL/_cat/indices/hsm-logs-*?format=json" | grep -q '"hsm-logs-'; then
  echo "FAIL: no hsm-logs-* index found" >&2
  exit 1
fi
echo "OK: hsm-logs-* index present"

if [ ! -f "$APP_LOG" ]; then
  echo "SKIP: backend log file not found at $APP_LOG (start the app and generate traffic first)" >&2
  exit 0
fi

TRACE="$(grep -o '"traceId":"[^"]*"' "$APP_LOG" | tail -1 | cut -d'"' -f4 || true)"
if [ -z "${TRACE:-}" ]; then
  echo "SKIP: no traceId found in $APP_LOG" >&2
  exit 0
fi
echo "== Looking up traceId '$TRACE' centrally =="

COUNT="$(curl -sf "$ES_URL/hsm-logs-*/_count?q=traceId:$TRACE" \
  | sed -n 's/.*"count":\([0-9][0-9]*\).*/\1/p')"
if [ "${COUNT:-0}" -lt 1 ]; then
  echo "FAIL: traceId '$TRACE' not found in Elasticsearch" >&2
  exit 1
fi

echo "OK: traceId '$TRACE' found ($COUNT document(s))"
echo "PASS: log collection and trace lookup verified"
