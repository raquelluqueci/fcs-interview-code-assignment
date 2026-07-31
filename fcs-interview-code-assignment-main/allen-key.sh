#!/usr/bin/env bash
# ============================================================================
#  allen-key.sh — the one tool that assembles the entire warehouse.
#
#  Like every IKEA flat-pack, this project ships with a single Allen key.
#  Run it and it bolts together, in one go:
#    * PostgreSQL 16          (one database per module, no schema clashes)
#    * OTel Collector         (OTLP gRPC/HTTP ingest -> Prometheus exporter)
#    * Prometheus + Grafana   (provisioned datasource + FCS dashboard)
#    * java-assignment-architect  (Quarkus, DDD/hexagonal variant)
#    * java-assignment-senior     (Quarkus, pragmatic variant)
#
#  Every host port is picked at random from a free range at startup, so it
#  never clashes with whatever is already running on your machine.
#  Logs from all containers stream to your terminal (and to ./logs/).
#  Ctrl+C tears everything down. No manual disassembly required.
#
#  Requirements : docker OR podman (auto-detected). Nothing else.
#  Usage        : ./allen-key.sh          (prod fast-jars; swagger-ui at /q/swagger-ui)
#                 ./allen-key.sh --dev    (quarkus:dev containers; Dev UI at /q/dev-ui)
#  Exit codes   : 0 ok | 1 missing engine/build failure | 130 Ctrl+C
# ============================================================================
set -Eeuo pipefail

MODE=prod
[[ "${1:-}" == "--dev" ]] && MODE=dev

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUNTIME_DIR="${REPO_ROOT}/.allen-key"
LOG_DIR="${REPO_ROOT}/logs/allen-key-$(date +%Y%m%d-%H%M%S)"
NETWORK="fcs-allen-net"
PG_USER="quarkus_test"
PG_PASS="quarkus_test"
PORT_RANGE_MIN=20000
PORT_RANGE_SPAN=40000
CONTAINERS=(fcs-postgres fcs-otelcol fcs-prometheus fcs-grafana fcs-app-architect fcs-app-senior)
LOG_PIDS=()

# ---------------------------------------------------------------- engine ---
if command -v docker >/dev/null 2>&1; then
  ENGINE=docker
  export DOCKER_BUILDKIT=1
elif command -v podman >/dev/null 2>&1; then
  ENGINE=podman
else
  echo "ERROR: neither docker nor podman found in PATH." >&2
  exit 1
fi

# ------------------------------------------------------------ random ports --
CHOSEN_PORTS=""
rand_free_port() {
  local p
  while :; do
    p=$(( (RANDOM % PORT_RANGE_SPAN) + PORT_RANGE_MIN ))
    case " ${CHOSEN_PORTS} " in *" ${p} "*) continue ;; esac
    # bash /dev/tcp connect succeeds => port busy => keep looking
    if ! (exec 3<>"/dev/tcp/127.0.0.1/${p}") 2>/dev/null; then
      CHOSEN_PORTS="${CHOSEN_PORTS} ${p}"
      echo "${p}"
      return 0
    fi
    exec 3>&- 3<&- 2>/dev/null || true
  done
}

PORT_ARCHITECT="$(rand_free_port)"
PORT_SENIOR="$(rand_free_port)"
PORT_GRAFANA="$(rand_free_port)"
PORT_PROMETHEUS="$(rand_free_port)"
PORT_POSTGRES="$(rand_free_port)"

# ---------------------------------------------------------------- cleanup ---
cleanup() {
  trap - INT TERM EXIT
  echo ""
  echo "[allen-key] Ctrl+C? Fine. Disassembling the warehouse (this one, unlike"
  echo "[allen-key] the real IKEA kind, goes back in the box in one piece)..."
  for pid in "${LOG_PIDS[@]:-}"; do kill "${pid}" 2>/dev/null || true; done
  "${ENGINE}" rm -f "${CONTAINERS[@]}" >/dev/null 2>&1 || true
  "${ENGINE}" network rm "${NETWORK}" >/dev/null 2>&1 || true
  echo "[allen-key] All screws accounted for. Bye."
}
trap 'cleanup; exit 130' INT TERM
trap cleanup EXIT

# ------------------------------------------------------- generated configs --
mkdir -p "${RUNTIME_DIR}" "${LOG_DIR}"

# One database per module: both apps run hibernate drop-and-create and would
# fight over a shared schema.
cat > "${RUNTIME_DIR}/pg-init.sql" <<SQL
CREATE DATABASE quarkus_architect OWNER ${PG_USER};
CREATE DATABASE quarkus_senior OWNER ${PG_USER};
SQL

# In-network Prometheus config (the checked-in one scrapes host.docker.internal;
# here everything lives on the same container network).
cat > "${RUNTIME_DIR}/prometheus.yml" <<YML
global:
  scrape_interval: 15s
  evaluation_interval: 15s
