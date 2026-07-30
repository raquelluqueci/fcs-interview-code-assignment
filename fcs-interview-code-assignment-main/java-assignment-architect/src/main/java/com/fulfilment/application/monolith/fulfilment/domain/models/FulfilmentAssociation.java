package com.fulfilment.application.monolith.fulfilment.domain.models;

import java.time.LocalDateTime;

/**
 * Represents a fulfilment association: a {@code Warehouse} (identified by its business unit
 * code) that is entitled to fulfil a given {@code Product} for a given {@code Store}.
 */
public class FulfilmentAssociation {

  public Long id;

  public String warehouseBusinessUnitCode;

  public Long productId;

  public Long storeId;

  public LocalDateTime createdAt;
}
