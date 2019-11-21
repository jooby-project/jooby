package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class RestAssuredTest {
  @ServerTest
  public void checkDefaultPost(ServerTestRunner runner) {
    runner.define(app -> {
      app.post("/foo", ctx -> ctx.form("one").value());
    }).ready(client -> {
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
