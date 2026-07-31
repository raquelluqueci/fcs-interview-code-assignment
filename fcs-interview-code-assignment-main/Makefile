SHELL := /bin/bash
.DEFAULT_GOAL := help

MODULE ?= java-assignment-senior
COMPOSE := docker compose -f docker-compose.observability.yml

.PHONY: help obs-up obs-down obs-ps test-senior test-architect test-all \
	run-senior run-architect odin-install odin-uninstall metrics-smoke

help:
	@echo "Targets:"
	@echo "  obs-up / obs-down     Recruiter observability stack (compose)"
	@echo "  run-senior|architect  quarkus:dev with Dev Services"
	@echo "  odin-install          Wire scrape + dashboard into local Odin stack"
	@echo "  metrics-smoke         probe /q/health and /q/metrics on :8080"
	@echo "  test-senior|architect|all"

obs-up:
	./scripts/observability-up.sh

obs-down:
	./scripts/observability-down.sh

obs-ps:
	$(COMPOSE) ps

test-senior:
	./java-assignment-senior/mvnw -f java-assignment-senior/pom.xml clean test

test-architect:
	./java-assignment-architect/mvnw -f java-assignment-architect/pom.xml clean test

test-all:
	mvn -f pom.xml clean test

# Override Claude/Codex shell OTEL_* with gRPC → local otelcol :4317
OTEL_RUN := env \
	OTEL_EXPORTER_OTLP_ENDPOINT=http://127.0.0.1:4317 \
	OTEL_EXPORTER_OTLP_PROTOCOL=grpc \
	OTEL_METRICS_EXPORTER=otlp \
	OTEL_TRACES_EXPORTER=otlp \
	OTEL_LOGS_EXPORTER=none \
	-u OTEL_RESOURCE_ATTRIBUTES \
	-u CLAUDE_CODE_ENABLE_TELEMETRY \
	-u OTEL_METRICS_INCLUDE_VERSION \
	-u OTEL_LOG_TOOL_DETAILS

run-senior:
	set -a; [ -f .env ] && . ./.env; set +a; \
	$(OTEL_RUN) mvn -s java-assignment-senior/mvn-settings-local.xml \
		-f java-assignment-senior/pom.xml quarkus:dev

run-architect:
	set -a; [ -f .env ] && . ./.env; set +a; \
	$(OTEL_RUN) mvn -s java-assignment-architect/mvn-settings-local.xml \
		-f java-assignment-architect/pom.xml quarkus:dev

odin-install:
	./scripts/install-local-odin.sh install

odin-uninstall:
	./scripts/install-local-odin.sh uninstall

metrics-smoke:
	@wget -qO- http://127.0.0.1:8080/q/health || true; echo
	@wget -qO- http://127.0.0.1:8080/q/metrics 2>/dev/null | head -n 40 || true
