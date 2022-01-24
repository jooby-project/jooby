package io.jooby.i2507;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.StatusCode;
import io.jooby.WebClient;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2507 {
  @ServerTest
  public void shouldNotThrowsStackIsNull(ServerTestRunner runner) {
    runner.define(app -> {
      app.assets("/");
    }).ready((WebClient http) -> {

      http
          .get("/any/path", rsp -> {
            assertEquals(StatusCode.NOT_FOUND.value(),
                rsp.code());
          });
    });
  }
}
