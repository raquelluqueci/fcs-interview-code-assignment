package com.fulfilment.application.monolith.fulfilment;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Associates a Warehouse as a fulfilment unit of a given Product to a given Store.
 */
@Entity
@Table(
    name = "fulfilment",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"warehouseId", "productId", "storeId"}))
@Cacheable
public class Fulfilment extends PanacheEntity {

  @Column(nullable = false)
  public Long warehouseId;

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public Long storeId;

  public Fulfilment() {}
}
