/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.Route;
import io.jooby.apt.NewProcessorRunner;
import source.RouteClassAttributes;

public class Issue1525 {
  @Test
  public void routeClassAttributes() throws Exception {
    new NewProcessorRunner(new RouteClassAttributes())
        .withRouter(
            app -> {
              Route route0 = app.getRoutes().get(0);
              assertEquals(2, route0.getAttributes().size(), route0.getAttributes().toString());
              assertEquals("Admin", route0.attribute("roleAnnotation"));

              Route route1 = app.getRoutes().get(1);
              assertEquals(2, route1.getAttributes().size(), route1.getAttributes().toString());
              assertEquals("User", route1.attribute("roleAnnotation"));
            });
  }
}
