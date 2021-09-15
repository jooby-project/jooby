package io.jooby.i2399;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.ServerOptions;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue2399 {

  @ServerTest
  public void shouldHttp2NotLostStreamIdOnException(ServerTestRunner runner) {
    runner.define(app -> {
      ServerOptions options = new ServerOptions();
      options.setHttp2(true);
      options.setSecurePort(8443);
      app.setServerOptions(options);

      app.error((ctx, cause, code) -> {
        ctx.send(cause.getMessage());
      });

      app.get("/2399", ctx -> ctx.query("q").value());

    }).ready((http, https) -> {
      https.get("/2399", rsp -> {
        assertEquals("Missing value: 'q'", rsp.body().string());
      });

      https.get("/2399/404", rsp -> {
        assertEquals("/2399/404", rsp.body().string());
      });
    });
  }

}
