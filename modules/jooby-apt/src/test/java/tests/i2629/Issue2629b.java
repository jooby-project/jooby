/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2629;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.jooby.apt.MvcModuleCompilerRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue2629b {

  @Test
  public void shouldSetMvcMethod() throws Exception {
    new MvcModuleCompilerRunner(new C2629b())
        .example(Expected2629b.class)
        .module(
            app -> {
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext();
              ctx.setQueryString("?s=foo&i=1&d=2&j=3&f=4&b=true");
              assertEquals("foo/1/2.0/GET/3/4.0/true", router.get("/2629", ctx).value().toString());
              var route = app.getRoutes().get(0);
              assertNotNull(route.getMvcMethod());
              assertEquals(
                  "bar/5/6.0/GET/7/8.0/false",
                  route.getMvcMethod().invoke(new C2629b(), "bar", 5, 6d, ctx, 7l, 8f, false));
            });
  }
}
