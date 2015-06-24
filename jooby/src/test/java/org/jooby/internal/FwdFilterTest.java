package org.jooby.internal;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class FwdFilterTest {

  @Test
  public void fwd() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, Route.Filter.class)
        .expect(unit -> {
          unit.get(Route.Filter.class)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        })
        .run(unit -> {
          new FwdFilter().fwd(unit.get(Route.Filter.class))
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }
}
