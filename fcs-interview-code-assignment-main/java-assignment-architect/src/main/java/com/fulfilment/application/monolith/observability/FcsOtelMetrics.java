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
 * Domain-level OpenTelemetry metrics + span enrichment.
 * Exported via OTLP to the local collector (otelcol → Prometheus :8889).
 */
@ApplicationScoped
public class FcsOtelMetrics {

    private static final AttributeKey<String> OP = AttributeKey.stringKey("fcs.operation");
    private static final AttributeKey<String> ROUTE = AttributeKey.stringKey("fcs.route");
    private static final AttributeKey<String> METHOD = AttributeKey.stringKey("http.method");
    private static final AttributeKey<Long> STATUS = AttributeKey.longKey("http.status_code");

    private final LongCounter domainOperations;
    private final LongCounter httpRequests;

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

    public void recordDomainOperation(String operation) {
        domainOperations.add(1, Attributes.of(OP, operation));
        Span span = Span.current();
        if (span.getSpanContext().isValid()) {
            span.setAttribute(OP, operation);
        }
    }

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
