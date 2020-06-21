package io.jooby.i1805;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1805 {
  @ServerTest
  public void shouldParseURLLikeParam(ServerTestRunner runner) {
    runner.define(app -> {
      app.mvc(new C1805());
    }).ready(client -> {
      client.get("/1805/uri?param=https://jooby.io", rsp -> {
        assertEquals("https://jooby.io", rsp.body().string());
      });
      client.get("/1805/url?param=https://jooby.io", rsp -> {
        assertEquals("https://jooby.io", rsp.body().string());
      });
    });
  }
}
