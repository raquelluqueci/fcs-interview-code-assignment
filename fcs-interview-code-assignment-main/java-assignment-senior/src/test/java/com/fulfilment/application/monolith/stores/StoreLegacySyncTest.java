package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreLegacySyncTest {

  @Inject RecordingLegacyStoreManagerGateway gateway;

  @BeforeEach
  void reset() {
    gateway.reset();
  }

  @Test
  public void createSyncsLegacySystemOnlyAfterCommit() {
    given()
        .contentType("application/json")
        .body("{\"name\":\"LEGACY-SYNC-CREATE\",\"quantityProductsInStock\":7}")
        .when()
        .post("/store")
        .then()
        .statusCode(201);

    assertEquals(1, gateway.invocations().size());
    var invocation = gateway.invocations().get(0);
    assertEquals("create", invocation.operation());
    assertTrue(
        invocation.committedAtCallTime(),
        "legacy gateway must only be called after the store is committed to the database");
  }

  @Test
  public void failedCreateDoesNotReachLegacySystem() {
    given()
        .contentType("application/json")
        .body("{\"id\":999,\"name\":\"INVALID\",\"quantityProductsInStock\":1}")
        .when()
        .post("/store")
        .then()
        .statusCode(422);

    assertEquals(0, gateway.invocations().size(), "failed requests must not reach the legacy system");
  }

  @Test
  public void updateSyncsLegacySystemOnlyAfterCommit() {
    given()
        .contentType("application/json")
        .body("{\"name\":\"TONSTAD-RENAMED\",\"quantityProductsInStock\":11}")
        .when()
        .put("/store/1")
        .then()
        .statusCode(200);

    assertEquals(1, gateway.invocations().size());
    var invocation = gateway.invocations().get(0);
    assertEquals("update", invocation.operation());
    assertTrue(
        invocation.committedAtCallTime(),
        "legacy gateway must only be called after the update is committed to the database");
  }

  @Test
  public void patchCanUpdateQuantityProductsInStockToZero() {
    String id =
        given()
            .contentType("application/json")
            .body("{\"name\":\"PATCH-ZERO\",\"quantityProductsInStock\":9}")
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");
    gateway.reset();

    int quantity =
        given()
            .contentType("application/json")
            .body("{\"quantityProductsInStock\":0}")
            .when()
            .patch("/store/" + id)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getInt("quantityProductsInStock");

    assertEquals(0, quantity);
    assertEquals(1, gateway.invocations().size());
  }

  @Test
  public void patchCanUpdateOnlyName() {
    String id =
        given()
            .contentType("application/json")
            .body("{\"name\":\"PATCH-NAME\",\"quantityProductsInStock\":7}")
            .when()
            .post("/store")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("id");
    gateway.reset();

    int quantity =
        given()
            .contentType("application/json")
            .body("{\"name\":\"PATCH-NAME-RENAMED\"}")
            .when()
            .patch("/store/" + id)
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getInt("quantityProductsInStock");

    assertEquals(7, quantity);
    assertEquals(1, gateway.invocations().size());
  }
}
