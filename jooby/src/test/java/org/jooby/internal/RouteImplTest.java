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
import org.jooby.test.MockUnit;
import org.junit.Test;

public class RouteImplTest {

  @Test(expected = Err.class)
  public void notFound() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status()).andReturn(Optional.empty());
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
    Route route = new RouteImpl((req, rsp, chain) -> {
    } , "GET", "/path", "/p?th", "path", Collections.emptyMap(),
        MediaType.valueOf("html", "json"),
        MediaType.valueOf("json", "html"), Collections.emptyMap());

    assertEquals("GET /path\n" +
        "  pattern: /p?th\n" +
        "  name: path\n" +
        "  vars: {}\n" +
        "  consumes: [text/html, application/json]\n" +
        "  produces: [application/json, text/html]\n", route.toString());
  }

  @Test
  public void consumes() {
    Route route = new RouteImpl((req, rsp, chain) -> {
    } , "GET", "/path", "/p?th", "path", Collections.emptyMap(),
        MediaType.valueOf("html", "json"),
        MediaType.valueOf("json", "html"), Collections.emptyMap());

    assertEquals(MediaType.valueOf("html", "json"), route.consumes());
  }

}
