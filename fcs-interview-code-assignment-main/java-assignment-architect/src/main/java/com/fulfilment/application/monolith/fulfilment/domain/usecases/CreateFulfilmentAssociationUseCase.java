package com.fulfilment.application.monolith.fulfilment.domain.usecases;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentAssociation;
import com.fulfilment.application.monolith.fulfilment.domain.ports.CreateFulfilmentAssociationOperation;
import com.fulfilment.application.monolith.fulfilment.domain.ports.FulfilmentAssociationStore;
import com.fulfilment.application.monolith.fulfilment.domain.validation.FulfilmentAssociationValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateFulfilmentAssociationUseCase implements CreateFulfilmentAssociationOperation {

  private final FulfilmentAssociationStore fulfilmentAssociationStore;
  private final FulfilmentAssociationValidator validator;

  public CreateFulfilmentAssociationUseCase(
      FulfilmentAssociationStore fulfilmentAssociationStore, FulfilmentAssociationValidator validator) {
    this.fulfilmentAssociationStore = fulfilmentAssociationStore;
    this.validator = validator;
  }

  @Override
  public void create(FulfilmentAssociation association) {
    validator.ensureWarehouseExists(association.warehouseBusinessUnitCode);
    validator.ensureProductExists(association.productId);
    validator.ensureStoreExists(association.storeId);

    validator.ensureAssociationIsNotDuplicated(association);
    validator.ensureProductStoreWarehouseQuota(association);
    validator.ensureStoreWarehouseQuota(association);
    validator.ensureWarehouseProductQuota(association);

    association.createdAt = LocalDateTime.now();
    fulfilmentAssociationStore.create(association);
  }
}
