package io.jooby.i2538;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.guice.GuiceModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.Jooby;

public class Issue2538 {

  interface A {
  }

  static class AImpl implements A {
  }

  static class AppA extends Jooby {
    {
      get("/mounted-does-not-work", ctx -> {
        final A a1 = ctx.require(A.class);
        final A a2 = require(A.class);
        return "OK";
      });
    }
  }

  @ServerTest
  public void shouldSharedRegistryOnMountedResources(ServerTestRunner runner) {
    runner.define(app -> {

      app.install(new GuiceModule(binder -> binder.bind(A.class).to(AImpl.class)));

      app.get("/hello", ctx -> "World");

      app.get("/not-mounted-works", ctx -> {
        final A a1 = ctx.require(A.class);
        final A a2 = ctx.require(A.class);
        return "OK";
      });

      app.mount(new AppA());

    }).ready(http -> {
      http
          .get("/mounted-does-not-work", rsp -> {
            assertEquals("OK", rsp.body().string());
          });
    });
  }
}
