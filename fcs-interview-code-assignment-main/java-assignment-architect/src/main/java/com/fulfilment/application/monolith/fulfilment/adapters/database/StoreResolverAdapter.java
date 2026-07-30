package com.fulfilment.application.monolith.fulfilment.adapters.database;

import com.fulfilment.application.monolith.fulfilment.domain.ports.StoreResolver;
import com.fulfilment.application.monolith.stores.Store;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StoreResolverAdapter implements StoreResolver {

  @Override
  public boolean existsById(Long storeId) {
    return storeId != null && Store.findById(storeId) != null;
  }
}
