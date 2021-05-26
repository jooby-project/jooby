package io.jooby.i2240;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.di.GuiceModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2240 {

  @ServerTest
  public void shouldShareRegistryOnAppComposite(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new GuiceModule());

      app.install(ChildApp2240::new);

      app.get("/main", ctx -> app.require(Service2240.class).foo());

    }).ready(http -> {
      http.get("/main", rsp -> {
        assertEquals("OK", rsp.body().string());
      });

      http.get("/child", rsp -> {
        assertEquals("OK", rsp.body().string());
      });
    });
  }

}
