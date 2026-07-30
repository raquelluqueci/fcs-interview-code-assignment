package com.fulfilment.application.monolith.fulfilment.domain.exceptions;

public class ProductNotFoundException extends FulfilmentDomainException {

  public ProductNotFoundException(Long productId) {
    super("Product not found for id: " + productId, 404);
  }
}
