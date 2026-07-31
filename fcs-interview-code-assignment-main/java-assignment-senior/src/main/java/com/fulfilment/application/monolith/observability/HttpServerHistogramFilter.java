package com.fulfilment.application.monolith.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * EN: Enables Prometheus histogram buckets on HTTP server timers so Grafana can
 *     compute p50/p95/p99 via histogram_quantile. Quarkus 3.13 has no first-class
 *     property for this.
 * PT: Activa buckets de histograma Prometheus nos timers HTTP para o Grafana
 *     calcular p50/p95/p99 com histogram_quantile. O Quarkus 3.13 nao expoe uma
 *     property nativa para isto.
 */
@Singleton
public class HttpServerHistogramFilter {

    /**
     * EN: CDI producer — Micrometer applies this filter to every registered meter.
     * PT: Producer CDI — o Micrometer aplica este filtro a todos os meters registados.
     */
    @Produces
    @Singleton
    public MeterFilter enableHttpServerHistograms() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                // EN: Only HTTP server request timers need full histograms.
                // PT: So os timers HTTP de servidor precisam de histogramas completos.
                if (id.getName() != null && id.getName().startsWith("http.server.requests")) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }
}
