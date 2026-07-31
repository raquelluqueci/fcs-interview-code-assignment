package com.fulfilment.application.monolith.observability;

/**
 * EN: Prevents Quarkus packaging from pruning okhttp. The OpenTelemetry OTLP gRPC
 *     sender SPI loads OkHttp at runtime even when application code never
 *     references it directly.
 * PT: Impede o packaging do Quarkus de remover o okhttp. O sender OTLP gRPC do
 *     OpenTelemetry carrega OkHttp em runtime mesmo sem referencia directa no
 *     codigo da aplicacao.
 *
 * @author tfantas — <a href="https://www.tfantas.io">www.tfantas.io</a> — Lisbon, July 2026
 */
final class OkHttpClasspathAnchor {
    static final Class<?> ANCHOR = okhttp3.OkHttpClient.class;

    private OkHttpClasspathAnchor() {
    }
}
