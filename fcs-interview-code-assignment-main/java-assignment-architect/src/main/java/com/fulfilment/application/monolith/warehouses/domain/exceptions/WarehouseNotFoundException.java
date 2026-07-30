package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when a warehouse cannot be located by its business unit code or identifier. */
public class WarehouseNotFoundException extends WarehouseDomainException {

  public WarehouseNotFoundException(String identifier) {
    super("Warehouse not found for identifier: " + identifier, 404);
  }
}
