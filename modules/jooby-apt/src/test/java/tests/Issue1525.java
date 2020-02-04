package tests;

import io.jooby.Route;
import io.jooby.apt.MvcModuleCompilerRunner;
import org.junit.jupiter.api.Test;
import source.RouteClassAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1525 {
  @Test
  public void routeClassAttributes() throws Exception {
    new MvcModuleCompilerRunner(new RouteClassAttributes())
        .module(app -> {
          Route route0 = app.getRoutes().get(0);
          assertEquals(2, route0.getAttributes().size(), route0.getAttributes().toString());
          assertEquals("Admin", route0.attribute("roleAnnotation"));

          Route route1 = app.getRoutes().get(1);
          assertEquals(2, route1.getAttributes().size(), route1.getAttributes().toString());
          assertEquals("User", route1.attribute("roleAnnotation"));
        })
    ;
  }
}
