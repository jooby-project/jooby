package io.jooby;

import org.junit.jupiter.api.Test;

import static io.jooby.StatusCode.NO_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnitTest {

  @Test
  public void unitTests() {
    Jooby app = new Jooby();

    app.get("/", ctx -> "OK");

    app.get("/{id}", ctx -> ctx.path("id").intValue());

    app.get("/search", ctx -> ctx.query("q").value("*:*"));

    app.delete("/{id}", ctx -> ctx.setStatusCode(NO_CONTENT));

    app.post("/", ctx -> ctx.body().value());

    MockRouter router = new MockRouter(app);

    assertEquals("OK", router.get("/"));

    assertEquals("*:*", router.get("/search"));

    assertEquals("foo", router.get("/search?q=foo"));

    router.get("/", result -> {
      assertEquals(StatusCode.OK, result.getStatusCode());
      assertEquals("text/plain", result.getContentType().getValue());
      assertEquals(2, result.getContentLength());
      assertEquals("OK", result.getResult());
    });

    router.get("/123", result -> {
      assertEquals(StatusCode.OK, result.getStatusCode());
      assertEquals("text/plain", result.getContentType().getValue());
      assertEquals(3, result.getContentLength());
      assertEquals(123, result.getResult(Integer.class).intValue());
    });

    router.delete("/123", result -> {
      assertEquals(NO_CONTENT, result.getStatusCode());
    });

    String body = "{\"message\":\"ok\"}";
    router.post("/", new MockContext().setBody(body), result -> {
      assertEquals(body, result.getResult());
    });
  }

  @Test
  public void pipeline() {
    Jooby app = new Jooby();

    app.before(ctx -> ctx.attribute("prefix", "<"));
    app.after((ctx, result) -> result + ">");
    app.get("/", ctx -> ctx.attribute("prefix") + "OK");

    MockRouter router = new MockRouter(app)
        .setFullExecution(true);

    assertEquals("<OK>", router.get("/"));
  }
}
