package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseResourceTest {

  @Test
  public void testCreateWarehouseSuccessfully() {
    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.001", "location", "AMSTERDAM-001", "capacity", 10, "stock", 5))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201)
        .body("businessUnitCode", equalTo("TST.001"))
        .body("location", equalTo("AMSTERDAM-001"))
        .body("capacity", equalTo(10))
        .body("stock", equalTo(5))
        .body("id", org.hamcrest.Matchers.notNullValue());
  }

  @Test
  public void testCreateWarehouseWithDuplicateBusinessUnitCodeReturns400() {
    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "MWH.001", "location", "ZWOLLE-002", "capacity", 10, "stock", 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseWithInvalidLocationReturns400() {
    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.002", "location", "NOWHERE-999", "capacity", 10, "stock", 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseExceedingLocationMaxNumberOfWarehousesReturns400() {
    // VETSBY-001 has maxNumberOfWarehouses = 1
    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.003", "location", "VETSBY-001", "capacity", 10, "stock", 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.004", "location", "VETSBY-001", "capacity", 10, "stock", 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseExceedingLocationMaxCapacityReturns400() {
    // HELMOND-001 has maxCapacity = 45
    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.005", "location", "HELMOND-001", "capacity", 50, "stock", 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseWithCapacityBelowStockReturns400() {
    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.006", "location", "EINDHOVEN-001", "capacity", 5, "stock", 10))
        .when()
        .post("warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testGetWarehouseByIdNotFoundReturns404() {
    given().when().get("warehouse/{id}", 999999).then().statusCode(404);
  }

  @Test
  public void testArchiveWarehouseTwiceReturns404OnSecondAttempt() {
    String id = given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.007", "location", "AMSTERDAM-001", "capacity", 10, "stock", 1))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201)
        .extract()
        .path("id")
        .toString();

    given().when().delete("warehouse/{id}", id).then().statusCode(204);

    given().when().delete("warehouse/{id}", id).then().statusCode(404);
  }

  @Test
  public void testReplaceUnknownBusinessUnitCodeReturns404() {
    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.999", "location", "AMSTERDAM-002", "capacity", 10, "stock", 1))
        .when()
        .post("warehouse/{businessUnitCode}/replacement", "TST.999-does-not-exist")
        .then()
        .statusCode(404);
  }

  @Test
  public void testReplaceWarehouseSuccessfully() {
    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.008", "location", "AMSTERDAM-002", "capacity", 20, "stock", 10))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.008", "location", "AMSTERDAM-002", "capacity", 25, "stock", 10))
        .when()
        .post("warehouse/{businessUnitCode}/replacement", "TST.008")
        .then()
        .statusCode(200)
        .body("businessUnitCode", equalTo("TST.008"))
        .body("capacity", equalTo(25))
        .body("stock", equalTo(10));
  }

  @Test
  public void testReplaceWarehouseWithMismatchedStockReturns400() {
    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.009", "location", "ZWOLLE-002", "capacity", 30, "stock", 10))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.009", "location", "ZWOLLE-002", "capacity", 15, "stock", 15))
        .when()
        .post("warehouse/{businessUnitCode}/replacement", "TST.009")
        .then()
        .statusCode(400);
  }

  @Test
  public void testReplaceWarehouseWithCapacityBelowPreviousStockReturns400() {
    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.010", "location", "EINDHOVEN-001", "capacity", 30, "stock", 20))
        .when()
        .post("warehouse")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(Map.of("businessUnitCode", "TST.010", "location", "EINDHOVEN-001", "capacity", 15, "stock", 15))
        .when()
        .post("warehouse/{businessUnitCode}/replacement", "TST.010")
        .then()
        .statusCode(400);
  }
}
