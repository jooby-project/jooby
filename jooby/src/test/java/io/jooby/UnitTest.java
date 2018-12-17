package io.jooby;

import org.junit.jupiter.api.Test;

import static io.jooby.StatusCode.NO_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnitTest {

  @Test
  public void unitTests() {
    new MockRouter(app -> {

      app.get("/", ctx -> "OK");

      app.get("/{id}", ctx -> ctx.path("id").intValue());

      app.delete("/{id}", ctx -> ctx.statusCode(NO_CONTENT));

      app.post("/", ctx -> ctx.body().value());

    }).apply(router -> {

      assertEquals("OK", router.get("/"));

      router.get("/", result -> {
        assertEquals(200, result.statusCode());
        assertEquals("text/plain;charset=utf-8", result.contentType());
        assertEquals(2, result.contentLength());
        assertEquals("OK", result.value());
      });

      router.get("/123", result -> {
        assertEquals(200, result.statusCode());
        assertEquals("text/plain;charset=utf-8", result.contentType());
        assertEquals(3, result.contentLength());
        assertEquals(123, result.value());
      });

      router.delete("/123", result -> {
        assertEquals(204, result.statusCode());
      });

      String body = "{\"message\":\"ok\"}";
      router.post("/", ctx -> ctx.setBody(body), result -> {
        assertEquals(body, result.value());
      });
    });

  }
}
