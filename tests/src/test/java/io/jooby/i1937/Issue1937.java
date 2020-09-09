package io.jooby.i1937;

import io.jooby.Context;
import io.jooby.di.GuiceModule;
import io.jooby.exception.RegistryException;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

  @ServerTest
  public void shouldThrowIfOutOfScope(ServerTestRunner runner) {
    runner.define(app -> {
      app.setContextAsService(true);
      app.onStarted(() -> {
        Throwable t = assertThrows(RegistryException.class, () -> app.require(Context.class));
        assertEquals(t.getMessage(), "Context is not available. Are you getting it from request scope?");
      });
    }).ready(http -> {});
  }

  @ServerTest
  public void shouldThrowIfOutOfScopeWithDI(ServerTestRunner runner) {
    runner.define(app -> {
      app.install(new GuiceModule());
      app.setContextAsService(true);
      app.onStarted(() -> {
        Throwable t = assertThrows(RegistryException.class, () -> app.require(Context.class));
        assertEquals(t.getMessage(), "Context is not available. Are you getting it from request scope?");
      });
    }).ready(http -> {});
  }
}
