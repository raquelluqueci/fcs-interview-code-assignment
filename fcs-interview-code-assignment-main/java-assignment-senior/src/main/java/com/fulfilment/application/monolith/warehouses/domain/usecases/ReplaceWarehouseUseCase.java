package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new WebApplicationException(
          "No active warehouse found for business unit code " + newWarehouse.businessUnitCode, 404);
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WebApplicationException("Location " + newWarehouse.location + " is not valid.", 400);
    }

    if (newWarehouse.capacity == null || newWarehouse.capacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Warehouse capacity exceeds the maximum capacity allowed for location "
              + location.identification,
          400);
    }

    if (newWarehouse.stock == null || newWarehouse.stock > newWarehouse.capacity) {
      throw new WebApplicationException("Warehouse capacity cannot be lower than its stock.", 400);
    }

    if (newWarehouse.capacity < existing.stock) {
      throw new WebApplicationException(
          "The new warehouse capacity cannot accommodate the stock of the replaced warehouse.", 400);
    }

    if (!newWarehouse.stock.equals(existing.stock)) {
      throw new WebApplicationException(
          "The new warehouse stock must match the stock of the replaced warehouse.", 400);
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
  }
}
