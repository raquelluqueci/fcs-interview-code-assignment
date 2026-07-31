#!/usr/bin/env bash
# Start recruiter compose stack (fail-closed on port clash with Odin native stack).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.observability.yml}"
REQUIRED_PORTS=(15432 4317 4318 8889 9090 3001)

port_in_use() {
  local p="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$p" -sTCP:LISTEN >/dev/null 2>&1
  else
    return 1
  fi
}

echo "==> Preflight ports (fail-closed if busy)"
busy=0
for p in "${REQUIRED_PORTS[@]}"; do
  if port_in_use "$p"; then
    echo "BLOCK: port $p already listening — use native Odin stack (make odin-install) or free the port"
    busy=1
  fi
done
if [[ "$busy" -ne 0 ]]; then
  exit 2
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "BLOCK: docker not found"
  exit 2
fi

docker compose -f "$COMPOSE_FILE" up -d
docker compose -f "$COMPOSE_FILE" ps
echo "OK: stack up. Run app on host: make run-senior"
echo "Grafana http://localhost:3001 | Prometheus http://localhost:9090 | OTEL :4317"
