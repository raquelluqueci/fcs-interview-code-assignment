# FCS warehouse fulfilment — local dev targets.
# Author: tfantas — https://www.tfantas.io — Lisbon, July 2026
SHELL := /bin/bash
.DEFAULT_GOAL := help

MODULE ?= java-assignment-senior
COMPOSE := docker compose -f docker-compose.observability.yml
MAVEN := mvn -s settings-central.xml

.PHONY: help obs-up obs-down obs-ps test-senior test-architect test-all \
	run-senior run-architect run-both metrics-smoke

help:
	@echo "Targets:"
	@echo "  obs-up / obs-down     Local observability stack (compose)"
	@echo "  run-senior|architect  quarkus:dev with Dev Services"
	@echo "  run-both              both modules + observability via allen-key.sh --dev"
	@echo "  metrics-smoke         probe /q/health and /q/metrics on :8080"
	@echo "  test-senior|architect|all"

obs-up:
	./scripts/observability-up.sh

obs-down:
	./scripts/observability-down.sh

obs-ps:
	$(COMPOSE) ps

test-senior:
	$(MAVEN) -f java-assignment-senior/pom.xml clean test

test-architect:
	$(MAVEN) -f java-assignment-architect/pom.xml clean test

test-all:
	$(MAVEN) -f pom.xml clean verify

# Send application telemetry to the local collector.
OTEL_RUN := env \
	OTEL_EXPORTER_OTLP_ENDPOINT=http://127.0.0.1:4317 \
	OTEL_EXPORTER_OTLP_PROTOCOL=grpc \
	OTEL_METRICS_EXPORTER=otlp \
	OTEL_TRACES_EXPORTER=otlp \
	OTEL_LOGS_EXPORTER=none \
	-u OTEL_RESOURCE_ATTRIBUTES

run-senior:
	set -a; [ -f .env ] && . ./.env; set +a; \
	QUARKUS_HTTP_PORT=$${SENIOR_PORT:-8083} $(OTEL_RUN) $(MAVEN) \
		-f java-assignment-senior/pom.xml quarkus:dev

run-architect:
	set -a; [ -f .env ] && . ./.env; set +a; \
	QUARKUS_HTTP_PORT=$${ARCHITECT_PORT:-8082} $(OTEL_RUN) $(MAVEN) \
		-f java-assignment-architect/pom.xml quarkus:dev

run-both:
	./allen-key.sh --dev

metrics-smoke:
	@wget -qO- http://127.0.0.1:8080/q/health || true; echo
	@wget -qO- http://127.0.0.1:8080/q/metrics 2>/dev/null | head -n 40 || true
