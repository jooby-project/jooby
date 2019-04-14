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

      app.delete("/{id}", ctx -> ctx.setStatusCode(NO_CONTENT));

      app.post("/", ctx -> ctx.body().value());

    }).apply(router -> {

      assertEquals("OK", router.get("/"));

      router.get("/", result -> {
        assertEquals(StatusCode.OK, result.getStatusCode());
        assertEquals("text/plain", result.getContentType().getValue());
        assertEquals(2, result.getContentLength());
        assertEquals("OK", result.getValue());
      });

      router.get("/123", result -> {
        assertEquals(StatusCode.OK, result.getStatusCode());
        assertEquals("text/plain", result.getContentType().getValue());
        assertEquals(3, result.getContentLength());
        assertEquals(123, result.getValue());
      });

      router.delete("/123", result -> {
        assertEquals(NO_CONTENT, result.getStatusCode());
      });

      String body = "{\"message\":\"ok\"}";
      router.post("/", ctx -> ctx.setBody(body), result -> {
        assertEquals(body, result.getValue());
      });
    });

  }
}
