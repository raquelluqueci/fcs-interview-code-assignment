package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseDomainException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Dedicated translation of Warehouse domain invariants into HTTP responses. Keeping this mapping
 * out of the use cases lets the domain layer stay free of any JAX-RS/HTTP concern.
 */
@Provider
public class WarehouseExceptionMapper implements ExceptionMapper<WarehouseDomainException> {

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(WarehouseDomainException exception) {
    ObjectNode exceptionJson = objectMapper.createObjectNode();
    exceptionJson.put("exceptionType", exception.getClass().getName());
    exceptionJson.put("code", exception.getHttpStatusCode());
    exceptionJson.put("error", exception.getMessage());

    return Response.status(exception.getHttpStatusCode()).entity(exceptionJson).build();
  }
}
