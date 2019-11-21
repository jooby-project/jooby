package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import org.junit.jupiter.api.DisplayName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1338 {
  @ServerTest
  @DisplayName("Form should not fail when empty")
  public void issue1338(ServerTestRunner runner) {
    runner.define(app -> {
      app.post("/1338/form", ctx -> ctx.form().toMap());

      app.post("/1338/multipart", ctx -> ctx.form().toMap());

    }).ready(client -> {
      client.post("/1338/form", rsp -> {
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
