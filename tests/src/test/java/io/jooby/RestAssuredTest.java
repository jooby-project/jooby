package io.jooby;

import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class RestAssuredTest {
  @Test
  public void checkDefaultPost() {
    new JoobyRunner(app -> {
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
