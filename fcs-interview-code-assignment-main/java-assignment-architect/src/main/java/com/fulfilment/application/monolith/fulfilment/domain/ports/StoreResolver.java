package com.fulfilment.application.monolith.fulfilment.domain.ports;

public interface StoreResolver {
  boolean existsById(Long storeId);
}
