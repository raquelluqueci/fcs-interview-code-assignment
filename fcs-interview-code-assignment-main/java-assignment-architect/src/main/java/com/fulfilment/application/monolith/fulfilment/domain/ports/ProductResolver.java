package com.fulfilment.application.monolith.fulfilment.domain.ports;

public interface ProductResolver {
  boolean existsById(Long productId);
}
