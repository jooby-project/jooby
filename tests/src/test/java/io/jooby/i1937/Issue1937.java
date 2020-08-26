package io.jooby.i1937;

import io.jooby.Context;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1937 {

  @ServerTest
  public void shouldFailIfContextAsServiceWasNotCalled(ServerTestRunner runner) {
    runner.define(app -> app.get("/i1937", ctx -> {
      app.require(Context.class);
      return "OK";
    })).ready(http -> http.get("/i1937", rsp -> assertEquals(500, rsp.code())));
  }

  @ServerTest
  public void shouldWorkIfContextAsServiceWasCalled(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/i1937", ctx -> {
        app.require(Context.class);
        return "OK";
      });

      app.setContextAsService(true);

    }).ready(http -> http.get("/i1937", rsp -> assertEquals(200, rsp.code())));
  }
}
