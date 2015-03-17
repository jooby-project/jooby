package org.jooby.internal.mvc;

import static org.easymock.EasyMock.expect;

import org.jooby.Env;
import org.jooby.MockUnit;
import org.jooby.internal.RouteMetadata;
import org.jooby.mvc.GET;
import org.jooby.mvc.Path;
import org.junit.Test;

public class MvcRoutesTest {

  @Path("/")
  public static class NoPublicMethod {

    @GET
    private void privateMethod() {

    }

  }

  public static class NoPath {

    @GET
    public void nopath() {

    }

  }

  @Test(expected = IllegalArgumentException.class)
  public void noPublicMethod() throws Exception {
    new MockUnit(Env.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .run(unit -> {
          Env env = unit.get(Env.class);
          MvcRoutes.routes(env, new RouteMetadata(env), NoPublicMethod.class);
        });
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
          MvcRoutes.routes(env, new RouteMetadata(env), NoPath.class);
        });
  }
}
