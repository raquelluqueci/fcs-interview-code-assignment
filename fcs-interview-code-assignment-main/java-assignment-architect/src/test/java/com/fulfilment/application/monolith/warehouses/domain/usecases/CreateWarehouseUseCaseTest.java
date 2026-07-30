package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.DuplicateBusinessUnitCodeException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidLocationException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.InvalidWarehouseCapacityException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.LocationCapacityExceededException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseValidator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private StaticLocationResolver locationResolver;
  private CreateWarehouseUseCase createWarehouseUseCase;

  @BeforeEach
  public void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    locationResolver = new StaticLocationResolver();
    locationResolver.register(new Location("ZWOLLE-001", 2, 100));

    createWarehouseUseCase =
        new CreateWarehouseUseCase(warehouseStore, new WarehouseValidator(locationResolver, warehouseStore));
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
  public void testCreateWarehouseSuccessfully() {
    Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 50, 10);

    createWarehouseUseCase.create(warehouse);

    assertEquals(1, warehouseStore.warehouses.size());
    assertNotNull(warehouse.createdAt);
    assertEquals("MWH.100", warehouseStore.warehouses.get(0).businessUnitCode);
  }

  @Test
  public void testCreateWarehouseWithDuplicateBusinessUnitCodeShouldThrow() {
    warehouseStore.create(newWarehouse("MWH.100", "ZWOLLE-001", 50, 10));

    Warehouse duplicate = newWarehouse("MWH.100", "ZWOLLE-001", 10, 5);

    assertThrows(DuplicateBusinessUnitCodeException.class, () -> createWarehouseUseCase.create(duplicate));
  }

  @Test
  public void testCreateWarehouseReusingArchivedBusinessUnitCodeShouldSucceed() {
    Warehouse archived = newWarehouse("MWH.100", "ZWOLLE-001", 50, 10);
    archived.archivedAt = LocalDateTime.now();
    warehouseStore.create(archived);

    Warehouse newOne = newWarehouse("MWH.100", "ZWOLLE-001", 10, 5);

    createWarehouseUseCase.create(newOne);

    assertEquals(2, warehouseStore.warehouses.size());
  }

  @Test
  public void testCreateWarehouseWithInvalidLocationShouldThrow() {
    Warehouse warehouse = newWarehouse("MWH.100", "DOES-NOT-EXIST", 50, 10);

    assertThrows(InvalidLocationException.class, () -> createWarehouseUseCase.create(warehouse));
  }

  @Test
  public void testCreateWarehouseWhenLocationReachedMaxNumberOfWarehousesShouldThrow() {
    warehouseStore.create(newWarehouse("MWH.100", "ZWOLLE-001", 10, 1));
    warehouseStore.create(newWarehouse("MWH.101", "ZWOLLE-001", 10, 1));

    Warehouse thirdWarehouse = newWarehouse("MWH.102", "ZWOLLE-001", 10, 1);

    assertThrows(
        LocationCapacityExceededException.class, () -> createWarehouseUseCase.create(thirdWarehouse));
  }

  @Test
  public void testCreateWarehouseExceedingLocationMaxCapacityShouldThrow() {
    warehouseStore.create(newWarehouse("MWH.100", "ZWOLLE-001", 80, 10));

    Warehouse warehouse = newWarehouse("MWH.101", "ZWOLLE-001", 30, 10);

    assertThrows(InvalidWarehouseCapacityException.class, () -> createWarehouseUseCase.create(warehouse));
  }

  @Test
  public void testCreateWarehouseWithCapacityBelowStockShouldThrow() {
    Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 5, 10);

    assertThrows(InvalidWarehouseCapacityException.class, () -> createWarehouseUseCase.create(warehouse));
  }
}
