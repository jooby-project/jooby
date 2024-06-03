/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2525;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.apt.NewProcessorRunner;
import io.jooby.test.MockContext;
import io.jooby.test.MockRouter;

public class Issue2525 {
  @Test
  public void shouldAcceptEmptyList() throws Exception {
    new NewProcessorRunner(new C2525())
        .withRouter(
            app -> {
              var router = new MockRouter(app);
              var ctx = withQueryString("?");
              var list = ctx.query().toList(Foo2525.class);
              assertEquals("[]", router.get("/2525", withQueryString("?")).value());
              assertEquals("[]", router.get("/2525", withQueryString("?something=else")).value());
              assertEquals(
                  "[{a:10, b:20}, {a:30, b:40}]",
                  router
                      .get(
                          "/2525",
                          withQueryString("?foo[0][a]=10&foo[0][b]=20&foo[1][a]=30&foo[1][b]=40"))
                      .value());
            });
  }

  private Context withQueryString(String queryString) {
    return new MockContext().setQueryString(queryString);
  }
}
