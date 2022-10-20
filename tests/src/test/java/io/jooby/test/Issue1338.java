/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1338 {
  @ServerTest
  @DisplayName("Form should not fail when empty")
  public void issue1338(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.post("/1338/form", ctx -> ctx.form().toMap());

              app.post("/1338/multipart", ctx -> ctx.form().toMap());
            })
        .ready(
            client -> {
              client.post(
                  "/1338/form",
                  rsp -> {
                    assertEquals("{}", rsp.body().string());
                  });
              given()
                  .port(client.getPort())
                  .contentType("multipart/form-data;")
                  .when()
                  .post("/1338/multipart")
                  .then()
                  .assertThat()
                  .body(equalTo("{}"));
            });
  }
}
