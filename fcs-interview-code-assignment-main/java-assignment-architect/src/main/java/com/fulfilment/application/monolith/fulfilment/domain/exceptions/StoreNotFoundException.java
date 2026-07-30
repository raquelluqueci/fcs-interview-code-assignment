package com.fulfilment.application.monolith.fulfilment.domain.exceptions;

public class StoreNotFoundException extends FulfilmentDomainException {

  public StoreNotFoundException(Long storeId) {
    super("Store not found for id: " + storeId, 404);
  }
}
