package com.fulfilment.application.monolith.fulfilment.domain.exceptions;

/**
 * Raised whenever a fulfilment association would violate one of the aggregate's quota
 * invariants (max warehouses per product+store, max warehouses per store, max products per
 * warehouse) or would duplicate an already existing association.
 */
public class FulfilmentConstraintViolationException extends FulfilmentDomainException {

  public FulfilmentConstraintViolationException(String message) {
    super(message, 400);
  }
}
