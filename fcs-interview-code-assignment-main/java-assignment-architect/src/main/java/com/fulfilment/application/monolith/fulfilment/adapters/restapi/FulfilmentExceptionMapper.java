package com.fulfilment.application.monolith.fulfilment.adapters.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fulfilment.application.monolith.fulfilment.domain.exceptions.FulfilmentDomainException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FulfilmentExceptionMapper implements ExceptionMapper<FulfilmentDomainException> {

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(FulfilmentDomainException exception) {
    ObjectNode exceptionJson = objectMapper.createObjectNode();
    exceptionJson.put("exceptionType", exception.getClass().getName());
    exceptionJson.put("code", exception.getHttpStatusCode());
    exceptionJson.put("error", exception.getMessage());

    return Response.status(exception.getHttpStatusCode()).entity(exceptionJson).build();
  }
}
