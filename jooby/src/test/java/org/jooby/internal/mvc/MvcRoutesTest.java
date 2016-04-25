package org.jooby.internal.mvc;

import static org.easymock.EasyMock.expect;

import java.util.List;

import org.jooby.Env;
import org.jooby.Route.Definition;
import org.jooby.internal.RouteMetadata;
import org.jooby.mvc.GET;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class MvcRoutesTest {

  public static class NoPath {

    @GET
    public void nopath() {

    }

  }

  @Test
  public void emptyConstructor() throws Exception {
    new MvcRoutes();

  }

  @Test(expected = IllegalArgumentException.class)
  public void nopath() throws Exception {
    new MockUnit(Env.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev").times(2);
        })
        .run(unit -> {
          Env env = unit.get(Env.class);
          List<Definition> routes = MvcRoutes.routes(env, new RouteMetadata(env), "", NoPath.class);
          System.out.println(routes);
        });
  }
}
