package org.jooby.issues;

import static org.easymock.EasyMock.expect;

import org.jooby.Env;
import org.jooby.Result;
import org.jooby.Results;
import org.jooby.internal.RouteMetadata;
import org.jooby.internal.mvc.MvcRoutes;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class Issue372 {

  public static class PingRoute {
    @Path("/ping")
    @GET
    private Result ping() {
      return Results.ok();
    }
  }

  public static class Ext extends PingRoute {
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailFastOnPrivateMvcRoutes() throws Exception {
    new MockUnit(Env.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev").times(2);
        })
        .run(unit -> {
          Env env = unit.get(Env.class);
          MvcRoutes.routes(env, new RouteMetadata(env), "", PingRoute.class);
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailFastOnPrivateMvcRoutesExt() throws Exception {
    new MockUnit(Env.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev").times(2);
        })
        .run(unit -> {
          Env env = unit.get(Env.class);
          MvcRoutes.routes(env, new RouteMetadata(env), "", Ext.class);
        });
  }

}
