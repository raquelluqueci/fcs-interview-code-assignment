package com.fulfilment.application.monolith.warehouses.domain.validation;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.DuplicateBusinessUnitCodeException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidLocationException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidWarehouseCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Domain service enforcing the invariants shared by warehouse creation and replacement. It is
 * deliberately kept as a stateless collaborator of the use cases (not a REST/adapter concern),
 * relying exclusively on the {@link LocationResolver} and {@link WarehouseStore} ports.
 */
@ApplicationScoped
public class WarehouseValidator {

  private final LocationResolver locationResolver;
  private final WarehouseStore warehouseStore;

  public WarehouseValidator(LocationResolver locationResolver, WarehouseStore warehouseStore) {
    this.locationResolver = locationResolver;
    this.warehouseStore = warehouseStore;
  }

  /** Ensures the location identifier resolves to a known, existing {@link Location}. */
  public Location ensureValidLocation(String locationIdentifier) {
    Location location = locationResolver.resolveByIdentifier(locationIdentifier);
    if (location == null) {
      throw new InvalidLocationException(locationIdentifier);
    }
    return location;
  }

  /**
   * Ensures no other *active* (non-archived) warehouse is already registered under the given
   * business unit code.
   */
  public void ensureBusinessUnitCodeIsUnique(String businessUnitCode) {
    Warehouse existing = warehouseStore.findByBusinessUnitCode(businessUnitCode);
    if (existing != null && existing.archivedAt == null) {
      throw new DuplicateBusinessUnitCodeException(businessUnitCode);
    }
  }

  /**
   * Ensures the location hasn't already reached its maximum number of active warehouses. When
   * {@code excludingBusinessUnitCode} is provided (replacement flow), the warehouse being
   * replaced is excluded from the count since it will be archived as part of the same operation.
   */
  public void ensureLocationHasRoomForNewWarehouse(
      Location location, String excludingBusinessUnitCode) {
    long activeWarehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(warehouse -> warehouse.archivedAt == null)
            .filter(warehouse -> location.identification.equals(warehouse.location))
            .filter(
                warehouse ->
                    excludingBusinessUnitCode == null
                        || !excludingBusinessUnitCode.equals(warehouse.businessUnitCode))
            .count();

    if (activeWarehousesAtLocation >= location.maxNumberOfWarehouses) {
      throw new LocationCapacityExceededException(
          location.identification, location.maxNumberOfWarehouses);
    }
  }

  /**
   * Ensures the total capacity of active warehouses at the location - summing the new/updated
   * warehouse capacity - doesn't exceed the location's maximum capacity. When {@code
   * excludingBusinessUnitCode} is provided (replacement flow), the warehouse being replaced is
   * excluded from the current total.
   */
  public void ensureLocationCapacityIsRespected(
      Location location, int newWarehouseCapacity, String excludingBusinessUnitCode) {
    int currentCapacityAtLocation =
        warehouseStore.getAll().stream()
            .filter(warehouse -> warehouse.archivedAt == null)
            .filter(warehouse -> location.identification.equals(warehouse.location))
            .filter(
                warehouse ->
                    excludingBusinessUnitCode == null
                        || !excludingBusinessUnitCode.equals(warehouse.businessUnitCode))
            .mapToInt(warehouse -> warehouse.capacity)
            .sum();

    if (currentCapacityAtLocation + newWarehouseCapacity > location.maxCapacity) {
      throw new InvalidWarehouseCapacityException(
          "Capacity of "
              + newWarehouseCapacity
              + " for location '"
              + location.identification
              + "' would exceed its maximum capacity of "
              + location.maxCapacity
              + " (already committed: "
              + currentCapacityAtLocation
              + ")");
    }
  }

  /** Ensures the warehouse capacity can hold its own informed stock. */
  public void ensureCapacityCoversStock(int capacity, int stock) {
    if (capacity < stock) {
      throw new InvalidWarehouseCapacityException(
          "Capacity of " + capacity + " cannot hold the informed stock of " + stock);
    }
  }
}
