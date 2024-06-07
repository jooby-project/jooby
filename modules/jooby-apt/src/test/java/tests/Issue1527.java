/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.jooby.Route;
import io.jooby.apt.ProcessorRunner;
import source.Controller1527;
import source.TopEnum;

public class Issue1527 {
  @Test
  public void annotation() throws Exception {
    new ProcessorRunner(new Controller1527())
        .withRouter(
            app -> {
              Route route0 = app.getRoutes().get(0);
              assertEquals(2, route0.getAttributes().size(), route0.getAttributes().toString());
              assertEquals(Controller1527.Role.ADMIN, route0.attribute("requireRole"));
              assertEquals(Arrays.asList(TopEnum.FOO), route0.attribute("topAnnotation"));

              Route route1 = app.getRoutes().get(1);
              assertEquals(1, route1.getAttributes().size(), route1.getAttributes().toString());
              assertEquals(
                  Arrays.asList(TopEnum.BAR, TopEnum.FOO), route1.attribute("topAnnotation"));

              Route route2 = app.getRoutes().get(2);
              assertEquals(2, route2.getAttributes().size(), route2.getAttributes().toString());
              assertEquals(Arrays.asList(TopEnum.FOO), route2.attribute("topAnnotation"));
              assertEquals(Arrays.asList("a", "b", "c"), route2.attribute("stringArrayAnnotation"));
            });
  }
}
