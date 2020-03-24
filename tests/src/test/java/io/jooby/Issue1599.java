package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1599 {

  @ServerTest
  public void issue1599(ServerTestRunner runner) {
    runner.define(app -> {
      app.path("/1599", () -> {

        app.get("/", ctx -> {
          return ctx.getRoute().getProduces().toString() + ctx.getRoute().getAttributes();
        });

        app.get("/{id}", ctx -> {
          return ctx.getRoute().getProduces().toString() + ctx.getRoute().getAttributes();
        });

      }).produces(MediaType.text)
          .attribute("foo", "bar");
    }).ready(client -> {
      client.get("/1599", rsp -> {
        assertEquals("[text/plain]{foo=bar}", rsp.body().string());
      });

      client.get("/1599/123", rsp -> {
        assertEquals("[text/plain]{foo=bar}", rsp.body().string());
      });
    });
  }
}
