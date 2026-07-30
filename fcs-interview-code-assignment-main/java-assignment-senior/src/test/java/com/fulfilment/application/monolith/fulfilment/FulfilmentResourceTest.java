package com.fulfilment.application.monolith.fulfilment;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Note on fixtures: pre-seeded warehouses (ids 1-3) and stores (ids 1-3) are reused wherever
 * possible to avoid burning the small, fixed per-location warehouse quotas defined in
 * LocationGateway. Pre-seeded product id "1" is deliberately avoided since ProductEndpointTest
 * deletes it as part of its own CRUD test, and both test classes share the same database for the
 * whole `mvn test` run.
 */
@QuarkusTest
public class FulfilmentResourceTest {

  private static final String PATH = "fulfilment";

  private String createProduct(String name) {
    return given()
        .contentType("application/json")
        .body("{\"name\":\"" + name + "\",\"stock\":100}")
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getString("id");
  }

  private String createWarehouse(String businessUnitCode) {
    return given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\""
                + businessUnitCode
                + "\",\"location\":\"AMSTERDAM-001\",\"capacity\":10,\"stock\":1}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getString("id");
  }

  private void associate(String warehouseId, String productId, String storeId) {
    given()
        .contentType("application/json")
        .body(fulfilmentJson(warehouseId, productId, storeId))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);
  }

  private String fulfilmentJson(String warehouseId, String productId, String storeId) {
    return "{\"warehouseId\":"
        + warehouseId
        + ",\"productId\":"
        + productId
        + ",\"storeId\":"
        + storeId
        + "}";
  }

  @Test
  public void testCreateFulfilmentAssociationSucceeds() {
    given()
        .contentType("application/json")
        .body(fulfilmentJson("1", "2", "1"))
        .when()
        .post(PATH)
        .then()
        .statusCode(201);
  }

  @Test
  public void testCreateDuplicateFulfilmentAssociationReturnsConflict() {
    associate("1", "3", "1");

    given()
        .contentType("application/json")
        .body(fulfilmentJson("1", "3", "1"))
        .when()
        .post(PATH)
        .then()
        .statusCode(409);
  }

  @Test
  public void testCreateFulfilmentWithUnknownWarehouseReturnsNotFound() {
    given()
        .contentType("application/json")
        .body(fulfilmentJson("99999", "2", "1"))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  public void testCreateFulfilmentWithUnknownProductReturnsNotFound() {
    given()
        .contentType("application/json")
        .body(fulfilmentJson("1", "99999", "1"))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  public void testCreateFulfilmentWithUnknownStoreReturnsNotFound() {
    given()
        .contentType("application/json")
        .body(fulfilmentJson("1", "2", "99999"))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  public void testMaxTwoWarehousesPerProductAndStoreIsEnforced() {
    // product 3 fulfilling store 2 from warehouses 1 and 2 is allowed (the limit)...
    associate("1", "3", "2");
    associate("2", "3", "2");

    // ...but a 3rd distinct warehouse for the same product+store pair is rejected.
    given()
        .contentType("application/json")
        .body(fulfilmentJson("3", "3", "2"))
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testMaxThreeWarehousesPerStoreIsEnforced() {
    String productForWarehouse3 = createProduct("FULFIL-PRODUCT-STORE3");

    // 3 distinct warehouses fulfilling store 3, each with a different product, is allowed.
    associate("1", "2", "3");
    associate("2", "3", "3");
    associate("3", productForWarehouse3, "3");

    // a 4th distinct warehouse for the same store is rejected, even with a brand-new product.
    String freshWarehouse = createWarehouse("MWH.900");
    String freshProduct = createProduct("FULFIL-PRODUCT-STORE3-EXTRA");

    given()
        .contentType("application/json")
        .body(fulfilmentJson(freshWarehouse, freshProduct, "3"))
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testMaxFiveProductsPerWarehouseIsEnforced() {
    String warehouse = createWarehouse("MWH.901");

    for (int i = 0; i < 5; i++) {
      String product = createProduct("FULFIL-PRODUCT-WH-" + i);
      associate(warehouse, product, "1");
    }

    String sixthProduct = createProduct("FULFIL-PRODUCT-WH-6");

    given()
        .contentType("application/json")
        .body(fulfilmentJson(warehouse, sixthProduct, "1"))
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }
}
