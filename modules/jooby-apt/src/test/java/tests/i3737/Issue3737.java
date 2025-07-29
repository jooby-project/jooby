/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3737;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3737 {
  @Test
  public void shouldNotFail() throws Exception {
    // must compile
    new ProcessorRunner(new C3737())
        .withRouter(
            (app, source) -> {
              var routes = app.getRoutes();
              assertNotNull(routes);
              assertFalse(routes.isEmpty());
              var route = app.getRoutes().get(0);
              assertNotNull(route);
            });
  }
}
