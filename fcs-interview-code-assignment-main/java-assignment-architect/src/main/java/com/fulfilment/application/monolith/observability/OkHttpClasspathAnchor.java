package com.fulfilment.application.monolith.observability;

/**
 * Prevents Quarkus packaging from pruning okhttp (required at runtime by
 * OpenTelemetry OTLP gRPC sender SPI even when no app code calls OkHttp).
 */
final class OkHttpClasspathAnchor {
    static final Class<?> ANCHOR = okhttp3.OkHttpClient.class;

    private OkHttpClasspathAnchor() {
    }
}
