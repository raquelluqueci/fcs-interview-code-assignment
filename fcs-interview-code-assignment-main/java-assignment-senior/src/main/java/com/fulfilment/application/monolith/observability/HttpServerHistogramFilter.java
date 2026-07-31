package com.fulfilment.application.monolith.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * Enables Prometheus histogram buckets for HTTP server timers so Grafana can
 * compute p50/p95/p99 via histogram_quantile (Quarkus 3.13 has no property for this).
 */
@Singleton
public class HttpServerHistogramFilter {

    @Produces
    @Singleton
    public MeterFilter enableHttpServerHistograms() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
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
