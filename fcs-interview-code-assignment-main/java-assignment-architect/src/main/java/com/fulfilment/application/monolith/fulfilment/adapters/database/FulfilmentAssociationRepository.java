package com.fulfilment.application.monolith.fulfilment.adapters.database;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentAssociation;
import com.fulfilment.application.monolith.fulfilment.domain.ports.FulfilmentAssociationStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class FulfilmentAssociationRepository
    implements FulfilmentAssociationStore, PanacheRepository<DbFulfilmentAssociation> {

  @Override
  public List<FulfilmentAssociation> getAll() {
    return this.listAll().stream().map(DbFulfilmentAssociation::toFulfilmentAssociation).toList();
  }

  @Override
  @Transactional
  public void create(FulfilmentAssociation association) {
    var entity = new DbFulfilmentAssociation();
    entity.warehouseBusinessUnitCode = association.warehouseBusinessUnitCode;
    entity.productId = association.productId;
    entity.storeId = association.storeId;
    entity.createdAt = association.createdAt;
    this.persist(entity);
    association.id = entity.id;
  }

  @Override
  @Transactional
  public void remove(FulfilmentAssociation association) {
    this.deleteById(association.id);
  }

  @Override
  public FulfilmentAssociation findAssociationById(Long id) {
    DbFulfilmentAssociation entity = findById(id);
    return entity == null ? null : entity.toFulfilmentAssociation();
  }

  @Override
  public List<FulfilmentAssociation> findByProductAndStore(Long productId, Long storeId) {
    return find(
            "productId = :productId and storeId = :storeId",
            Parameters.with("productId", productId).and("storeId", storeId))
        .<DbFulfilmentAssociation>stream()
        .map(DbFulfilmentAssociation::toFulfilmentAssociation)
        .toList();
  }

  @Override
  public List<FulfilmentAssociation> findByStore(Long storeId) {
    return find("storeId", storeId).<DbFulfilmentAssociation>stream()
        .map(DbFulfilmentAssociation::toFulfilmentAssociation)
        .toList();
  }

  @Override
  public List<FulfilmentAssociation> findByWarehouse(String warehouseBusinessUnitCode) {
    return find("warehouseBusinessUnitCode", warehouseBusinessUnitCode)
        .<DbFulfilmentAssociation>stream()
        .map(DbFulfilmentAssociation::toFulfilmentAssociation)
        .toList();
  }

  @Override
  public boolean exists(String warehouseBusinessUnitCode, Long productId, Long storeId) {
    return count(
            "warehouseBusinessUnitCode = :buCode and productId = :productId and storeId = :storeId",
            Parameters.with("buCode", warehouseBusinessUnitCode)
                .and("productId", productId)
                .and("storeId", storeId))
        > 0;
  }
}
