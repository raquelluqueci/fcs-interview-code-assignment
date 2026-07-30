package com.fulfilment.application.monolith.stores;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Recording specialization of {@link LegacyStoreManagerGateway} used by tests. Every invocation is
 * recorded together with a real database probe: at call time it opens a brand-new transaction and
 * checks whether the store row is already visible — which is only true if the surrounding
 * transaction has effectively committed. The real gateway only emulates the legacy system by
 * writing a temp file, so recording invocations loses no production behavior.
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class RecordingLegacyStoreManagerGateway extends LegacyStoreManagerGateway {

  public record Invocation(String operation, Long storeId, boolean committedAtCallTime) {}

  private final List<Invocation> invocations = new CopyOnWriteArrayList<>();

  /** Accessor method (not a field) so calls are delegated through the CDI client proxy. */
  public List<Invocation> invocations() {
    return invocations;
  }

  @Override
  public void createStoreOnLegacySystem(Store store) {
    invocations.add(new Invocation("create", store.id, visibleInNewTransaction(store.id)));
  }

  @Override
  public void updateStoreOnLegacySystem(Store store) {
    invocations.add(new Invocation("update", store.id, visibleInNewTransaction(store.id)));
  }

  private boolean visibleInNewTransaction(Long id) {
    if (id == null) {
      return false;
    }
    return QuarkusTransaction.requiringNew().call(() -> Store.findById(id) != null);
  }

  public void reset() {
    invocations.clear();
  }
}
