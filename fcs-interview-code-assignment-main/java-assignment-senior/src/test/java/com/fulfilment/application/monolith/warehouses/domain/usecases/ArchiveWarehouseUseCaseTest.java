package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  @Test
  public void testArchiveSetsArchivedAtAndPersistsThroughStore() {
    // given
    CollectionWarehouseStore warehouseStore = new CollectionWarehouseStore();
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";
    warehouse.location = "ZWOLLE-001";
    warehouse.capacity = 40;
    warehouse.stock = 10;
    warehouseStore.warehouses.add(warehouse);

    ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(warehouseStore);

    // when
    useCase.archive(warehouse);

    // then
    assertNotNull(warehouse.archivedAt);
    assertEquals(1, warehouseStore.updateCalls);
  }

  static class CollectionWarehouseStore implements WarehouseStore {
    List<Warehouse> warehouses = new ArrayList<>();
    int updateCalls = 0;

    @Override
    public List<Warehouse> getAll() {
      return warehouses;
    }

    @Override
    public void create(Warehouse warehouse) {
      warehouses.add(warehouse);
    }

    @Override
    public void update(Warehouse warehouse) {
      updateCalls++;
    }

    @Override
    public void remove(Warehouse warehouse) {
      warehouses.removeIf(w -> w.businessUnitCode.equals(warehouse.businessUnitCode));
    }

    @Override
    public Warehouse findByBusinessUnitCode(String buCode) {
      return warehouses.stream()
          .filter(w -> w.businessUnitCode.equals(buCode) && w.archivedAt == null)
          .findFirst()
          .orElse(null);
    }
  }
}
