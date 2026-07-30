package com.fulfilment.application.monolith.fulfilment.adapters.restapi;

/** Plain request payload for creating a fulfilment association. */
public class FulfilmentAssociationRequest {

  public String warehouseBusinessUnitCode;

  public Long productId;

  public Long storeId;
}
