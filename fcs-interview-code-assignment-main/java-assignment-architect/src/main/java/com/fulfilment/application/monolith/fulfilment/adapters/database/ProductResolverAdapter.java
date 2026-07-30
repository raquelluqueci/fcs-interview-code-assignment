package com.fulfilment.application.monolith.fulfilment.adapters.database;

import com.fulfilment.application.monolith.fulfilment.domain.ports.ProductResolver;
import com.fulfilment.application.monolith.products.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProductResolverAdapter implements ProductResolver {

  @Inject ProductRepository productRepository;

  @Override
  public boolean existsById(Long productId) {
    return productId != null && productRepository.findById(productId) != null;
  }
}
