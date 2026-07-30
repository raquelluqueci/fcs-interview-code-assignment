package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when a warehouse capacity violates the location's total capacity or its own stock. */
public class InvalidWarehouseCapacityException extends WarehouseDomainException {

  public InvalidWarehouseCapacityException(String message) {
    super(message, 400);
  }
}
