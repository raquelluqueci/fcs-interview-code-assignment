package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = toDomainWarehouse(requireWarehousePayload(data));

    createWarehouseOperation.create(warehouse);

    DbWarehouse created = findDbWarehouseByBusinessUnitCodeOrThrow(warehouse.businessUnitCode);
    return toWarehouseResponse(created);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    DbWarehouse dbWarehouse = findDbWarehouseByIdOrThrow(id);
    return toWarehouseResponse(dbWarehouse);
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    DbWarehouse dbWarehouse = findDbWarehouseByIdOrThrow(id);

    var warehouse = dbWarehouse.toWarehouse();
    archiveWarehouseOperation.archive(warehouse);
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(String businessUnitCode, @NotNull Warehouse data) {
    var newWarehouse = toDomainWarehouse(requireWarehousePayload(data));
    // the business unit code identifying the warehouse is carried over from the path,
    // regardless of what (if anything) was informed in the request body.
    newWarehouse.businessUnitCode = businessUnitCode;

    replaceWarehouseOperation.replace(newWarehouse);

    DbWarehouse replaced = findDbWarehouseByBusinessUnitCodeOrThrow(businessUnitCode);
    return toWarehouseResponse(replaced);
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainWarehouse(
      Warehouse data) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }

  private Warehouse requireWarehousePayload(Warehouse data) {
    if (data == null) {
      throw new WebApplicationException("Warehouse payload must be provided.", 400);
    }
    if (isBlank(data.getBusinessUnitCode())) {
      throw new WebApplicationException("Warehouse businessUnitCode must be informed.", 400);
    }
    if (isBlank(data.getLocation())) {
      throw new WebApplicationException("Warehouse location must be informed.", 400);
    }
    if (data.getCapacity() == null) {
      throw new WebApplicationException("Warehouse capacity must be informed.", 400);
    }
    if (data.getStock() == null) {
      throw new WebApplicationException("Warehouse stock must be informed.", 400);
    }
    return data;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private DbWarehouse findDbWarehouseByIdOrThrow(String id) {
    Long numericId;
    try {
      numericId = Long.valueOf(id);
    } catch (NumberFormatException e) {
      throw new WarehouseNotFoundException(id);
    }

    DbWarehouse dbWarehouse = warehouseRepository.findById(numericId);
    if (dbWarehouse == null) {
      throw new WarehouseNotFoundException(id);
    }
    return dbWarehouse;
  }

  private DbWarehouse findDbWarehouseByBusinessUnitCodeOrThrow(String businessUnitCode) {
    // a business unit code can be reused by an archived warehouse (replacement history), so the
    // currently active one - the one callers actually care about right after create/replace -
    // must be looked up explicitly rather than relying on insertion order.
    return warehouseRepository
        .find("businessUnitCode = ?1 and archivedAt is null", businessUnitCode)
        .<DbWarehouse>firstResultOptional()
        .orElseThrow(() -> new WarehouseNotFoundException(businessUnitCode));
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }

  private Warehouse toWarehouseResponse(DbWarehouse dbWarehouse) {
    Warehouse response = toWarehouseResponse(dbWarehouse.toWarehouse());
    response.setId(String.valueOf(dbWarehouse.id));
    return response;
  }
}
