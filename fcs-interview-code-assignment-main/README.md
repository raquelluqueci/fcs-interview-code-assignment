# FCS Interview — Warehouse Fulfilment Assignment

Quarkus 3.13 fulfilment case study with **OpenTelemetry**, **Prometheus** and **Grafana** wired for local demo (and recruiter compose).

Portuguese: [README-br.md](README-br.md)

## Modules

| Module | Role |
|--------|------|
| [`java-assignment`](java-assignment/) | Untouched baseline (not in Maven reactor) |
| [`java-assignment-senior`](java-assignment-senior/) | Pragmatic senior completion |
| [`java-assignment-architect`](java-assignment-architect/) | Architecture-first completion |

Parent reactor: [`pom.xml`](pom.xml) builds **senior + architect** only.

Case study brief: [`case-study/`](case-study/).

## Observability architecture

```
                    ┌──────────────────────────────┐
  HTTP API :8080 ──►│  Quarkus (senior|architect)  │
                    │  • REST auto-spans (OTEL)    │
                    │  • FcsOtelMetrics (domain)   │
                    │  • Micrometer HTTP/JVM (SLI) │
                    └───────────┬──────────────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          ▼                     ▼                     ▼
   OTLP gRPC :4317        GET /q/metrics         GET /q/health
   otelcol-contrib        job fcs-fulfilment      SmallRye
          │
          ├─ metrics ──► Prometheus job otelcol (:8889)
          └─ traces  ──► debug exporter only (no Tempo/Jaeger)
                                │
                     Prometheus :9090  (/prometheus on Odin gateway)
                                ▼
                     Grafana :3001     (/grafana)
                     uid: fcs-fulfilment-obs  (panels = Micrometer job)
```

| Signal | How | Where to look | Honest scope |
|--------|-----|---------------|--------------|
| **RED + JVM (primary SLI)** | Micrometer `/q/metrics` | Prometheus job `fcs-fulfilment` | Dashboard panels |
| **OTEL domain counters** | OTLP **gRPC :4317** → otelcol | job `otelcol` — `fcs_http_server_requests_total{service_name=~"warehouse-fulfilment.*"}` | Demo/experimental; not all Grafana panels |
| **Traces** | REST auto-instrumentation + filter attributes | logs `traceId=` + otelcol **debug** | **No** Tempo/Jaeger UI in this submission |
| **Health** | `/q/health` | curl | — |

All configs live under [`observability/`](observability/) so the submission is self-contained.

## Prerequisites

- **JDK 17** (`JAVA_HOME` pointing at JDK 17 — Quarkus 3.13)
- Maven 3.9+ (or use the module `mvnw` if wrapper jars are present)
- PostgreSQL (Dev Services if Docker is available; otherwise local Postgres)
- Optional local stack (Odin): Prometheus, Grafana, otelcol-contrib already running

**Important:** Claude/Codex shells often export OTEL_*. The Makefile **sets** the correct values for this app (`OTEL_EXPORTER_OTLP_ENDPOINT=http://127.0.0.1:4317`, `PROTOCOL=grpc`) so host otelcol receives traces/metrics. `okhttp` is a runtime dependency for the gRPC OTLP sender.

## Quick start (local Odin stack)

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# One-time: inject scrape job + Grafana dashboard into native stack
make odin-install

# Run senior (or architect) — unsets hostile OTEL_* from the shell
make run-senior
# make run-architect
```

App: `http://127.0.0.1:8080`  
Health: `http://127.0.0.1:8080/q/health`  
Metrics (Micrometer): `http://127.0.0.1:8080/q/metrics`

### Prometheus — queries that show data

Open the Graph UI (not the empty “No data queried yet” placeholder):

- UI: [http://localhost/prometheus/graph](http://localhost/prometheus/graph)
- Targets: [http://localhost/prometheus/targets](http://localhost/prometheus/targets) → job **`fcs-fulfilment`** must be **UP**

Paste any of these expressions:

```promql
# Target alive
up{job="fcs-fulfilment"}

# Request rate (Micrometer)
sum(rate(http_server_requests_seconds_count{job="fcs-fulfilment"}[1m]))

# By URI
sum by (uri, status) (rate(http_server_requests_seconds_count{job="fcs-fulfilment"}[5m]))

# p95 latency
histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="fcs-fulfilment"}[5m])))

# OpenTelemetry HTTP counters (via otelcol :8889)
fcs_http_server_requests_total{service_name=~"warehouse-fulfilment.*"}

sum by (fcs_route) (rate(fcs_http_server_requests_total{service_name=~"warehouse-fulfilment.*"}[1m]))
```

Deep-link with query pre-filled:  
[http://localhost/prometheus/graph?g0.expr=up%7Bjob%3D%22fcs-fulfilment%22%7D&g0.tab=0](http://localhost/prometheus/graph?g0.expr=up%7Bjob%3D%22fcs-fulfilment%22%7D&g0.tab=0)

Direct API checks:

```bash
wget -qO- 'http://127.0.0.1:9090/prometheus/api/v1/query?query=up{job="fcs-fulfilment"}'
wget -qO- http://127.0.0.1:8889/metrics | grep fcs_
```

### Grafana

Dashboard: [http://localhost/grafana/d/fcs-fulfilment-obs/](http://localhost/grafana/d/fcs-fulfilment-obs/)  
JSON source: [`observability/grafana/dashboards/fcs-fulfilment-obs.json`](observability/grafana/dashboards/fcs-fulfilment-obs.json)

## Recruiter path (Docker Compose)

When host ports `9090/3001/4317/4318/8889/15432` are free:

```bash
./scripts/observability-up.sh   # fail-closed if ports busy
make run-senior                 # app on host :8080
# Grafana http://localhost:3001  Prometheus http://localhost:9090
./scripts/observability-down.sh
```

Compose file: [`docker-compose.observability.yml`](docker-compose.observability.yml).

## Repository layout (observability)

```
observability/
  otel/otel-collector-config.yaml     # recruiter collector
  prometheus/
    scrape-fcs-fulfilment.yml          # host scrape fragment
    prometheus.yml                    # compose full config
    rules/fcs-recording-rules.yml
    file_sd/fcs-targets.json
  grafana/
    dashboards/fcs-fulfilment-obs.json
    provisioning/...
  nginx/                              # optional gateway snippets
scripts/
  install-local-odin.sh               # wire host Prometheus + Grafana
  observability-up.sh / -down.sh
```

## Build & test

```bash
export JAVA_HOME=…/openjdk@17…
mvn -s java-assignment-senior/mvn-settings-local.xml -f java-assignment-senior/pom.xml test
mvn -s java-assignment-senior/mvn-settings-local.xml -f java-assignment-architect/pom.xml test
```

Integration tests that need Dev Services require Docker. Unit tests (use cases, location) do not.

## OpenTelemetry implementation notes

| Piece | File |
|-------|------|
| Extension | `quarkus-opentelemetry` in both module POMs |
| OTLP export | `application.properties` → `http://127.0.0.1:4317` **gRPC** (okhttp on classpath) |
| Domain metrics | `…/observability/FcsOtelMetrics.java` |
| HTTP filter | `…/observability/FcsOtelHttpFilter.java` |
| Histogram buckets (Micrometer p95) | `…/observability/HttpServerHistogramFilter.java` |

Tests force `%test.quarkus.otel.sdk.disabled=true` so Surefire does not depend on a collector.

## License

See [LICENSE](LICENSE).
