package com.fulfilment.application.monolith.fulfilment.domain.validation;

import com.fulfilment.application.monolith.fulfilment.domain.exceptions.FulfilmentConstraintViolationException;
import com.fulfilment.application.monolith.fulfilment.domain.exceptions.ProductNotFoundException;
import com.fulfilment.application.monolith.fulfilment.domain.exceptions.StoreNotFoundException;
import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentAssociation;
import com.fulfilment.application.monolith.fulfilment.domain.ports.FulfilmentAssociationStore;
import com.fulfilment.application.monolith.fulfilment.domain.ports.ProductResolver;
import com.fulfilment.application.monolith.fulfilment.domain.ports.StoreResolver;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Domain service enforcing the Warehouse-Product-Store fulfilment invariants:
 *
 * <ol>
 *   <li>A Product can be fulfilled by a maximum of 2 different Warehouses per Store
 *   <li>A Store can be fulfilled by a maximum of 3 different Warehouses
 *   <li>A Warehouse can store a maximum of 5 different Products
 * </ol>
 */
@ApplicationScoped
public class FulfilmentAssociationValidator {

  static final int MAX_WAREHOUSES_PER_PRODUCT_AND_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private final FulfilmentAssociationStore fulfilmentAssociationStore;
  private final WarehouseStore warehouseStore;
  private final ProductResolver productResolver;
  private final StoreResolver storeResolver;

  public FulfilmentAssociationValidator(
      FulfilmentAssociationStore fulfilmentAssociationStore,
      WarehouseStore warehouseStore,
      ProductResolver productResolver,
      StoreResolver storeResolver) {
    this.fulfilmentAssociationStore = fulfilmentAssociationStore;
    this.warehouseStore = warehouseStore;
    this.productResolver = productResolver;
    this.storeResolver = storeResolver;
  }

  public void ensureWarehouseExists(String warehouseBusinessUnitCode) {
    var warehouse = warehouseStore.findByBusinessUnitCode(warehouseBusinessUnitCode);
    if (warehouse == null || warehouse.archivedAt != null) {
      throw new WarehouseNotFoundException(warehouseBusinessUnitCode);
    }
  }

  public void ensureProductExists(Long productId) {
    if (!productResolver.existsById(productId)) {
      throw new ProductNotFoundException(productId);
    }
  }

  public void ensureStoreExists(Long storeId) {
    if (!storeResolver.existsById(storeId)) {
      throw new StoreNotFoundException(storeId);
    }
  }

  public void ensureAssociationIsNotDuplicated(FulfilmentAssociation candidate) {
    if (fulfilmentAssociationStore.exists(
        candidate.warehouseBusinessUnitCode, candidate.productId, candidate.storeId)) {
      throw new FulfilmentConstraintViolationException(
          "Warehouse '"
              + candidate.warehouseBusinessUnitCode
              + "' already fulfils product "
              + candidate.productId
              + " for store "
              + candidate.storeId);
    }
  }

  public void ensureProductStoreWarehouseQuota(FulfilmentAssociation candidate) {
    Set<String> warehousesForProductAndStore =
        fulfilmentAssociationStore.findByProductAndStore(candidate.productId, candidate.storeId)
            .stream()
            .map(association -> association.warehouseBusinessUnitCode)
            .collect(Collectors.toSet());

    if (!warehousesForProductAndStore.contains(candidate.warehouseBusinessUnitCode)
        && warehousesForProductAndStore.size() >= MAX_WAREHOUSES_PER_PRODUCT_AND_STORE) {
      throw new FulfilmentConstraintViolationException(
          "Product "
              + candidate.productId
              + " for store "
              + candidate.storeId
              + " already has the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_AND_STORE
              + " fulfilling warehouses");
    }
  }

  public void ensureStoreWarehouseQuota(FulfilmentAssociation candidate) {
    Set<String> warehousesForStore =
        fulfilmentAssociationStore.findByStore(candidate.storeId).stream()
            .map(association -> association.warehouseBusinessUnitCode)
            .collect(Collectors.toSet());

    if (!warehousesForStore.contains(candidate.warehouseBusinessUnitCode)
        && warehousesForStore.size() >= MAX_WAREHOUSES_PER_STORE) {
      throw new FulfilmentConstraintViolationException(
          "Store "
              + candidate.storeId
              + " already has the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " fulfilling warehouses");
    }
  }

  public void ensureWarehouseProductQuota(FulfilmentAssociation candidate) {
    Set<Long> productsForWarehouse =
        fulfilmentAssociationStore.findByWarehouse(candidate.warehouseBusinessUnitCode).stream()
            .map(association -> association.productId)
            .collect(Collectors.toSet());

    if (!productsForWarehouse.contains(candidate.productId)
        && productsForWarehouse.size() >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new FulfilmentConstraintViolationException(
          "Warehouse '"
              + candidate.warehouseBusinessUnitCode
              + "' already stores the maximum of "
              + MAX_PRODUCTS_PER_WAREHOUSE
              + " different products");
    }
  }
}
