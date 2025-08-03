/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class Issue3741 {
  public static class RouterBase extends Jooby {
    {
      get("/3741/{id}", ctx -> "Path Var");

      get("/3741/static", ctx -> "Static");
    }
  }

  @Test
  public void shouldDetectDuplicatedRoutes() {
    var main = new Jooby();
    main.setRouterOptions(new RouterOptions().setFailOnDuplicateRoutes(true));
    main.mount(new RouterBase());
    var cause =
        assertThrows(
            IllegalArgumentException.class, () -> main.get("/3741/{id}", ctx -> "Something"));
    assertTrue(cause.getMessage().startsWith("Route already exists"));

    cause =
        assertThrows(
            IllegalArgumentException.class, () -> main.get("/3741/static", ctx -> "Static"));
    assertTrue(cause.getMessage().startsWith("Route already exists"));
  }

  @Test
  public void shouldDetectDuplicatedRoutesOnPaths() {
    var main = new Jooby();
    main.setRouterOptions(new RouterOptions().setFailOnDuplicateRoutes(true));
    main.mount("/path", new RouterBase());
    var cause =
        assertThrows(
            IllegalArgumentException.class, () -> main.get("/path/3741/{id}", ctx -> "Something"));
    assertTrue(cause.getMessage().startsWith("Route already exists"));

    cause =
        assertThrows(
            IllegalArgumentException.class, () -> main.get("/path/3741/static", ctx -> "Static"));
    assertTrue(cause.getMessage().startsWith("Route already exists"));
  }
}
