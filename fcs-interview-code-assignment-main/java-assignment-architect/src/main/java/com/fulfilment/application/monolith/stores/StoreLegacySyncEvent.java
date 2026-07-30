package com.fulfilment.application.monolith.stores;

/**
 * Domain event fired whenever a {@link Store} is created or updated.
 *
 * <p>It is intentionally decoupled from the transaction boundary: producers only signal "this
 * store changed", while the actual propagation to the legacy system is left to an observer that
 * only reacts once the surrounding transaction has committed successfully (see {@link
 * StoreLegacySyncObserver}). This guarantees the downstream legacy system never sees data that
 * wasn't effectively persisted in our database.
 */
public class StoreLegacySyncEvent {

  public enum Operation {
    CREATED,
    UPDATED
  }

  public final Store store;
  public final Operation operation;

  public StoreLegacySyncEvent(Store store, Operation operation) {
    this.store = store;
    this.operation = operation;
  }
}
