package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1344 {

  public static class App1344 extends Jooby {
    {
      decorator(next -> ctx -> "<" + next.apply(ctx) + ">");

      get("/1344", Context::getRequestPath);
    }
  }

  @ServerTest
  @DisplayName("Decorator from composition")
  public void issue1338(ServerTestRunner runner) {
    runner.define(app -> {

      app.decorator(next -> ctx -> "[" + next.apply(ctx) + "]");

      app.use(new App1344());
    }).ready(client -> {
      client.get("/1344", rsp -> {
        assertEquals("[</1344>]", rsp.body().string());
      });
    });
  }
}
