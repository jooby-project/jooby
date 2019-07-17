package io.jooby;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1344 {

  public static class App1344 extends Jooby {
    {
      decorator(next -> ctx -> "<" + next.apply(ctx) + ">");

      get("/1344", Context::pathString);
    }
  }

  @Test
  @DisplayName("Decorator from composition")
  public void issue1338() {
    new JoobyRunner(app -> {

      app.decorator(next -> ctx -> "[" + next.apply(ctx) + "]");

      app.use(new App1344());
    }).ready(client -> {
      client.get("/1344", rsp -> {
        assertEquals("[</1344>]", rsp.body().string());
      });
    });
  }
}
