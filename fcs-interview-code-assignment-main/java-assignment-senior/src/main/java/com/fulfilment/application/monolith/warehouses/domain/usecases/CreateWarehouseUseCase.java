package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new WebApplicationException("Warehouse business unit code is required.", 400);
    }

    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException(
          "A warehouse with business unit code " + warehouse.businessUnitCode + " already exists.", 400);
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WebApplicationException("Location " + warehouse.location + " is not valid.", 400);
    }

    if (warehouse.capacity == null || warehouse.capacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Warehouse capacity exceeds the maximum capacity allowed for location "
              + location.identification,
          400);
    }

    if (warehouse.stock == null || warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException("Warehouse capacity cannot be lower than its stock.", 400);
    }

    long activeWarehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.archivedAt == null)
            .filter(w -> location.identification.equals(w.location))
            .count();
    if (activeWarehousesAtLocation >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Maximum number of warehouses already reached for location " + location.identification, 400);
    }

    int activeCapacityAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.archivedAt == null)
            .filter(w -> location.identification.equals(w.location))
            .map(w -> w.capacity)
            .filter(capacity -> capacity != null)
            .reduce(0, Integer::sum);
    if (activeCapacityAtLocation + warehouse.capacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Warehouse capacity exceeds the maximum capacity allowed for location "
              + location.identification,
          400);
    }

    warehouse.createdAt = LocalDateTime.now();

    warehouseStore.create(warehouse);
  }
}
