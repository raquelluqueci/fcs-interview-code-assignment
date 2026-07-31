package com.fulfilment.application.monolith.stores;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.Transactional;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.jboss.logging.Logger;

@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Inject TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  private static final Logger LOGGER = Logger.getLogger(StoreResource.class.getName());

  /**
   * Ensures the given action only runs after the current transaction has committed
   * successfully, so the legacy system never receives data that didn't actually make it
   * into our database.
   */
  private void runAfterCommit(Runnable action) {
    transactionSynchronizationRegistry.registerInterposedSynchronization(
        new Synchronization() {
          @Override
          public void beforeCompletion() {}

          @Override
          public void afterCompletion(int status) {
            if (status == Status.STATUS_COMMITTED) {
              action.run();
            }
          }
        });
  }

  @GET
  public List<Store> get() {
    return Store.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  @Transactional
  public Response create(Store store) {
    if (store.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    store.persist();

    runAfterCommit(() -> legacyStoreManagerGateway.createStoreOnLegacySystem(store));

    return Response.ok(store).status(201).build();
  }

  @PUT
  @Path("{id}")
  @Transactional
  public Store update(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    entity.name = updatedStore.name;
    entity.quantityProductsInStock = updatedStore.quantityProductsInStock;

    runAfterCommit(() -> legacyStoreManagerGateway.updateStoreOnLegacySystem(entity));

    return entity;
  }

  @PATCH
  @Path("{id}")
  @Transactional
  public Store patch(Long id, ObjectNode updatedStore) {
    Store entity = Store.findById(id);

    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    if (updatedStore == null || updatedStore.isNull()) {
      throw new WebApplicationException("Store patch payload must be provided.", 400);
    }

    if (updatedStore.has("name")) {
      if (updatedStore.get("name").isNull() || updatedStore.get("name").asText().isBlank()) {
        throw new WebApplicationException("Store Name was not set on request.", 422);
      }
      entity.name = updatedStore.get("name").asText();
    }

    if (updatedStore.has("quantityProductsInStock")) {
      if (!updatedStore.get("quantityProductsInStock").canConvertToInt()) {
        throw new WebApplicationException(
            "Store quantityProductsInStock was invalidly set on request.", 422);
      }
      entity.quantityProductsInStock = updatedStore.get("quantityProductsInStock").asInt();
    }

    runAfterCommit(() -> legacyStoreManagerGateway.updateStoreOnLegacySystem(entity));

    return entity;
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response delete(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    entity.delete();
    return Response.status(204).build();
  }

  @Provider
  public static class ErrorMapper implements ExceptionMapper<Exception> {

    @Inject ObjectMapper objectMapper;

    @Override
    public Response toResponse(Exception exception) {
      LOGGER.error("Failed to handle request", exception);

      int code = 500;
      if (exception instanceof WebApplicationException) {
        code = ((WebApplicationException) exception).getResponse().getStatus();
      }

      ObjectNode exceptionJson = objectMapper.createObjectNode();
      exceptionJson.put("exceptionType", exception.getClass().getName());
      exceptionJson.put("code", code);

      if (exception.getMessage() != null) {
        exceptionJson.put("error", exception.getMessage());
      }

      return Response.status(code).entity(exceptionJson).build();
    }
  }
}
