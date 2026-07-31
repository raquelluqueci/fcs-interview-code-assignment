package com.fulfilment.application.monolith.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * EN: Domain-level OpenTelemetry metrics and span enrichment.
 *     Exported via OTLP (gRPC) to the collector, then scraped by Prometheus on :8889.
 * PT: Metricas OpenTelemetry de dominio e enriquecimento de spans.
 *     Exportadas via OTLP (gRPC) para o collector e depois scraped pelo Prometheus em :8889.
 *
 * @author tfantas — <a href="https://www.tfantas.io">www.tfantas.io</a> — Lisbon, July 2026
 */
@ApplicationScoped
public class FcsOtelMetrics {

    private static final AttributeKey<String> OP = AttributeKey.stringKey("fcs.operation");
    private static final AttributeKey<String> ROUTE = AttributeKey.stringKey("fcs.route");
    private static final AttributeKey<String> METHOD = AttributeKey.stringKey("http.method");
    private static final AttributeKey<Long> STATUS = AttributeKey.longKey("http.status_code");

    private final LongCounter domainOperations;
    private final LongCounter httpRequests;

    /**
     * EN: Builds OTEL meters bound to the global OpenTelemetry instance injected by Quarkus.
     * PT: Constroi meters OTEL ligados a instancia OpenTelemetry global injectada pelo Quarkus.
     */
    @Inject
    public FcsOtelMetrics(OpenTelemetry openTelemetry) {
        Meter meter = openTelemetry.getMeter("fcs-fulfilment");
        this.domainOperations = meter
                .counterBuilder("fcs.domain.operations")
                .setDescription("Domain operations performed by the fulfilment service")
                .setUnit("1")
                .build();
        this.httpRequests = meter
                .counterBuilder("fcs.http.server.requests")
                .setDescription("HTTP requests observed via OpenTelemetry")
                .setUnit("1")
                .build();
    }

    /**
     * EN: Increments the coarse domain counter (warehouse|store|product|fulfilment)
     *     and attaches the same label to the current span when one is active.
     * PT: Incrementa o contador de dominio (warehouse|store|product|fulfilment)
     *     e grava o mesmo label no span actual, se existir.
     *
     * @param operation domain bucket name / nome do bucket de dominio
     */
    public void recordDomainOperation(String operation) {
        domainOperations.add(1, Attributes.of(OP, operation));
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            span.setAttribute(OP, operation);
        }
    }

    /**
     * EN: Records one HTTP observation (method, low-cardinality route, status).
     *     Marks the active span as ERROR when status &gt;= 500.
     * PT: Regista uma observacao HTTP (metodo, rota de baixa cardinalidade, status).
     *     Marca o span activo como ERROR quando status &gt;= 500.
     */
    public void recordHttp(String method, String route, int status) {
        httpRequests.add(1, Attributes.of(
                METHOD, method == null ? "UNKNOWN" : method,
                ROUTE, route == null ? "unknown" : route,
                STATUS, (long) status));
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            span.setAttribute(METHOD, method == null ? "UNKNOWN" : method);
            span.setAttribute(ROUTE, route == null ? "unknown" : route);
            span.setAttribute(STATUS, (long) status);
            if (status >= 500) {
                span.setStatus(StatusCode.ERROR);
            }
        }
    }
}
