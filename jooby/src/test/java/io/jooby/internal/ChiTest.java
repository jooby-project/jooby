/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.MessageEncoder;
import io.jooby.Route;
import io.jooby.Router;

public class ChiTest {

  @Test
  public void routeOverride() {
    Chi router = new Chi(false);
    Route foo = route("GET", "/abcd", stringHandler("foo"));
    Route bar = route("GET", "/abcd", stringHandler("bar"));
    router.insert(foo);
    router.insert(bar);

    Router.Match result = router.find("GET", "/abcd");
    assertTrue(result.matches());
    assertEquals(bar, result.route());
  }

  @Test
  public void routeCase() {
    Chi router = new Chi(false);
    Route foo = route("GET", "/abcd", stringHandler("foo"));
    Route foos = route("GET", "/abcd/", stringHandler("foo/"));
    router.insert(foo);
    router.insert(foos);

    Router.Match result = router.find("GET", "/abcd/");
    assertTrue(result.matches());
    assertEquals(foos, result.route());
  }

  @Test
  public void staticMapExpansionAndOverrides() {
    Chi router = new Chi(false);

    // Add up to 8 routes to trigger StaticMap1 through StaticMapN
    for (int i = 1; i <= 8; i++) {
      router.insert(route("GET", "/path" + i, stringHandler("v" + i)));

      // Override the same path to trigger the `if (patternX.equals(path))` override branches
      for (int j = 1; j <= i; j++) {
        router.insert(route("GET", "/path" + j, stringHandler("v" + j + "_override")));
      }
    }

    assertTrue(router.exists("GET", "/path1"));
    assertTrue(router.exists("GET", "/path8"));
    assertFalse(router.exists("GET", "/path9")); // Missing
  }

  @Test
  public void multipleMethods() {
    Chi router = new Chi(false);
    router.insert(route("GET", "/multi", stringHandler("get")));
    router.insert(route("POST", "/multi", stringHandler("post")));
    router.insert(route("PUT", "/multi", stringHandler("put")));

    assertTrue(router.exists("GET", "/multi"));
    assertTrue(router.exists("POST", "/multi"));
    assertTrue(router.exists("PUT", "/multi"));
    assertFalse(router.exists("DELETE", "/multi"));

    // Check method not allowed correctly triggers
    Router.Match result = router.find("DELETE", "/multi");
    assertFalse(result.matches());
  }

  @Test
  public void nodeSplittingAndEdges() {
    Chi router = new Chi(false);
    // These will force the tree to split prefixes e.g. /a vs /ab vs /abc
    router.insert(route("GET", "/abc/d", stringHandler("1")));
    router.insert(route("GET", "/abc/e", stringHandler("2"))); // Splits at /abc/
    router.insert(route("GET", "/abx/y", stringHandler("3"))); // Splits at /ab

    assertTrue(router.find("GET", "/abc/d").matches());
    assertTrue(router.find("GET", "/abc/e").matches());
    assertTrue(router.find("GET", "/abx/y").matches());
  }

  @Test
  public void failOnDuplicateRoutes() {
    Chi router = new Chi(true); // Fail on duplicate is TRUE
    router.insert(route("GET", "/dup", stringHandler("1")));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              router.insert(route("GET", "/dup", stringHandler("2")));
            });
    assertTrue(ex.getMessage().contains("Route already exists: GET /dup"));
  }

  @Test
  public void wildOnRootAndCatchAllBase() throws Exception {
    Chi router = new Chi(false);

    router.insert(route("GET", "/foo/?*", stringHandler("foo"))); // Creates baseCatchAll /foo
    router.insert(route("GET", "/bar/*", stringHandler("bar")));
    router.insert(route("GET", "/*", stringHandler("root")));
    router.insert(route("GET", "/?*", stringHandler("base_catchall"))); // Converted to /*

    assertTrue(router.exists("GET", "/foo"));
    assertTrue(router.exists("GET", "/foo/bar"));
    assertTrue(router.exists("GET", "/bar/xyz"));
    assertTrue(router.exists("GET", "/anything"));
  }

  @Test
  public void searchStringAndRegexAutoAnchors() throws Exception {
    Chi router = new Chi(false);

    // Regex missing both ^ and $
    router.insert(route("GET", "/regex/{nid:[0-9]+}", stringHandler("nid")));
    // Regex missing $ only
    router.insert(route("GET", "/regex2/{zid:^[0-9]+}/edit", stringHandler("zid")));

    router.insert(route("GET", "/articles/{id}", stringHandler("id")));
    router.insert(route("GET", "/articles/*", stringHandler("*")));

    assertTrue(router.find("GET", "/regex/678").matches());
    assertTrue(router.find("GET", "/regex2/678/edit").matches());
    assertFalse(router.find("GET", "/regex/abc").matches()); // Regex fails

    // Test segment tail other than '/'
    router.insert(route("GET", "/file-{id}.jpg", stringHandler("file")));
    assertTrue(router.find("GET", "/file-123.jpg").matches());
  }

  @Test
  public void wildCardParsingExceptions() {
    Chi router = new Chi(false);

    // 1. Missing closing delimiter
    IllegalArgumentException ex1 =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              router.insert(route("GET", "/foo/{bar", stringHandler("err")));
            });
    assertTrue(ex1.getMessage().contains("missing"));

    // 2. Wildcard not at the end
    IllegalArgumentException ex2 =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              router.insert(route("GET", "/foo/*/bar", stringHandler("err")));
            });
    assertEquals(
        "Router: wildcard '*' must be the last element in a route. Found trailing segment in:"
            + " /foo/*/bar",
        ex2.getMessage());

    IllegalArgumentException ex3 =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              router.insert(route("GET", "/foo/*{bar}", stringHandler("err")));
            });
    assertEquals(
        "Router: wildcard '*' must be the last pattern in a route, otherwise use a '{param}'",
        ex3.getMessage());
  }

  @Test
  public void backtrackingAndMethodNotAllowed() {
    Chi router = new Chi(false);

    router.insert(route("GET", "/a/b/c", stringHandler("c")));
    router.insert(route("GET", "/a/{p}/d", stringHandler("d")));
    router.insert(route("POST", "/a/{p}/d", stringHandler("d-post")));

    // Matches static
    assertTrue(router.find("GET", "/a/b/c").matches());

    // Backtracks past the static `/b/c` branch to match the `{p}/d` branch
    assertTrue(router.find("GET", "/a/b/d").matches());

    // Matches but wrong method
    Router.Match result = router.find("PUT", "/a/b/d");
    assertFalse(result.matches());
  }

  @Test
  public void destroyAndEncoder() {
    Chi router = new Chi(false);
    MessageEncoder mockEncoder = mock(MessageEncoder.class);
    router.setEncoder(mockEncoder); // Coverage for setEncoder

    router.insert(route("GET", "/a/{b}", stringHandler("b")));
    router.insert(route("GET", "/x/y", stringHandler("y")));

    router.destroy(); // Coverage for internal Node.destroy() recursion
    assertNotNull(router);
  }

  private Route.Handler stringHandler(String foo) {
    return ctx -> foo;
  }

  private Route route(String method, String pattern, Route.Handler handler) {
    return new Route(method, pattern, handler).setEncoder(MessageEncoder.TO_STRING);
  }

  private Context ctx(String path) {
    Context context = mock(Context.class);
    when(context.getRequestPath()).thenReturn(path);
    when(context.getMethod()).thenReturn("GET");
    return context;
  }
}
