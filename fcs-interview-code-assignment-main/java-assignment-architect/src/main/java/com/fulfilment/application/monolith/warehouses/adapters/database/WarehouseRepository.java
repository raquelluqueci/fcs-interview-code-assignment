package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return this.list("archivedAt is null").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    DbWarehouse entity = new DbWarehouse();
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
    this.persist(entity);
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    DbWarehouse entity = findByBusinessUnitCodeOrNull(warehouse.businessUnitCode);
    if (entity == null) {
      return;
    }
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt;
    entity.archivedAt = warehouse.archivedAt;
    this.persist(entity);
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    DbWarehouse entity = findByBusinessUnitCodeOrNull(warehouse.businessUnitCode);
    if (entity != null) {
      this.delete(entity);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse entity = findByBusinessUnitCodeOrNull(buCode);
    return entity == null ? null : entity.toWarehouse();
  }

  private DbWarehouse findByBusinessUnitCodeOrNull(String buCode) {
    // a business unit code can be reused across an archive+replace history, so only the
    // currently active warehouse (archivedAt is null) is a valid match here - callers (domain
    // use cases) always mean "the warehouse currently identified by this code".
    return find("businessUnitCode = :buCode and archivedAt is null", Parameters.with("buCode", buCode))
        .firstResult();
  }
}
