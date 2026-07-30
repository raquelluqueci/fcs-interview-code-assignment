package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

/**
 * Propagates {@link Store} changes to the legacy system.
 *
 * <p>The observer method is bound to {@link TransactionPhase#AFTER_SUCCESS}, so it only executes
 * once the transaction that produced the {@link StoreLegacySyncEvent} has committed. If the
 * transaction rolls back, the legacy system is never contacted, guaranteeing it only receives
 * confirmed data.
 */
@ApplicationScoped
public class StoreLegacySyncObserver {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreChanged(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreLegacySyncEvent event) {
    switch (event.operation) {
      case CREATED -> legacyStoreManagerGateway.createStoreOnLegacySystem(event.store);
      case UPDATED -> legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store);
    }
  }
}