rule_files:
  - /etc/prometheus/rules/*.yml
scrape_configs:
  - job_name: prometheus
    static_configs: [{ targets: ["localhost:9090"] }]
  - job_name: otel-collector
    static_configs: [{ targets: ["fcs-otelcol:8889"] }]
  - job_name: fcs-fulfilment
    metrics_path: /q/metrics
    static_configs:
      - targets: ["fcs-app-architect:8080"]
        labels: { variant: architect }
      - targets: ["fcs-app-senior:8080"]
        labels: { variant: senior }
YML

# ----------------------------------------------------------------- banner ---
cat <<'BANNER'

        ____________________________
       /                            \
      |   ALLEN KEY v1.0 (included)  |
       \____________________________/
              ||
              ||        Some assembly required.
         =====##=====   Zero assembly performed by you.
              ||
              ||__

BANNER
echo "[allen-key] engine: ${ENGINE} | logs: ${LOG_DIR}"

# ------------------------------------------------------------------ build ---
if [[ "${MODE}" == "prod" ]]; then
  echo "[allen-key] Step 1/6 — building both app images (first run downloads Maven deps, grab a köttbullar)..."
  "${ENGINE}" build -f "${REPO_ROOT}/Dockerfile.fullstack" \
    --build-arg MODULE=java-assignment-architect -t fcs-architect:allen "${REPO_ROOT}"
  "${ENGINE}" build -f "${REPO_ROOT}/Dockerfile.fullstack" \
    --build-arg MODULE=java-assignment-senior -t fcs-senior:allen "${REPO_ROOT}"
else
  echo "[allen-key] Step 1/6 — dev mode: no image build; apps run via quarkus:dev (Dev UI enabled)..."
fi

# ---------------------------------------------------------------- network ---
echo "[allen-key] Step 2/6 — network + leftover cleanup..."
"${ENGINE}" rm -f "${CONTAINERS[@]}" >/dev/null 2>&1 || true
"${ENGINE}" network rm "${NETWORK}" >/dev/null 2>&1 || true
"${ENGINE}" network create "${NETWORK}" >/dev/null

# --------------------------------------------------------------- database ---
echo "[allen-key] Step 3/6 — PostgreSQL on 127.0.0.1:${PORT_POSTGRES}..."
"${ENGINE}" run -d --name fcs-postgres --network "${NETWORK}" \
  -e POSTGRES_USER="${PG_USER}" -e POSTGRES_PASSWORD="${PG_PASS}" -e POSTGRES_DB=quarkus_test \
  -v "${RUNTIME_DIR}/pg-init.sql:/docker-entrypoint-initdb.d/10-init.sql:ro" \
  -p "127.0.0.1:${PORT_POSTGRES}:5432" \
  docker.io/library/postgres:16-alpine >/dev/null

for _ in $(seq 1 60); do
  if "${ENGINE}" exec fcs-postgres pg_isready -U "${PG_USER}" -d quarkus_test >/dev/null 2>&1; then break; fi
  sleep 1
done
"${ENGINE}" exec fcs-postgres pg_isready -U "${PG_USER}" -d quarkus_test >/dev/null

# ---------------------------------------------------------- observability ---
echo "[allen-key] Step 4/6 — observability (otel-collector, prometheus:${PORT_PROMETHEUS}, grafana:${PORT_GRAFANA})..."
"${ENGINE}" run -d --name fcs-otelcol --network "${NETWORK}" \
  -v "${REPO_ROOT}/observability/otel/otel-collector-config.yaml:/etc/otelcol/config.yaml:ro" \
  docker.io/otel/opentelemetry-collector-contrib:0.114.0 \
  --config=/etc/otelcol/config.yaml >/dev/null

"${ENGINE}" run -d --name fcs-prometheus --network "${NETWORK}" \
  -v "${RUNTIME_DIR}/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
  -v "${REPO_ROOT}/observability/prometheus/rules:/etc/prometheus/rules:ro" \
  -p "127.0.0.1:${PORT_PROMETHEUS}:9090" \
  docker.io/prom/prometheus:v2.55.1 \
  --config.file=/etc/prometheus/prometheus.yml --storage.tsdb.path=/prometheus \
  --web.enable-lifecycle >/dev/null

"${ENGINE}" run -d --name fcs-grafana --network "${NETWORK}" \
  -e GF_SECURITY_ADMIN_USER=admin -e GF_SECURITY_ADMIN_PASSWORD=admin \
  -e GF_AUTH_ANONYMOUS_ENABLED=true -e GF_AUTH_ANONYMOUS_ORG_ROLE=Viewer \
  -e GF_USERS_DEFAULT_THEME=dark \
  -e GF_SERVER_ROOT_URL="http://localhost:${PORT_GRAFANA}" \
  -v "${REPO_ROOT}/observability/grafana/provisioning:/etc/grafana/provisioning:ro" \
  -v "${REPO_ROOT}/observability/grafana/dashboards:/etc/grafana/dashboards:ro" \
  -p "127.0.0.1:${PORT_GRAFANA}:3000" \
  docker.io/grafana/grafana:11.3.1 >/dev/null

# ------------------------------------------------------------------- apps ---
echo "[allen-key] Step 5/6 — the two Quarkus variants..."
run_app() { # name image_or_module db_name host_port
  local common_env=(
    -e QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://fcs-postgres:5432/$3"
    -e QUARKUS_DATASOURCE_USERNAME="${PG_USER}"
    -e QUARKUS_DATASOURCE_PASSWORD="${PG_PASS}"
    -e QUARKUS_HTTP_PORT=8080
    -e QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT="http://fcs-otelcol:4317"
    -e OTEL_EXPORTER_OTLP_ENDPOINT="http://fcs-otelcol:4317"
  )
  if [[ "${MODE}" == "prod" ]]; then
    "${ENGINE}" run -d --name "$1" --network "${NETWORK}" \
      "${common_env[@]}" \
      -p "127.0.0.1:$4:8080" \
      "$2" >/dev/null
  else
    # quarkus:dev inside a Maven container: Dev UI only exists in dev mode.
    # Source is bind-mounted; a named volume keeps the Maven repo warm.
    "${ENGINE}" run -d --name "$1" --network "${NETWORK}" \
      "${common_env[@]}" \
      -v "${REPO_ROOT}:/workspace" \
      -v fcs-m2-dev:/root/.m2 \
      -w /workspace \
      -p "127.0.0.1:$4:8080" \
      docker.io/library/maven:3.9-eclipse-temurin-17 \
      mvn -q -B -pl "$2" -am quarkus:dev -DskipTests \
        -Dquarkus.http.host=0.0.0.0 -Dquarkus.analytics.disabled=true >/dev/null
  fi
}
if [[ "${MODE}" == "prod" ]]; then
  run_app fcs-app-architect fcs-architect:allen quarkus_architect "${PORT_ARCHITECT}"
  run_app fcs-app-senior    fcs-senior:allen    quarkus_senior    "${PORT_SENIOR}"
else
  run_app fcs-app-architect java-assignment-architect quarkus_architect "${PORT_ARCHITECT}"
  run_app fcs-app-senior    java-assignment-senior    quarkus_senior    "${PORT_SENIOR}"
fi

# dev mode compiles on first request path; cold ~/.m2 can take several minutes
WAIT_TRIES=60
[[ "${MODE}" == "dev" ]] && WAIT_TRIES=600
wait_http() { # host_port label
  for _ in $(seq 1 "${WAIT_TRIES}"); do
    if (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; then
      exec 3>&- 3<&- 2>/dev/null || true
      echo "[allen-key]   $2 is up on http://localhost:$1"
      return 0
    fi
    sleep 1
  done
  echo "[allen-key]   WARNING: $2 not reachable on :$1 after ${WAIT_TRIES}s (check logs below)" >&2
}
wait_http "${PORT_ARCHITECT}" "architect"
wait_http "${PORT_SENIOR}" "senior"

# ---------------------------------------------------------------- summary ---
UI_HINT="/q/swagger-ui (prod build; Dev UI needs --dev)"
[[ "${MODE}" == "dev" ]] && UI_HINT="/q/dev-ui and /q/swagger-ui (dev mode)"
cat <<SUMMARY

[allen-key] Step 6/6 — warehouse assembled. Furniture map (random ports, fresh each run):

  Architect API   http://localhost:${PORT_ARCHITECT}      (health: /q/health, metrics: /q/metrics, ui: ${UI_HINT})
  Senior API      http://localhost:${PORT_SENIOR}      (health: /q/health, metrics: /q/metrics, ui: ${UI_HINT})
  Grafana         http://localhost:${PORT_GRAFANA}      (admin/admin, dashboard auto-provisioned)
  Prometheus      http://localhost:${PORT_PROMETHEUS}
  PostgreSQL      localhost:${PORT_POSTGRES}      (${PG_USER}/${PG_PASS}, dbs: quarkus_architect, quarkus_senior)

  Streaming all container logs below (also saved to ${LOG_DIR}).
  Press Ctrl+C to stop and remove everything.

SUMMARY

# ------------------------------------------------------------------- logs ---
for c in "${CONTAINERS[@]}"; do
  ( "${ENGINE}" logs -f "${c}" 2>&1 | while IFS= read -r line; do
      printf '[%s] %s\n' "${c#fcs-}" "${line}"
    done | tee -a "${LOG_DIR}/${c}.log" ) &
  LOG_PIDS+=("$!")
done

wait
