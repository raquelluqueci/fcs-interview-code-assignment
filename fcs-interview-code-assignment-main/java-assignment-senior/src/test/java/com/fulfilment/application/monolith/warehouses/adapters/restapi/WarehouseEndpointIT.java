package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
public class WarehouseEndpointIT {

  @Test
  public void testSimpleListWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testSimpleCheckingArchivingWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Archive the ZWOLLE-001:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, ZWOLLE-001 should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString("ZWOLLE-001")),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));
  }

  @Test
  public void testArchivingUnknownWarehouseReturnsNotFound() {
    given().when().delete("warehouse/99999").then().statusCode(404);
  }

  @Test
  public void testGetSingleWarehouse() {
    given().when().get("warehouse/2").then().statusCode(200).body(containsString("MWH.012"));
  }

  @Test
  public void testGetUnknownWarehouseReturnsNotFound() {
    given().when().get("warehouse/99999").then().statusCode(404);
  }

  @Test
  public void testCreateWarehouseWithDuplicateBusinessUnitCodeReturnsBadRequest() {
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"MWH.023\",\"location\":\"TILBURG-001\",\"capacity\":30,\"stock\":10}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseWithInvalidLocationReturnsBadRequest() {
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"MWH.900\",\"location\":\"NOWHERE-001\",\"capacity\":30,\"stock\":10}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateAndReplaceWarehouse() {
    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.500\",\"location\":\"HELMOND-001\",\"capacity\":40,\"stock\":10}")
        .when()
        .post("warehouse")
        .then()
        .statusCode(201)
        .body(containsString("MWH.500"));

    given()
        .contentType("application/json")
        .body(
            "{\"businessUnitCode\":\"MWH.500\",\"location\":\"HELMOND-001\",\"capacity\":45,\"stock\":10}")
        .when()
        .post("warehouse/MWH.500/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.500"));
  }
}
