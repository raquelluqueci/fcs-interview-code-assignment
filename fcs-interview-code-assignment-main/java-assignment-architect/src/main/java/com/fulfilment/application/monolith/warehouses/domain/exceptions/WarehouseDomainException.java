package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Base type for every invariant violation raised by the Warehouse domain (use cases and domain
 * validators). It purposely carries no dependency on JAX-RS: the translation to an HTTP response
 * is the sole responsibility of a dedicated {@code ExceptionMapper} in the REST adapter layer.
 */
public abstract class WarehouseDomainException extends RuntimeException {

  private final int httpStatusCode;

  protected WarehouseDomainException(String message, int httpStatusCode) {
    super(message);
    this.httpStatusCode = httpStatusCode;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }
}
