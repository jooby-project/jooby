package io.jooby.i1806;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1806 {
  @ServerTest
  public void shouldNotGetListWithNullValue(ServerTestRunner runner) {
    runner.define(app -> {
      app.mvc(new C1806());
      app.get("/1806/s", ctx -> ctx.query("names").toList(String.class));
    }).ready(client -> {
      client.get("/1806/s", rsp -> {
        assertEquals("[]", rsp.body().string());
      });
      client.get("/1806/c", rsp -> {
        assertEquals("[]", rsp.body().string());
      });
    });
  }
}
