package io.jooby.i1905;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue1905 {

  @ServerTest
  public void shouldInstallApp(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(A1905::new);
      app.install("/b", B1905::new);

    }).ready(http -> {
      http.get("/a", rsp -> assertEquals("AService1905;2", rsp.body().string()));

      http.get("/b/b", rsp -> assertEquals("BService1905;2", rsp.body().string()));
    });
  }
}
