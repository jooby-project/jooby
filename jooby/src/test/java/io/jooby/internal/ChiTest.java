package io.jooby.internal;

import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChiTest {

  @Test
  public void routeOverride() {
    Chi router = new Chi();
    Route foo = route("GET", "/abcd", stringHandler("foo"));
    Route bar = route("GET", "/abcd", stringHandler("bar"));
    router.insert(foo);
    router.insert(bar);

    Router.Match result = router
        .find("GET", "/abcd");
    assertTrue(result.matches());
    assertEquals(bar, result.route());
  }

  @Test
  public void routeCase() {
    Chi router = new Chi();
    Route foo = route("GET", "/abcd", stringHandler("foo"));
    Route foos = route("GET", "/abcd/", stringHandler("foo/"));
    router.insert(foo);
    router.insert(foos);

    Router.Match result = router
        .find("GET", "/abcd/");
    assertTrue(result.matches());
    assertEquals(foos, result.route());
  }

  @Test
  public void wildOnRoot() throws Exception {
    Chi router = new Chi();

    router.insert(route("GET", "/foo/?*", stringHandler("foo")));
    router.insert(route("GET", "/bar/*", stringHandler("bar")));
    router.insert(route("GET", "/*", stringHandler("root")));

    find(router, "/", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("root", result.route().getPipeline().apply(ctx));
    });

    find(router, "/foo", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("foo", result.route().getPipeline().apply(ctx));
    });

    find(router, "/bar", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("root", result.route().getPipeline().apply(ctx));
    });

    find(router, "/foox", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("root", result.route().getPipeline().apply(ctx));
    });

    find(router, "/foo/", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("foo", result.route().getPipeline().apply(ctx));
    });

    find(router, "/foo/x", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("foo", result.route().getPipeline().apply(ctx));
    });

    find(router, "/bar/x", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("bar", result.route().getPipeline().apply(ctx));
    });
  }

  @Test
  public void searchString() throws Exception {
    Chi router = new Chi();

    // app.get("/regex/{zid:[0-9]+}/edit", ctx -> ctx.getRoute().getPathKeys());

    router.insert(route("GET", "/regex/{nid:[0-9]+}", stringHandler("nid")));
    router.insert(route("GET", "/regex/{zid:[0-9]+}/edit", stringHandler("zid")));
    router.insert(route("GET", "/articles/{id}", stringHandler("id")));
    router.insert(route("GET", "/articles/*", stringHandler("*")));

    find(router, "/regex/678/edit", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("zid", result.route().getPipeline().apply(ctx));
    });

    find(router, "/articles/tail/match", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("*", result.route().getPipeline().apply(ctx));
    });

    find(router, "/articles/123", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("id", result.route().getPipeline().apply(ctx));
    });
  }

  @Test
  public void searchParam() throws Exception {
    Chi router = new Chi();

    router.insert(route("GET", "/articles/{id}", stringHandler("id")));
    router.insert(route("GET", "/articles/*", stringHandler("catchall")));

    find(router, "/articles/123", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("id", result.route().getPipeline().apply(ctx));
    });

    find(router, "/articles/a/b", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("catchall", result.route().getPipeline().apply(ctx));
    });
  }

  @Test
  public void multipleRegex() {
    Chi router = new Chi();

    router.insert(route("GET", "/{lang:[a-z][a-z]}/{page:[^.]+}/", stringHandler("1515")));

    find(router, "/12/f/", (ctx, result) -> {
      assertFalse(result.matches());
      assertEquals(null, result.route().getPipeline().apply(ctx));
    });

    find(router, "/ar/page/", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("1515", result.route().getPipeline().apply(ctx));
    });

    find(router, "/arx/page/", (ctx, result) -> {
      assertFalse(result.matches());
      assertEquals(null, result.route().getPipeline().apply(ctx));
    });
  }

  @Test
  public void regexWithQuantity() {
    Chi router = new Chi();

    router.insert(route("GET", "/{lang:[a-z]{2}}/", stringHandler("qx")));

    find(router, "/12/", (ctx, result) -> {
      assertFalse(result.matches());
      assertEquals(null, result.route().getPipeline().apply(ctx));
    });

    find(router, "/ar/", (ctx, result) -> {
      assertTrue(result.matches());
      assertEquals("qx", result.route().getPipeline().apply(ctx));
    });
  }

  private void find(Chi router, String pattern,
      SneakyThrows.Consumer2<Context, Router.Match> consumer) {
    Router.Match result = router
        .find("GET", pattern);
    consumer.accept(ctx(pattern), result);
  }

  private Route.Handler stringHandler(String foo) {
    return ctx -> foo;
  }

  private Route route(String method, String pattern, Route.Handler handler) {
    return new Route(method, pattern, handler)
        .setEncoder(MessageEncoder.TO_STRING);
  }

  private Context ctx(String path) {
    Context context = mock(Context.class);
    when(context.getRequestPath()).thenReturn(path);
    when(context.getMethod()).thenReturn("GET");
    return context;
  }
}
