package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Optional;

import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Source;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class RouteImplTest {

  @Test(expected = Err.class)
  public void notFound() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status()).andReturn(Optional.empty());

          Request req = unit.get(Request.class);
          expect(req.path(true)).andReturn("/x");
        })
        .run(unit -> {
          RouteImpl.notFound("GET", "/x", MediaType.ALL)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @Test
  public void statusSetOnNotFound() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status()).andReturn(Optional.of(org.jooby.Status.OK));
        })
        .run(unit -> {
          RouteImpl.notFound("GET", "/x", MediaType.ALL)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @Test
  public void toStr() {
    Route.Filter f = (req, rsp, chain) -> {
    };
    Route route = new RouteImpl(f, new Route.Definition("GET", "/p?th", f)
        .name("path")
        .consumes("html", "json"), "GET", "/path", MediaType.valueOf("json", "html"),
        Collections.emptyMap(), null, Source.UNKNOWN);

    assertEquals("| Method | Path  | Source      | Name  | Pattern | Consumes                      | Produces                      |\n" +
        "|--------|-------|-------------|-------|---------|-------------------------------|-------------------------------|\n" +
        "| GET    | /path | ~unknown:-1 | /path | /p?th   | [text/html, application/json] | [application/json, text/html] |", route.toString());
  }

  @Test
  public void consumes() {
    Route.Filter f = (req, rsp, chain) -> {
    };
    Route route = new RouteImpl(f, new Route.Definition("GET", "/p?th", f).consumes("html", "json"),
        "GET", "/path", Collections.emptyList(), Collections.emptyMap(), null, Source.UNKNOWN);

    assertEquals(MediaType.valueOf("html", "json"), route.consumes());
  }

}
