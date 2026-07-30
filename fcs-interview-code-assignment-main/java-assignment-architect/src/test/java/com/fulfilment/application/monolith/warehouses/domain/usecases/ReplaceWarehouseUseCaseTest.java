package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidWarehouseCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidWarehouseReplacementException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseValidator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private StaticLocationResolver locationResolver;
  private ReplaceWarehouseUseCase replaceWarehouseUseCase;
  private Warehouse existingWarehouse;

  @BeforeEach
  public void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    locationResolver = new StaticLocationResolver();
    locationResolver.register(new Location("ZWOLLE-001", 2, 100));

    replaceWarehouseUseCase =
        new ReplaceWarehouseUseCase(warehouseStore, new WarehouseValidator(locationResolver, warehouseStore));

    existingWarehouse = newWarehouse("MWH.100", "ZWOLLE-001", 50, 20);
    warehouseStore.create(existingWarehouse);
  }

  private Warehouse newWarehouse(String buCode, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  public void testReplaceWarehouseSuccessfully() {
    Warehouse replacement = newWarehouse("MWH.100", "ZWOLLE-001", 60, 20);

    replaceWarehouseUseCase.replace(replacement);

    assertNotNull(existingWarehouse.archivedAt);
    assertNotNull(replacement.createdAt);
    assertEquals(2, warehouseStore.warehouses.size());
  }

  @Test
  public void testReplaceUnknownBusinessUnitCodeShouldThrow() {
    Warehouse replacement = newWarehouse("MWH.999", "ZWOLLE-001", 60, 20);

    assertThrows(WarehouseNotFoundException.class, () -> replaceWarehouseUseCase.replace(replacement));
  }

  @Test
  public void testReplaceAlreadyArchivedWarehouseShouldThrow() {
    existingWarehouse.archivedAt = LocalDateTime.now();

    Warehouse replacement = newWarehouse("MWH.100", "ZWOLLE-001", 60, 20);

    assertThrows(WarehouseNotFoundException.class, () -> replaceWarehouseUseCase.replace(replacement));
  }

  @Test
  public void testReplaceWithCapacityBelowPreviousStockShouldThrow() {
    Warehouse replacement = newWarehouse("MWH.100", "ZWOLLE-001", 15, 15);

    assertThrows(
        InvalidWarehouseReplacementException.class, () -> replaceWarehouseUseCase.replace(replacement));
  }

  @Test
  public void testReplaceWithDifferentStockShouldThrow() {
    Warehouse replacement = newWarehouse("MWH.100", "ZWOLLE-001", 60, 25);

    assertThrows(
        InvalidWarehouseReplacementException.class, () -> replaceWarehouseUseCase.replace(replacement));
  }

  @Test
  public void testReplaceExceedingLocationMaxCapacityShouldThrow() {
    warehouseStore.create(newWarehouse("MWH.200", "ZWOLLE-001", 40, 5));

    // existing (MWH.100, 50) is excluded from the sum, so 40 (MWH.200) + 70 (replacement) = 110 > 100
    Warehouse replacement = newWarehouse("MWH.100", "ZWOLLE-001", 70, 20);

    assertThrows(InvalidWarehouseCapacityException.class, () -> replaceWarehouseUseCase.replace(replacement));
  }
}
