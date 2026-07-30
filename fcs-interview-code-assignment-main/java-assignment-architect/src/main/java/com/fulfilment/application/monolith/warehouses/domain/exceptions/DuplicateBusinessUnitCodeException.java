package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when a warehouse is created with a business unit code already in use by an active warehouse. */
public class DuplicateBusinessUnitCodeException extends WarehouseDomainException {

  public DuplicateBusinessUnitCodeException(String businessUnitCode) {
    super("A warehouse with business unit code '" + businessUnitCode + "' already exists", 400);
  }
}
