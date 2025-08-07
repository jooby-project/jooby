/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3756;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3756 {
  @Test
  public void shouldCompile() throws Exception {
    // must compile
    new ProcessorRunner(new C3756((S3756) s -> {}))
        .withRouter(
            (app, source) -> {
              System.out.println(source);
              var routes = app.getRoutes();
              assertNotNull(routes);
              assertFalse(routes.isEmpty());
              var route = app.getRoutes().get(0);
              assertNotNull(route);
            });
  }
}
