package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("fulfilment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentResource {

  private static final int MAX_WAREHOUSES_PER_PRODUCT_AND_STORE = 2;
  private static final int MAX_WAREHOUSES_PER_STORE = 3;
  private static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  @Inject WarehouseRepository warehouseRepository;

  @Inject ProductRepository productRepository;

  @GET
  public List<Fulfilment> get() {
    return Fulfilment.listAll();
  }

  @POST
  @Transactional
  public Response create(Fulfilment request) {
    if (request.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }
    if (request.warehouseId == null || request.productId == null || request.storeId == null) {
      throw new WebApplicationException("warehouseId, productId and storeId are all required.", 400);
    }

    DbWarehouse warehouse = warehouseRepository.findById(request.warehouseId);
    if (warehouse == null || warehouse.archivedAt != null) {
      throw new WebApplicationException(
          "Warehouse with id " + request.warehouseId + " does not exist or is archived.", 404);
    }

    Product product = productRepository.findById(request.productId);
    if (product == null) {
      throw new WebApplicationException(
          "Product with id " + request.productId + " does not exist.", 404);
    }

    Store store = Store.findById(request.storeId);
    if (store == null) {
      throw new WebApplicationException("Store with id " + request.storeId + " does not exist.", 404);
    }

    boolean alreadyAssociated =
        Fulfilment.find(
                "warehouseId = ?1 and productId = ?2 and storeId = ?3",
                request.warehouseId, request.productId, request.storeId)
            .firstResultOptional()
            .isPresent();
    if (alreadyAssociated) {
      throw new WebApplicationException(
          "This warehouse is already fulfilling this product for this store.", 409);
    }

    Set<Long> warehousesForStore =
        Fulfilment.<Fulfilment>list("storeId", request.storeId).stream()
            .map(f -> f.warehouseId)
            .collect(Collectors.toSet());
    if (!warehousesForStore.contains(request.warehouseId)
        && warehousesForStore.size() >= MAX_WAREHOUSES_PER_STORE) {
      throw new WebApplicationException(
          "Store " + request.storeId + " already has the maximum of " + MAX_WAREHOUSES_PER_STORE
              + " fulfilling warehouses.",
          400);
    }

    Set<Long> productsForWarehouse =
        Fulfilment.<Fulfilment>list("warehouseId", request.warehouseId).stream()
            .map(f -> f.productId)
            .collect(Collectors.toSet());
    if (!productsForWarehouse.contains(request.productId)
        && productsForWarehouse.size() >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new WebApplicationException(
          "Warehouse " + request.warehouseId + " already fulfils the maximum of "
              + MAX_PRODUCTS_PER_WAREHOUSE + " different products.",
          400);
    }

    Set<Long> warehousesForProductInStore =
        Fulfilment.<Fulfilment>list(
                "productId = ?1 and storeId = ?2", request.productId, request.storeId)
            .stream()
            .map(f -> f.warehouseId)
            .collect(Collectors.toSet());
    if (!warehousesForProductInStore.contains(request.warehouseId)
        && warehousesForProductInStore.size() >= MAX_WAREHOUSES_PER_PRODUCT_AND_STORE) {
      throw new WebApplicationException(
          "Product " + request.productId + " already has the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_AND_STORE + " warehouses fulfilling store "
              + request.storeId + ".",
          400);
    }

    request.persist();

    return Response.ok(request).status(201).build();
  }
}
