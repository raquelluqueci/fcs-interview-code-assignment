package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private InMemoryWarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase archiveWarehouseUseCase;
  private Warehouse existingWarehouse;

  @BeforeEach
  public void setUp() {
    warehouseStore = new InMemoryWarehouseStore();
    archiveWarehouseUseCase = new ArchiveWarehouseUseCase(warehouseStore);

    existingWarehouse = new Warehouse();
    existingWarehouse.businessUnitCode = "MWH.100";
    existingWarehouse.location = "ZWOLLE-001";
    existingWarehouse.capacity = 50;
    existingWarehouse.stock = 20;
    warehouseStore.create(existingWarehouse);
  }

  @Test
  public void testArchiveWarehouseSuccessfully() {
    var request = new Warehouse();
    request.businessUnitCode = "MWH.100";

    archiveWarehouseUseCase.archive(request);

    assertNotNull(existingWarehouse.archivedAt);
  }

  @Test
  public void testArchiveUnknownWarehouseShouldThrow() {
    var request = new Warehouse();
    request.businessUnitCode = "MWH.999";

    assertThrows(WarehouseNotFoundException.class, () -> archiveWarehouseUseCase.archive(request));
  }

  @Test
  public void testArchiveAlreadyArchivedWarehouseShouldThrow() {
    existingWarehouse.archivedAt = LocalDateTime.now();

    var request = new Warehouse();
    request.businessUnitCode = "MWH.100";

    assertThrows(WarehouseNotFoundException.class, () -> archiveWarehouseUseCase.archive(request));
  }
}
