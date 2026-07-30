package com.fulfilment.application.monolith.fulfilment.domain.exceptions;

/**
 * Base type for every invariant violation raised by the Fulfilment domain. Mirrors {@code
 * WarehouseDomainException}: no JAX-RS dependency here, translation to HTTP is the
 * responsibility of a dedicated {@code ExceptionMapper} in the REST adapter layer.
 */
public abstract class FulfilmentDomainException extends RuntimeException {

  private final int httpStatusCode;

  protected FulfilmentDomainException(String message, int httpStatusCode) {
    super(message);
    this.httpStatusCode = httpStatusCode;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }
}
