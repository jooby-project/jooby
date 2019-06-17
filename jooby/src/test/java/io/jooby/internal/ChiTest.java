package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Renderer;
import io.jooby.Route;
import io.jooby.Sneaky;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChiTest {

  @Test
  public void routeOverride() {
    $Chi router = new $Chi();
    Route foo = route("GET", "/abcd", stringHandler("foo"));
    Route bar = route("GET", "/abcd", stringHandler("bar"));
    router.insert(foo);
    router.insert(bar);

    RouterMatch result = router
        .find(ctx("/abcd"), Renderer.TO_STRING,
            Collections.emptyList());
    assertTrue(result.matches);
    assertEquals(bar, result.route());
  }

  private Context ctx(String path) {
    Context context = mock(Context.class);
    when(context.pathString()).thenReturn(path);
    when(context.getMethod()).thenReturn("GET");
    return context;
  }

  @Test
  public void routeCase() {
    $Chi router = new $Chi();
    router.setIgnoreTrailingSlash(false);
    Route foo = route("GET", "/abcd", stringHandler("foo"));
    Route foos = route("GET", "/abcd/", stringHandler("foo/"));
    router.insert(foo);
    router.insert(foos);

    RouterMatch result = router
        .find(ctx("/abcd/"), Renderer.TO_STRING,
            Collections.emptyList());
    assertTrue(result.matches);
    assertEquals(foos, result.route());
  }

  @Test
  public void wildOnRoot() throws Exception {
    $Chi router = new $Chi();

    router.insert(route("GET", "/foo/?*", stringHandler("foo")));
    router.insert(route("GET", "/bar/*", stringHandler("bar")));
    router.insert(route("GET", "/*", stringHandler("root")));

    find(router, "/", (ctx, result) -> {
      assertTrue(result.matches);
      assertEquals("root", result.route().getPipeline().apply(ctx));
    });

    find(router, "/foo", (ctx, result) -> {
      assertTrue(result.matches);
      assertEquals("foo", result.route().getPipeline().apply(ctx));
    });

    find(router, "/bar", (ctx, result) -> {
      assertTrue(result.matches);
      assertEquals("root", result.route().getPipeline().apply(ctx));
    });

    find(router, "/foox", (ctx, result) -> {
      assertTrue(result.matches);
      assertEquals("root", result.route().getPipeline().apply(ctx));
    });

    find(router, "/foo/", (ctx, result) -> {
      assertTrue(result.matches);
      assertEquals("foo", result.route().getPipeline().apply(ctx));
    });

    find(router, "/foo/x", (ctx, result) -> {
      assertTrue(result.matches);
      assertEquals("foo", result.route().getPipeline().apply(ctx));
    });

    find(router, "/bar/x", (ctx, result) -> {
      assertTrue(result.matches);
      assertEquals("bar", result.route().getPipeline().apply(ctx));
    });
  }

  private void find($Chi router, String pattern,
      Sneaky.Consumer2<Context, RouterMatch> consumer) {
    Context rootctx = ctx(pattern);
    RouterMatch result = router
        .find(rootctx, Renderer.TO_STRING, Collections.emptyList());
    consumer.accept(rootctx, result);
  }

  private Route.Handler stringHandler(String foo) {
    return ctx -> foo;
  }

  private Route route(String method, String pattern, Route.Handler handler) {
    return new Route(method, pattern, String.class, handler, null, null, null,
        Renderer.TO_STRING, Collections.emptyMap());
  }

}
