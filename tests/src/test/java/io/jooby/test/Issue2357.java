package io.jooby.test;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import org.junit.jupiter.api.Assertions;

class Issue2357 {

  @ServerTest
  public void headersShouldBeCaseInsensitive(ServerTestRunner runner) {
    runner.define(app -> app.get("/", ctx -> {
      Assertions.assertEquals("value1", ctx.header().get("x-header1").value());
      Assertions.assertEquals("value1", ctx.header().get("X-HEADER1").value());
      Assertions.assertEquals("value1", ctx.header().get("X-hEaDeR1").value());
      Assertions.assertEquals("value1", ctx.header("x-header1").value());
      Assertions.assertEquals("value1", ctx.header("X-HEADER1").value());
      Assertions.assertEquals("value1", ctx.header("X-hEaDeR1").value());
      return "OK";
    })).ready(http -> http.header("x-header1", "value1").get("/", rsp -> {
      Assertions.assertEquals(200, rsp.code());
    }));
  }
}
