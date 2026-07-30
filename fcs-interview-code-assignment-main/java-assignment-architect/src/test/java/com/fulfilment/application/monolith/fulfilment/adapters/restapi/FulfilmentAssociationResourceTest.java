package com.fulfilment.application.monolith.fulfilment.adapters.restapi;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Every real {@code Location} in {@code LocationGateway} caps how many warehouses it can host
 * (1 to 5). Since {@code @QuarkusTest} keeps a single Dev Services database running for the
 * whole test run, a handful of warehouses are created once here and reused across test methods
 * (each method still uses fresh, unique products/stores) instead of minting a brand new warehouse
 * per test - which would exhaust the low-quota locations after a handful of tests.
 */
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FulfilmentAssociationResourceTest {

  private String warehouse1; // AMSTERDAM-001 - general purpose pool warehouse
  private String warehouse2; // AMSTERDAM-002
  private String warehouse3; // EINDHOVEN-001
  private String warehouse4; // ZWOLLE-002
  private String warehouse5; // AMSTERDAM-001 - 4th distinct warehouse for the per-store quota test
  private String warehouseForProductQuota; // HELMOND-001 - dedicated to the per-warehouse quota test

  @BeforeAll
  public void createSharedWarehousePool() {
    warehouse1 = createWarehouse("FUL.001", "AMSTERDAM-001");
    warehouse2 = createWarehouse("FUL.002", "AMSTERDAM-002");
    warehouse3 = createWarehouse("FUL.003", "EINDHOVEN-001");
    warehouse4 = createWarehouse("FUL.004", "ZWOLLE-002");
    warehouse5 = createWarehouse("FUL.005", "AMSTERDAM-001");
    warehouseForProductQuota = createWarehouse("FUL.006", "HELMOND-001");
  }

  private String createWarehouse(String businessUnitCode, String location) {
    return given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", businessUnitCode, "location", location, "capacity", 10, "stock", 0))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201)
        .extract()
        .path("businessUnitCode")
        .toString();
  }

  private Long createStoreEntity(String name) {
    Number id =
        given()
            .contentType("application/json")
            .body(Map.of("name", name, "quantityProductsInStock", 0))
            .when()
            .post("store")
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    return id.longValue();
  }

  private Long createProductEntity(String name) {
    Number id =
        given()
            .contentType("application/json")
            .body(Map.of("name", name, "stock", 0))
            .when()
            .post("product")
            .then()
            .statusCode(201)
            .extract()
            .path("id");
    return id.longValue();
  }

  @Test
  public void testCreateFulfilmentAssociationSuccessfully() {
    Long store = createStoreEntity("FUL-STORE-001");
    Long product = createProductEntity("FUL-PRODUCT-001");

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", warehouse1, "productId", product, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(201);
  }

  @Test
  public void testCreateFulfilmentAssociationWithUnknownWarehouseReturns404() {
    Long store = createStoreEntity("FUL-STORE-002");
    Long product = createProductEntity("FUL-PRODUCT-002");

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", "DOES-NOT-EXIST", "productId", product, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(404);
  }

  @Test
  public void testCreateFulfilmentAssociationWithUnknownProductReturns404() {
    Long store = createStoreEntity("FUL-STORE-003");

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", warehouse1, "productId", 999999, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(404);
  }

  @Test
  public void testCreateFulfilmentAssociationWithUnknownStoreReturns404() {
    Long product = createProductEntity("FUL-PRODUCT-004");

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", warehouse1, "productId", product, "storeId", 999999))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDuplicateFulfilmentAssociationReturns400() {
    Long store = createStoreEntity("FUL-STORE-005");
    Long product = createProductEntity("FUL-PRODUCT-005");

    Map<String, Object> body =
        Map.of("warehouseBusinessUnitCode", warehouse1, "productId", product, "storeId", store);

    given().contentType("application/json").body(body).when().post("fulfilment").then().statusCode(201);

    given().contentType("application/json").body(body).when().post("fulfilment").then().statusCode(400);
  }

  @Test
  public void testMaxTwoWarehousesPerProductAndStoreIsEnforced() {
    Long store = createStoreEntity("FUL-STORE-006");
    Long product = createProductEntity("FUL-PRODUCT-006");

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", warehouse1, "productId", product, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", warehouse2, "productId", product, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", warehouse3, "productId", product, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(400);
  }

  @Test
  public void testMaxThreeWarehousesPerStoreIsEnforced() {
    Long store = createStoreEntity("FUL-STORE-007");
    Long product1 = createProductEntity("FUL-PRODUCT-007");
    Long product2 = createProductEntity("FUL-PRODUCT-008");
    Long product3 = createProductEntity("FUL-PRODUCT-009");
    Long product4 = createProductEntity("FUL-PRODUCT-010");

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", warehouse2, "productId", product1, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", warehouse3, "productId", product2, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", warehouse4, "productId", product3, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(Map.of("warehouseBusinessUnitCode", warehouse5, "productId", product4, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(400);
  }

  @Test
  public void testMaxFiveProductsPerWarehouseIsEnforced() {
    Long store = createStoreEntity("FUL-STORE-008");

    for (int i = 1; i <= 5; i++) {
      Long product = createProductEntity("FUL-PRODUCT-0" + (10 + i));
      given()
          .contentType("application/json")
          .body(
              Map.of(
                  "warehouseBusinessUnitCode", warehouseForProductQuota, "productId", product, "storeId", store))
          .when()
          .post("fulfilment")
          .then()
          .statusCode(201);
    }

    Long sixthProduct = createProductEntity("FUL-PRODUCT-999");
    given()
        .contentType("application/json")
        .body(
            Map.of(
                "warehouseBusinessUnitCode", warehouseForProductQuota, "productId", sixthProduct, "storeId", store))
        .when()
        .post("fulfilment")
        .then()
        .statusCode(400);
  }

  @Test
  public void testDeleteFulfilmentAssociation() {
    Long store = createStoreEntity("FUL-STORE-009");
    Long product = createProductEntity("FUL-PRODUCT-900");

    Number id =
        given()
            .contentType("application/json")
            .body(Map.of("warehouseBusinessUnitCode", warehouse1, "productId", product, "storeId", store))
            .when()
            .post("fulfilment")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given().when().delete("fulfilment/{id}", id.longValue()).then().statusCode(204);

    given().when().get("fulfilment/{id}", id.longValue()).then().statusCode(404);
  }
}
