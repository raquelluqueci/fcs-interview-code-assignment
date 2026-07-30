package com.fulfilment.application.monolith.fulfilment.domain.ports;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentAssociation;
import java.util.List;

public interface FulfilmentAssociationStore {

  List<FulfilmentAssociation> getAll();

  void create(FulfilmentAssociation association);

  void remove(FulfilmentAssociation association);

  FulfilmentAssociation findAssociationById(Long id);

  List<FulfilmentAssociation> findByProductAndStore(Long productId, Long storeId);

  List<FulfilmentAssociation> findByStore(Long storeId);

  List<FulfilmentAssociation> findByWarehouse(String warehouseBusinessUnitCode);

  boolean exists(String warehouseBusinessUnitCode, Long productId, Long storeId);
}
