package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidWarehouseReplacementException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator warehouseValidator;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.warehouseValidator = warehouseValidator;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    Warehouse previousWarehouse = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (previousWarehouse == null || previousWarehouse.archivedAt != null) {
      throw new WarehouseNotFoundException(newWarehouse.businessUnitCode);
    }

    Location location = warehouseValidator.ensureValidLocation(newWarehouse.location);
    warehouseValidator.ensureLocationHasRoomForNewWarehouse(location, previousWarehouse.businessUnitCode);
    warehouseValidator.ensureLocationCapacityIsRespected(
        location, newWarehouse.capacity, previousWarehouse.businessUnitCode);
    warehouseValidator.ensureCapacityCoversStock(newWarehouse.capacity, newWarehouse.stock);

    // Additional replacement-only invariants: the new warehouse must accommodate the stock being
    // carried over, and that stock must match exactly - this is a swap-in-place, not a stock
    // adjustment.
    if (newWarehouse.capacity < previousWarehouse.stock) {
      throw new InvalidWarehouseReplacementException(
          "New warehouse capacity of "
              + newWarehouse.capacity
              + " cannot accommodate the stock of the replaced warehouse ("
              + previousWarehouse.stock
              + ")");
    }
    if (!newWarehouse.stock.equals(previousWarehouse.stock)) {
      throw new InvalidWarehouseReplacementException(
          "New warehouse stock ("
              + newWarehouse.stock
              + ") must match the stock of the replaced warehouse ("
              + previousWarehouse.stock
              + ")");
    }

    previousWarehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(previousWarehouse);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}
