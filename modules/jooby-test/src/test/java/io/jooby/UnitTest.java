package io.jooby;

import io.reactivex.Single;
import org.junit.jupiter.api.Test;

import static io.jooby.StatusCode.NO_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnitTest {

  @Test
  public void unitTests() {
    Jooby app = new Jooby();

    app.get("/", ctx -> "OK");

    app.get("/{id}", ctx -> ctx.path("id").intValue());

    app.get("/search", ctx -> ctx.query("q").value("*:*"));

    app.delete("/{id}", ctx -> ctx.setResponseCode(NO_CONTENT));

    app.post("/", ctx -> ctx.body().value());

    app.post("/pojo", ctx -> ctx.body(PojoBody.class));

    MockRouter router = new MockRouter(app);

    assertEquals("OK", router.get("/").value());

    assertEquals("*:*", router.get("/search").value());

    assertEquals("foo", router.get("/search?q=foo").value());

    router.get("/", result -> {
      assertEquals(StatusCode.OK, result.getStatusCode());
      assertEquals("text/plain", result.getContentType().getValue());
      assertEquals(2, result.getContentLength());
      assertEquals("OK", result.value());
    });

    router.get("/123", result -> {
      assertEquals(StatusCode.OK, result.getStatusCode());
      assertEquals("text/plain", result.getContentType().getValue());
      assertEquals(3, result.getContentLength());
      assertEquals(123, result.value(Integer.class).intValue());
    });

    router.delete("/123", result -> {
      assertEquals(NO_CONTENT, result.getStatusCode());
    });

    String body = "{\"message\":\"ok\"}";
    router.post("/", new MockContext().setBody(body), result -> {
      assertEquals(body, result.value());
    });

    PojoBody pojo = new PojoBody();
    router.post("/pojo", new MockContext().setBody(pojo), result -> {
      assertEquals(pojo, result.value());
    });

    router.get("/x/notfound", new MockContext().setBody(pojo), result -> {
      assertEquals(StatusCode.NOT_FOUND, result.getStatusCode());
    });
  }

  @Test
  public void pipeline() {
    Jooby app = new Jooby();

    app.before(ctx -> ctx.setResponseHeader("before", "<"));
    app.after((ctx, result) -> ctx.setResponseHeader("after", ">"));
    app.get("/", ctx -> "OK");

    MockRouter router = new MockRouter(app)
        .setFullExecution(true);

    router.get("/", rsp -> {
      assertEquals("OK", rsp.value());
      assertEquals("<", rsp.getHeaders().get("before"));
      assertEquals(">", rsp.getHeaders().get("after"));
    });
  }

  @Test
  public void formdata() {
    Jooby app = new Jooby();

    app.post("/", ctx -> ctx.form("name").value());

    MockRouter router = new MockRouter(app);
    MockContext context = new MockContext();
    context.setForm(Formdata.create(context).put("name", "Easy Unit"));

    assertEquals("Easy Unit", router.post("/", context).value());
  }

  @Test
  public void formdataMock() {
    Jooby app = new Jooby();

    app.post("/", ctx -> ctx.form("name").value());

    Value name = mock(Value.class);
    when(name.value()).thenReturn("Easy Unit");

    Context context = mock(Context.class);
    when(context.form("name")).thenReturn(name);

    MockRouter router = new MockRouter(app);

    assertEquals("Easy Unit", router.post("/", context).value());
  }

  @Test
  public void rx() {
    Jooby app = new Jooby();

    app.get("/", ctx -> Single.fromCallable(() -> "rx"));

    MockRouter router = new MockRouter(app);

    assertEquals("rx", router.get("/").value(Single.class).blockingGet());
  }
}
