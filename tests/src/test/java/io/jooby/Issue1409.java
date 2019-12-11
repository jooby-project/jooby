package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1409 {

  @ServerTest
  public void shouldKeepDeveloperProvidedPath(ServerTestRunner runner) {
    runner.define(app -> {
      app.get("//doubleslash", Context::getRequestPath);

      app.path("//path", () -> {
        app.get("//", Context::getRequestPath);
      });

      app.path("/context", () -> {
        app.get("/", Context::getRequestPath);

        app.get("//", Context::getRequestPath);
      });

      app.get("/trailing/", Context::getRequestPath);

      app.get("/trailing", ctx -> "/trailing!");
    }).ready(client -> {
      client.get("//doubleslash", rsp -> {
        assertEquals("//doubleslash", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("//path//", rsp -> {
        assertEquals("//path//", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("/trailing/", rsp -> {
        assertEquals("/trailing/", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("/trailing", rsp -> {
        assertEquals("/trailing!", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("/context", rsp -> {
        assertEquals("/context", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("/context//", rsp -> {
        assertEquals("/context//", rsp.body().string());
        assertEquals(200, rsp.code());
      });
    });
  }

  @ServerTest
  public void shouldNormRequestPath(ServerTestRunner runner) {
    runner.define(app -> {
      app.setRouterOptions(RouterOption.NORM, RouterOption.NO_TRAILING_SLASH);
      app.get("/doubleslash", Context::getRequestPath);

      app.path("/path", () -> {
        app.get("/", Context::getRequestPath);
      });

      app.path("/context", () -> {
        app.get("/", Context::getRequestPath);
      });

      app.get("/trailing", Context::getRequestPath);

    }).ready(client -> {
      client.get("//doubleslash", rsp -> {
        assertEquals("//doubleslash", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("//path//", rsp -> {
        assertEquals("//path//", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("/trailing/", rsp -> {
        assertEquals("/trailing/", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("/trailing", rsp -> {
        assertEquals("/trailing", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("/context", rsp -> {
        assertEquals("/context", rsp.body().string());
        assertEquals(200, rsp.code());
      });

      client.get("/context//", rsp -> {
        assertEquals("/context//", rsp.body().string());
        assertEquals(200, rsp.code());
      });
    });
  }
}
