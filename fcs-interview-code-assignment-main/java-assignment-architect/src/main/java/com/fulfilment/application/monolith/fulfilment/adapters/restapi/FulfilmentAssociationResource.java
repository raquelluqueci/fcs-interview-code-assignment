package com.fulfilment.application.monolith.fulfilment.adapters.restapi;

import com.fulfilment.application.monolith.fulfilment.domain.exceptions.FulfilmentAssociationNotFoundException;
import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentAssociation;
import com.fulfilment.application.monolith.fulfilment.domain.ports.CreateFulfilmentAssociationOperation;
import com.fulfilment.application.monolith.fulfilment.domain.ports.FulfilmentAssociationStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * BONUS: associates {@code Warehouse} units as fulfilment units of {@code Products} for
 * {@code Stores}. Hand-coded (no OpenAPI contract exists for this resource yet), consistent with
 * how {@code Product} and {@code Store} are exposed in this codebase.
 */
@Path("fulfilment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentAssociationResource {

  @Inject FulfilmentAssociationStore fulfilmentAssociationStore;
  @Inject CreateFulfilmentAssociationOperation createFulfilmentAssociationOperation;

  @GET
  public List<FulfilmentAssociation> listAll() {
    return fulfilmentAssociationStore.getAll();
  }

  @GET
  @Path("{id}")
  public FulfilmentAssociation getSingle(Long id) {
    FulfilmentAssociation association = fulfilmentAssociationStore.findAssociationById(id);
    if (association == null) {
      throw new FulfilmentAssociationNotFoundException(id);
    }
    return association;
  }

  @POST
  public Response create(FulfilmentAssociationRequest request) {
    if (request == null
        || request.warehouseBusinessUnitCode == null
        || request.warehouseBusinessUnitCode.isBlank()
        || request.productId == null
        || request.storeId == null) {
      throw new WebApplicationException(
          "warehouseBusinessUnitCode, productId and storeId must all be informed.", 400);
    }

    var association = new FulfilmentAssociation();
    association.warehouseBusinessUnitCode = request.warehouseBusinessUnitCode;
    association.productId = request.productId;
    association.storeId = request.storeId;

    createFulfilmentAssociationOperation.create(association);

    return Response.status(201).entity(association).build();
  }

  @DELETE
  @Path("{id}")
  public Response remove(@PathParam("id") Long id) {
    FulfilmentAssociation association = fulfilmentAssociationStore.findAssociationById(id);
    if (association == null) {
      throw new FulfilmentAssociationNotFoundException(id);
    }
    fulfilmentAssociationStore.remove(association);
    return Response.status(204).build();
  }
}
