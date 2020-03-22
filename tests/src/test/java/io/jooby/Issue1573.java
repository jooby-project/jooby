package io.jooby;

import io.jooby.i1573.Controller1573;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1573 {

  @ServerTest
  public void issue1573(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("/edit/{id:[0-9]+}?", ctx -> ctx.path("id").value("own"));
      app.mvc(new Controller1573());
    }).ready(client -> {
      client.get("/edit", rsp -> {
        assertEquals("own", rsp.body().string());
      });

      client.get("/edit/123", rsp -> {
        assertEquals("123", rsp.body().string());
      });

      client.get("/profile", rsp -> {
        assertEquals("self", rsp.body().string());
      });

      client.get("/profile/123", rsp -> {
        assertEquals("123", rsp.body().string());
      });
    });
  }
}
