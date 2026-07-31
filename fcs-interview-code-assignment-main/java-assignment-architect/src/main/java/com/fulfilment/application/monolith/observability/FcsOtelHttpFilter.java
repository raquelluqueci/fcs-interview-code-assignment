package com.fulfilment.application.monolith.observability;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * EN: JAX-RS response filter that emits OpenTelemetry counters for every request.
 * PT: Filtro JAX-RS de resposta que emite contadores OpenTelemetry em cada pedido.
 *
 * @author tfantas — <a href="https://www.tfantas.io">www.tfantas.io</a> — Lisbon, July 2026
 */
@Provider
public class FcsOtelHttpFilter implements ContainerResponseFilter {

    @Inject
    FcsOtelMetrics metrics;

    /**
     * EN: After the response is built, record HTTP + coarse domain metrics.
     * PT: Depois da resposta construida, regista metricas HTTP e de dominio.
     */
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        String method = request.getMethod();
        String route = normalizedRoute(request);
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

    /**
     * EN: Prefer matched JAX-RS templates; fall back to path with IDs stripped.
     *     Keeps Prometheus/OTEL label cardinality under control.
     * PT: Prefere templates JAX-RS matched; senao usa o path sem IDs.
     *     Controla a cardinalidade das labels no Prometheus/OTEL.
     */
    static String normalizedRoute(ContainerRequestContext request) {
        if (request.getUriInfo() == null) {
            return "unknown";
        }
        var matched = request.getUriInfo().getMatchedURIs();
        if (matched != null && !matched.isEmpty()) {
            String m = matched.get(0);
            if (m != null && !m.isBlank()) {
                return resourceRoute(m);
            }
        }
        String path = request.getUriInfo().getPath();
        if (path == null || path.isBlank()) {
            return "root";
        }
        return resourceRoute(path);
    }

    /**
     * EN: Collapse any path to the first resource segment only (e.g. /warehouse/{id} -&gt; /warehouse).
     * PT: Reduz o path ao primeiro segmento de recurso (ex.: /warehouse/{id} -&gt; /warehouse).
     */
    private static String resourceRoute(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        String[] segments = normalized.split("/");
        if (segments.length < 2 || segments[1].isBlank()) {
            return "/";
        }
        // EN: Entity IDs / business-unit codes must never become metric labels.
        // PT: IDs de entidade / business-unit codes nunca podem ser labels de metrica.
        return "/" + segments[1].toLowerCase();
    }
}
