package io.jooby.i1786;

import io.jooby.StatusCode;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1786 {
  @ServerTest
  public void shouldFollowNonNullAnnotation(ServerTestRunner runner) {
    runner.define(app -> {
      app.mvc(new Controller1786());
    }).ready(client -> {
      client.get("/1786/nonnull", rsp -> {
        assertEquals(StatusCode.BAD_REQUEST_CODE, rsp.code());
      });
    });
  }
}
