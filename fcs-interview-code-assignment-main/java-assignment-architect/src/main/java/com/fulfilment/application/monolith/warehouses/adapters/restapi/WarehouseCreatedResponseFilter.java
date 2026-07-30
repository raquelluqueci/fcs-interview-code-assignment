package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * The generated {@code WarehouseResource} interface (from the OpenAPI contract) declares {@code
 * createANewWarehouseUnit} returning the {@code Warehouse} DTO directly rather than a {@code
 * Response}, so the default success status is 200. This filter rewrites it to the 201 the OpenAPI
 * spec promises for warehouse creation, without touching the generated interface (which would be
 * overwritten on the next build anyway).
 */
@Provider
public class WarehouseCreatedResponseFilter implements ContainerResponseFilter {

  @Context ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if (responseContext.getStatus() == 200
        && resourceInfo.getResourceClass() == WarehouseResourceImpl.class
        && "createANewWarehouseUnit".equals(resourceInfo.getResourceMethod().getName())) {
      responseContext.setStatus(Response.Status.CREATED.getStatusCode());
    }
  }
}
