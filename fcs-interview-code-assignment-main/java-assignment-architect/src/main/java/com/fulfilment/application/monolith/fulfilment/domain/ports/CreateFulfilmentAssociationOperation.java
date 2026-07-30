package com.fulfilment.application.monolith.fulfilment.domain.ports;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentAssociation;

public interface CreateFulfilmentAssociationOperation {
  void create(FulfilmentAssociation association);
}
