package io.jooby.internal;

import io.jooby.MockContext;
import io.jooby.Renderer;
import io.jooby.Route;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChiTest {

  @Test
  public void routeOverride() {
    $Chi router = new $Chi();
    RouteImpl foo = route("GET", "/abcd", stringHandler("foo"));
    RouteImpl bar = route("GET", "/abcd", stringHandler("bar"));
    router.insert(foo);
    router.insert(bar);

    RouterMatch result = router
        .find(new MockContext().setPathString("/abcd"), Renderer.TO_STRING,
            Collections.emptyList());
    assertTrue(result.matches);
    assertEquals(bar, result.route());
  }

  private Route.Handler stringHandler(String foo) {
    return ctx -> foo;
  }

  private RouteImpl route(String method, String pattern, Route.Handler handler) {
    return new RouteImpl(method, pattern, Collections.emptyList(), String.class, handler, handler,
        Renderer.TO_STRING, Collections.emptyMap());
  }
}
