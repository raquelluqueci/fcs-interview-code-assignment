# FCS Interview — Assignment Warehouse Fulfilment

Case study Quarkus 3.13 com **OpenTelemetry**, **Prometheus** e **Grafana** preparados para desenvolvimento local.

English: [README.md](README.md)

## Módulos

| Módulo | Papel |
|--------|--------|
| [`java-assignment`](java-assignment/) | Baseline intocado (fora do reactor Maven) |
| [`java-assignment-senior`](java-assignment-senior/) | Variante pragmática (sénior) |
| [`java-assignment-architect`](java-assignment-architect/) | Variante architecture-first |

Reactor pai: [`pom.xml`](pom.xml) — só **senior + architect**.

Briefing: [`case-study/`](case-study/).

## Arquitectura de observabilidade

```
                    ┌──────────────────────────────┐
  HTTP API :8080 ──►│  Quarkus (senior|architect)  │
                    │  • Spans REST (OTEL)         │
                    │  • FcsOtelMetrics (domínio)  │
                    │  • Micrometer HTTP/JVM (SLI) │
                    └───────────┬──────────────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          ▼                     ▼                     ▼
   OTLP gRPC :4317        GET /q/metrics         GET /q/health
   otelcol-contrib        job fcs-fulfilment      SmallRye
          │
          ├─ metrics ──► job otelcol (:8889)
          └─ traces  ──► exporter debug only (sem Tempo/Jaeger)
                                │
                     Prometheus :9090
                                ▼
                     Grafana :3001     (painéis = Micrometer)
```

| Sinal | Como | Onde ver | Escopo honesto |
|-------|------|----------|----------------|
| **RED + JVM (SLI principal)** | Micrometer `/q/metrics` | job `fcs-fulfilment` | Painéis Grafana |
| **Contadores OTEL** | OTLP **gRPC :4317** → otelcol | job `otelcol` — `fcs_http_server_requests_total{service_name=~"warehouse-fulfilment.*"}` | Demo; não todos os painéis |
| **Traces** | Auto-instrumentação + atributos no filter | logs `traceId=` + otelcol **debug** | **Sem** UI Tempo/Jaeger nesta entrega |
| **Health** | `/q/health` | curl | — |

Configs versionadas em [`observability/`](observability/) — o zip de submissão é auto-contido.

## Pré-requisitos

- **JDK 17**
- Maven 3.9+
- PostgreSQL (Dev Services com Docker, ou Postgres local)
- Docker ou Podman para a stack local autocontida e os Dev Services

O Makefile define o endpoint e o protocolo OTLP necessários pelo collector local. A dependência `okhttp` no POM é necessária para o sender OTLP gRPC.

## Arranque rápido

```bash
export JAVA_HOME=/caminho/para/jdk-17
./allen-key.sh --dev
```

O script imprime portas aleatórias em loopback para os dois módulos. Cada módulo expõe `/q/swagger-ui`, `/q/dev-ui`, `/q/health` e `/q/metrics`.

### Prometheus — como ver dados (não é a página em branco)

A aba Graph começa com **“No data queried yet”** até colares uma query. Isso é normal.

- Graph: [http://localhost:9090/graph](http://localhost:9090/graph)
- Targets: [http://localhost:9090/targets](http://localhost:9090/targets) → **`fcs-fulfilment` = UP**

Queries:

```promql
up{job="fcs-fulfilment"}

sum(rate(http_server_requests_seconds_count{job="fcs-fulfilment"}[1m]))

sum by (uri, status) (rate(http_server_requests_seconds_count{job="fcs-fulfilment"}[5m]))

histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{job="fcs-fulfilment"}[5m])))

fcs_http_server_requests_total{service_name=~"warehouse-fulfilment.*"}

sum by (fcs_route) (rate(fcs_http_server_requests_total{service_name=~"warehouse-fulfilment.*"}[1m]))
```

Link com query pronta:  
[http://localhost:9090/graph?g0.expr=up%7Bjob%3D%22fcs-fulfilment%22%7D&g0.tab=0](http://localhost:9090/graph?g0.expr=up%7Bjob%3D%22fcs-fulfilment%22%7D&g0.tab=0)

```bash
wget -qO- 'http://127.0.0.1:9090/api/v1/query?query=up{job="fcs-fulfilment"}'
wget -qO- http://127.0.0.1:8889/metrics | grep fcs_
```

### Grafana

Dashboard: [http://localhost:3001/d/fcs-fulfilment-obs/](http://localhost:3001/d/fcs-fulfilment-obs/)
JSON: [`observability/grafana/dashboards/fcs-fulfilment-obs.json`](observability/grafana/dashboards/fcs-fulfilment-obs.json)

## Stack de observabilidade com Docker Compose

Com portas `9090/3001/4317/4318/8889/15432` livres:

```bash
cp .env.example .env
./scripts/observability-up.sh
make run-senior    # senior :8083; architect usa :8082
./scripts/observability-down.sh
```

Ficheiro: [`docker-compose.observability.yml`](docker-compose.observability.yml).

## Layout `observability/`

```
observability/
  otel/          # collector local
  prometheus/    # scrape, rules, config compose
  grafana/       # dashboard + provisioning
scripts/
  observability-up.sh / -down.sh
```

## Build e testes

```bash
export JAVA_HOME=/caminho/para/jdk-17
mvn -s settings-central.xml -f pom.xml clean verify
mvn -s settings-central.xml -f java-assignment-senior/pom.xml test
mvn -s settings-central.xml -f java-assignment-architect/pom.xml test
```

Testes de integração com Dev Services precisam de Docker.

## OpenTelemetry — o que foi implementado

| Peça | Ficheiro |
|------|----------|
| Extensão | `quarkus-opentelemetry` nos dois POMs |
| Export OTLP | `application.properties` → `http://127.0.0.1:4317` **gRPC** (okhttp no classpath) |
| Métricas de domínio | `…/observability/FcsOtelMetrics.java` |
| Filter HTTP | `…/observability/FcsOtelHttpFilter.java` |
| Histograms Micrometer (p95) | `…/observability/HttpServerHistogramFilter.java` |

Em testes: `%test.quarkus.otel.sdk.disabled=true`.

## Licença

Ver [LICENSE](LICENSE).
