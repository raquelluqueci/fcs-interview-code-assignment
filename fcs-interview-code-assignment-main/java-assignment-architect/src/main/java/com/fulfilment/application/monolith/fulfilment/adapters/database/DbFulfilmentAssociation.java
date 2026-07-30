package com.fulfilment.application.monolith.fulfilment.adapters.database;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentAssociation;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "fulfilment_association")
public class DbFulfilmentAssociation {

  @Id @GeneratedValue public Long id;

  public String warehouseBusinessUnitCode;

  public Long productId;

  public Long storeId;

  public LocalDateTime createdAt;

  public DbFulfilmentAssociation() {}

  public FulfilmentAssociation toFulfilmentAssociation() {
    var association = new FulfilmentAssociation();
    association.id = this.id;
    association.warehouseBusinessUnitCode = this.warehouseBusinessUnitCode;
    association.productId = this.productId;
    association.storeId = this.storeId;
    association.createdAt = this.createdAt;
    return association;
  }
}
