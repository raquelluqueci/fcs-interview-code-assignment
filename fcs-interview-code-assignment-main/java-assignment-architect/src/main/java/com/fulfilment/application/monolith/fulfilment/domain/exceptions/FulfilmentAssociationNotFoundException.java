package com.fulfilment.application.monolith.fulfilment.domain.exceptions;

public class FulfilmentAssociationNotFoundException extends FulfilmentDomainException {

  public FulfilmentAssociationNotFoundException(Long id) {
    super("Fulfilment association not found for id: " + id, 404);
  }
}
