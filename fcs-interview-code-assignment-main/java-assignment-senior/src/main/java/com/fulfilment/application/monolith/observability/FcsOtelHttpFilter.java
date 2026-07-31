package com.fulfilment.application.monolith.observability;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Emits OpenTelemetry domain HTTP counters for every JAX-RS response.
 */
@Provider
public class FcsOtelHttpFilter implements ContainerResponseFilter {

    @Inject
    FcsOtelMetrics metrics;

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        String method = request.getMethod();
        // Prefer matched JAX-RS template (low cardinality) over raw getPath() with IDs
        String route = templateRoute(request);
        metrics.recordHttp(method, route, response.getStatus());

        String lower = route.toLowerCase();
        if (lower.startsWith("warehouse") || lower.startsWith("/warehouse")) {
            metrics.recordDomainOperation("warehouse");
        } else if (lower.startsWith("store") || lower.startsWith("/store")) {
            metrics.recordDomainOperation("store");
        } else if (lower.startsWith("product") || lower.startsWith("/product")) {
            metrics.recordDomainOperation("product");
        } else if (lower.startsWith("fulfilment") || lower.startsWith("/fulfilment")) {
            metrics.recordDomainOperation("fulfilment");
        }
    }

    private static String templateRoute(ContainerRequestContext request) {
        if (request.getUriInfo() == null) {
            return "unknown";
        }
        var matched = request.getUriInfo().getMatchedURIs();
        if (matched != null && !matched.isEmpty()) {
            // First matched URI is the most specific template path when available
            String m = matched.get(0);
            if (m != null && !m.isBlank()) {
                return m.startsWith("/") ? m : "/" + m;
            }
        }
        String path = request.getUriInfo().getPath();
        if (path == null || path.isBlank()) {
            return "root";
        }
        // Strip numeric path segments to avoid high-cardinality labels
        return path.replaceAll("/\\d+", "/{id}");
    }
}
