package org.jooby.issues;

import org.jooby.Jooby;
import org.jooby.Route;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class Issue187 extends ServerFeature {

  public static class RouteBean1 {

    @GET
    @Path("/1/foo")
    public String name(final Route route) {
      return route.name();
    }
  }

  public static class RouteBean2 {

    @GET
    @Path("/2/foo")
    public String name(final Route route) {
      return route.name();
    }
  }

  public static class App extends Jooby {

    public App(final String prefix) {
      super(prefix);

      get("/foo", req -> req.route().name())
          .name("foo");

      use(RouteBean2.class);
    }

  }

  public Issue187() {
    super("187");

    use(new App("bar"));

    use(RouteBean1.class);
  }

  @Test
  public void renameRoutesViaPrefix() throws Exception {
    request().get("/foo")
        .expect("/bar/foo");

    request().get("/1/foo")
        .expect("/187/RouteBean1.name");

    request().get("/2/foo")
        .expect("/bar/RouteBean2.name");
  }

}
