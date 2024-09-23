/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3539;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3539 {

  @Test
  public void shouldGenerateAnnotationWithDefaultValue() throws Exception {
    new ProcessorRunner(new C3539())
        .withRouter(
            app -> {
              var routes = app.getRoutes();
              assertNotNull(routes);
              assertFalse(routes.isEmpty());
              var route = app.getRoutes().get(0);
              assertNotNull(route);
              Map<String, Object> attributes = route.getAttributes();
              assertNotNull(attributes);
              assertFalse(attributes.isEmpty());
              var secured = attributes.get(Secured3525.class.getSimpleName());
              assertSame(secured, Boolean.TRUE);
            });
  }
}
