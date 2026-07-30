package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private CollectionWarehouseStore warehouseStore;
  private Location location;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    warehouseStore = new CollectionWarehouseStore();
    location = new Location("ZWOLLE-001", 1, 40);
    useCase = new ReplaceWarehouseUseCase(warehouseStore, new SingleLocationResolver(location));
  }

  private Warehouse newWarehouse(String buCode, String locationId, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = locationId;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  public void testReplaceArchivesOldAndCreatesNewWithSameBusinessUnitCode() {
    Warehouse existing = newWarehouse("MWH.100", "ZWOLLE-001", 40, 10);
    warehouseStore.warehouses.add(existing);

    Warehouse replacement = newWarehouse("MWH.100", "ZWOLLE-001", 40, 10);

    useCase.replace(replacement);

    assertNotNull(existing.archivedAt);
    assertEquals(2, warehouseStore.warehouses.size());
    assertNotNull(replacement.createdAt);
  }

  @Test
  public void testReplaceWhenNoActiveWarehouseExistsFails() {
    Warehouse replacement = newWarehouse("MWH.999", "ZWOLLE-001", 40, 10);

    WebApplicationException exception =
        assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
    assertEquals(404, exception.getResponse().getStatus());
  }

  @Test
  public void testReplaceWithCapacityLowerThanExistingStockFails() {
    Warehouse existing = newWarehouse("MWH.100", "ZWOLLE-001", 40, 30);
    warehouseStore.warehouses.add(existing);

    Warehouse replacement = newWarehouse("MWH.100", "ZWOLLE-001", 20, 20);

    WebApplicationException exception =
        assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
    assertEquals(400, exception.getResponse().getStatus());
  }

  @Test
  public void testReplaceWithMismatchingStockFails() {
    Warehouse existing = newWarehouse("MWH.100", "ZWOLLE-001", 40, 10);
    warehouseStore.warehouses.add(existing);

    Warehouse replacement = newWarehouse("MWH.100", "ZWOLLE-001", 40, 20);

    WebApplicationException exception =
        assertThrows(WebApplicationException.class, () -> useCase.replace(replacement));
    assertEquals(400, exception.getResponse().getStatus());
  }

  static class CollectionWarehouseStore implements WarehouseStore {
    List<Warehouse> warehouses = new ArrayList<>();

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
      warehouses.stream()
          .filter(w -> w.businessUnitCode.equals(warehouse.businessUnitCode) && w.archivedAt == null)
          .findFirst()
          .ifPresent(
              existing -> {
                existing.location = warehouse.location;
                existing.capacity = warehouse.capacity;
                existing.stock = warehouse.stock;
                existing.archivedAt = warehouse.archivedAt;
              });
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

  static class SingleLocationResolver implements LocationResolver {
    private final Location location;

    SingleLocationResolver(Location location) {
      this.location = location;
    }

    @Override
    public Location resolveByIdentifier(String identifier) {
      return location != null && location.identification.equals(identifier) ? location : null;
    }
  }
}
