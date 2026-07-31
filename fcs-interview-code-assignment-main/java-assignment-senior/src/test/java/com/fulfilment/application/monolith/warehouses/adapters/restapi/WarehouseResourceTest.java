package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseResourceTest {

  private static final String PATH = "warehouse";

  @Test
  public void testListAllInitialWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testGetSingleWarehouseById() {
    given().when().get(PATH + "/1").then().statusCode(200).body(containsString("MWH.001"));
  }

  @Test
  public void testGetUnknownWarehouseReturnsNotFound() {
    given().when().get(PATH + "/99999").then().statusCode(404);
  }

  @Test
  public void testCreateWarehouseSucceeds() {
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.700\",\"location\":\"EINDHOVEN-001\",\"capacity\":50,\"stock\":10}")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("MWH.700"), containsString("EINDHOVEN-001"));
  }

  @Test
  public void testCreateWarehouseWithDuplicateBusinessUnitCodeReturnsBadRequest() {
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.001\",\"location\":\"ZWOLLE-002\",\"capacity\":30,\"stock\":10}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseWithInvalidLocationReturnsBadRequest() {
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.701\",\"location\":\"NOWHERE-001\",\"capacity\":30,\"stock\":10}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseWithCapacityAboveLocationMaxReturnsBadRequest() {
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.702\",\"location\":\"TILBURG-001\",\"capacity\":9999,\"stock\":10}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseWithStockAboveCapacityReturnsBadRequest() {
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.703\",\"location\":\"EINDHOVEN-001\",\"capacity\":10,\"stock\":20}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseWhenLocationCapacityReachedReturnsBadRequest() {
    // TILBURG-001 allows only 1 warehouse, and MWH.023 already occupies it.
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.704\",\"location\":\"TILBURG-001\",\"capacity\":10,\"stock\":5}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testArchiveWarehouseRemovesItFromActiveList() {
    String id =
        given()
            .contentType("application/json")
            .body(
                "{\"businessUnitCode\":\"MWH.705\",\"location\":\"HELMOND-001\",\"capacity\":40,\"stock\":5}")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH).then().statusCode(200).body(not(containsString("MWH.705")));
  }

  @Test
  public void testArchiveUnknownWarehouseReturnsNotFound() {
    given().when().delete(PATH + "/99999").then().statusCode(404);
  }

  @Test
  public void testReplaceWarehouseArchivesOldAndCreatesNewWithSameBusinessUnitCode() {
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.706\",\"location\":\"VETSBY-001\",\"capacity\":50,\"stock\":20}")
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.706\",\"location\":\"VETSBY-001\",\"capacity\":60,\"stock\":20}")
        .when()
        .post(PATH + "/MWH.706/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.706"));
  }

  @Test
  public void testReplaceWarehouseWithMismatchingStockReturnsBadRequest() {
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.707\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":20}")
        .when()
        .post(PATH)
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.707\",\"location\":\"AMSTERDAM-002\",\"capacity\":60,\"stock\":25}")
        .when()
        .post(PATH + "/MWH.707/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  public void testReplaceUnknownWarehouseReturnsNotFound() {
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.999\",\"location\":\"AMSTERDAM-002\",\"capacity\":50,\"stock\":20}")
        .when()
        .post(PATH + "/MWH.999/replacement")
        .then()
        .statusCode(404);
  }
}
