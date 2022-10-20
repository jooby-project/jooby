/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class RestAssuredTest {
  @ServerTest
  public void checkDefaultPost(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.post("/foo", ctx -> ctx.form("one").value());
            })
        .ready(
            client -> {
              given()
                  .port(client.getPort())
                  .param("one", "1")
                  .when()
                  .post("/foo")
                  .then()
                  .assertThat()
                  .body(equalTo("1"));
            });
  }
}
