package io.jooby;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import okhttp3.FormBody;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1687 {

  @ServerTest
  public void shouldUseHiddenMethod(ServerTestRunner runner) {
    runner.define(app -> {
      app.setHiddenMethod("_method");
      app.put("/1687", Context::getMethod);
    }).ready(client -> {
      client.post("/1687", new FormBody.Builder().add("_method", "put").build(), rsp -> {
        assertEquals("PUT", rsp.body().string());
      });

      // If _method is missing, then 405
      client.post("/1687", rsp -> {
        assertEquals(StatusCode.METHOD_NOT_ALLOWED.value(), rsp.code());
      });
    });
  }

  @ServerTest
  public void shouldUseHiddenMethodFromStrategy(ServerTestRunner runner) {
    runner.define(app -> {
      app.setHiddenMethod(ctx -> ctx.header("X-HTTP-Method-Override").toOptional());
      app.put("/1687", Context::getMethod);
    }).ready(client -> {
      client.header("X-HTTP-Method-Override", "put");
      client.post("/1687", rsp -> {
        assertEquals("PUT", rsp.body().string());
      });
    });
  }

}
