package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when a location has already reached its maximum number of active warehouses. */
public class LocationCapacityExceededException extends WarehouseDomainException {

  public LocationCapacityExceededException(String locationIdentifier, int maxNumberOfWarehouses) {
    super(
        "Location '"
            + locationIdentifier
            + "' has already reached its maximum number of warehouses ("
            + maxNumberOfWarehouses
            + ")",
        400);
  }
}
