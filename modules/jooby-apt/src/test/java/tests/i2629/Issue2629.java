/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2629;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue2629 {

  @Test
  public void shouldSetMvcMethod() throws Exception {
    new ProcessorRunner(new C2629(), Map.of("jooby.mvcMethod", true))
        .withRouter(
            app -> {
              MockRouter router = new MockRouter(app);
              MockContext ctx = new MockContext();
              ctx.setQueryString("?type=foo&number=12&bool=true");
              assertEquals("foo:[12]:true", router.get("/2629", ctx).value().toString());
              var route = app.getRoutes().get(0);
              assertNotNull(route.getMvcMethod());
              try {
                assertEquals(
                    "foo:[14]:false",
                    route
                        .getMvcMethod()
                        .toMethodHandle()
                        .invoke(new C2629(), "foo", List.of(14), Boolean.FALSE));
              } catch (Throwable cause) {
                fail(cause);
              }
            });
  }
}
