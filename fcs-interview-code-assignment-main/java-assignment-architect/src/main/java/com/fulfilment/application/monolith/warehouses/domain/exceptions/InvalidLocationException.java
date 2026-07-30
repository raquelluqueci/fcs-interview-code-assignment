package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when a warehouse references a location identifier that doesn't resolve to a known location. */
public class InvalidLocationException extends WarehouseDomainException {

  public InvalidLocationException(String locationIdentifier) {
    super("Location does not exist: " + locationIdentifier, 400);
  }
}
