package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory {@link WarehouseStore} implementation backing the use case unit tests: it keeps
 * warehouses in a plain list so tests can assert on state without needing a real database.
 */
class InMemoryWarehouseStore implements WarehouseStore {

  final List<Warehouse> warehouses = new ArrayList<>();

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
    // callers always mutate the same object instance returned by findByBusinessUnitCode,
    // so the in-memory list already reflects the change once this method is invoked.
  }

  @Override
  public void remove(Warehouse warehouse) {
    warehouses.remove(warehouse);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return warehouses.stream()
        .filter(warehouse -> buCode.equals(warehouse.businessUnitCode))
        .findFirst()
        .orElse(null);
  }
}
