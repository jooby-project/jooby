package io.jooby;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1413 {

  @Test
  public void shouldDoPreflight() {
    new JoobyRunner(app -> {
      app.decorator(new CorsHandler(new Cors().setMethods("*")));

      app.put("/api/v1/machines/{key}", ctx -> ctx.path("key").value());

    }).ready(client -> {
      client
          .header("Origin", "http://foo.com")
          .header("Access-Control-Request-Method", "PUT")
          .options("/api/v1/machines/123", rsp -> {
            assertEquals("", rsp.body().string());
            assertEquals(200, rsp.code());
            assertEquals("http://foo.com", rsp.header("Access-Control-Allow-Origin"));
            assertEquals("true", rsp.header("Access-Control-Allow-Credentials"));
          });

      client
          .header("Origin", "http://foo.com")
          .put("/api/v1/machines/123", rsp -> {
            assertEquals("123", rsp.body().string());
            assertEquals(200, rsp.code());
            assertEquals(null, rsp.header("Access-Control-Allow-Origin"));
            assertEquals(null, rsp.header("Access-Control-Allow-Credentials"));
          });
    });
  }
}
