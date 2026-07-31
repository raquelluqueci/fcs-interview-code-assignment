package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validation.WarehouseValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

/**
 * EN: Creates a warehouse after delegating rule checks to WarehouseValidator.
 * PT: Cria um warehouse depois de delegar as regras de negocio ao WarehouseValidator.
 */
@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final WarehouseValidator warehouseValidator;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, WarehouseValidator warehouseValidator) {
    this.warehouseStore = warehouseStore;
    this.warehouseValidator = warehouseValidator;
  }

  /**
   * EN: Validate then persist; validators throw domain/HTTP errors on breach.
   * PT: Valida e depois persiste; os validators lancam erros de dominio/HTTP se houver violacao.
   */
  @Override
  public void create(Warehouse warehouse) {
    // EN: Keep orchestration thin — all policy lives in WarehouseValidator.
    // PT: Orquestracao fina — toda a politica fica no WarehouseValidator.
    warehouseValidator.ensureBusinessUnitCodeIsUnique(warehouse.businessUnitCode);

    Location location = warehouseValidator.ensureValidLocation(warehouse.location);
    warehouseValidator.ensureLocationHasRoomForNewWarehouse(location, null);
    warehouseValidator.ensureLocationCapacityIsRespected(location, warehouse.capacity, null);
    warehouseValidator.ensureCapacityCoversStock(warehouse.capacity, warehouse.stock);

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    warehouseStore.create(warehouse);
  }
}
