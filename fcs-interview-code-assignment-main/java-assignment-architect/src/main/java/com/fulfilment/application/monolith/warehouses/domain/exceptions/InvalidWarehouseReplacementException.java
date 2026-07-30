package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when a warehouse replacement request violates business-unit-code, capacity or stock invariants. */
public class InvalidWarehouseReplacementException extends WarehouseDomainException {

  public InvalidWarehouseReplacementException(String message) {
    super(message, 400);
  }
}
