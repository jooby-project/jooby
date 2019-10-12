package io.jooby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HttpsTest {

  @Test
  public void https() {
    new JoobyRunner(app -> {

      app.setServerOptions(new ServerOptions().setSecurePort(8433));
      app.get("/",
          ctx -> "schema: " + ctx.getScheme() + "; protocol: " + ctx.getProtocol() + "; secure: "
              + ctx.isSecure());
    }).ready((http, https, server) -> {
      https.get("/", rsp -> {
        assertEquals("schema: https; protocol: HTTP/1.1; secure: true", rsp.body().string());
      });
      http.get("/", rsp -> {
        assertEquals("schema: http; protocol: HTTP/1.1; secure: false", rsp.body().string());
      });
    });

  }
}
