package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jooby.Route.Definition;
import org.jooby.internal.RouteImpl;
import org.junit.Test;

public class RouteDefinitionTest {

  @Test
  public void newHandler() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);

          Response rsp = unit.get(Response.class);
          rsp.send("x");

          Route.Chain chain = unit.get(Route.Chain.class);

          chain.next(req, rsp);
        })
        .run(unit -> {
          Definition def = new Route.Definition("GET", "/", (req, rsp) -> {
            rsp.send("x");
          });

          RouteImpl route = (RouteImpl) (def.matches("GET", "/", MediaType.all,
              MediaType.ALL).get());

          route.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  @Test
  public void newOneArgHandler() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);

          Response rsp = unit.get(Response.class);
          rsp.send("x");

          Route.Chain chain = unit.get(Route.Chain.class);

          chain.next(req, rsp);
        })
        .run(unit -> {
          Definition def = new Route.Definition("GET", "/", (req) -> {
            return "x";
          });

          RouteImpl route = (RouteImpl) (def.matches("GET", "/", MediaType.all,
              MediaType.ALL).get());

          route.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  @Test
  public void newZeroArgHandler() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);

          Response rsp = unit.get(Response.class);
          rsp.send("x");

          Route.Chain chain = unit.get(Route.Chain.class);

          chain.next(req, rsp);
        })
        .run(unit -> {
          Definition def = new Route.Definition("GET", "/", () -> {
            return "x";
          });

          RouteImpl route = (RouteImpl) (def.matches("GET", "/", MediaType.all,
              MediaType.ALL).get());

          route.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  @Test
  public void newFilter() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send("x");

        })
        .run(unit -> {
          Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
            rsp.send("x");
          });

          RouteImpl route = (RouteImpl) (def.matches("GET", "/", MediaType.all,
              MediaType.ALL).get());

          route.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  @Test
  public void toStr() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .expect(unit -> {
        })
        .run(unit -> {
          Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
          });

          assertEquals("GET /\n" +
              "  name: anonymous\n" +
              "  consume: [*/*]\n" +
              "  produces: [*/*]\n", def.toString());
        });
  }

  @Test(expected = NullPointerException.class)
  public void nullVerb() throws Exception {
    new Route.Definition(null, "/", (req, rsp, chain) -> {
    });
  }

  @Test
  public void noMatches() throws Exception {
    Optional<Route> matches = new Route.Definition("delete", "/", (req, rsp, chain) -> {
    }).matches("POST", "/", MediaType.all, MediaType.ALL);
    assertEquals(Optional.empty(), matches);
  }

  @Test
  public void chooseMostSpecific() throws Exception {
    Optional<Route> matches = new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).matches("GET", "/", MediaType.all, Arrays.asList(MediaType.json));
    assertEquals(true, matches.isPresent());
  }

  @Test
  public void consumesMany() throws Exception {
    Route.Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).consumes("text/*", "json")
        .produces("json");
    assertEquals(MediaType.json, def.consumes().get(0));
    assertEquals(MediaType.valueOf("text/*"), def.consumes().get(1));

    assertEquals(true, def.matches("GET", "/", MediaType.all, MediaType.ALL)
        .isPresent());
    assertEquals(true, def.matches("GET", "/", MediaType.json, MediaType.ALL)
        .isPresent());
    assertEquals(false, def.matches("GET", "/", MediaType.xml, MediaType.ALL)
        .isPresent());
    assertEquals(false,
        def.matches("GET", "/", MediaType.json, Arrays.asList(MediaType.html))
        .isPresent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void consumesEmpty() throws Exception {
    new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).consumes(Collections.emptyList());
  }

  @Test(expected = IllegalArgumentException.class)
  public void consumesNull() throws Exception {
    new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).consumes((List<MediaType>) null);
  }

  @Test
  public void consumesOne() throws Exception {
    Route.Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).consumes("json");
    assertEquals(MediaType.json, def.consumes().get(0));
  }

  @Test
  public void canConsume() throws Exception {
    Route.Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).consumes("json");
    assertEquals(true, def.canConsume("json"));
    assertEquals(false, def.canConsume("html"));
    assertEquals(true, def.canConsume(MediaType.json));
    assertEquals(false, def.canConsume(MediaType.html));
  }

  @Test
  public void producesMany() throws Exception {
    Route.Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).produces("text/*", "json");
    assertEquals(MediaType.json, def.produces().get(0));
    assertEquals(MediaType.valueOf("text/*"), def.produces().get(1));

    assertEquals(true, def.matches("GET", "/", MediaType.all, MediaType.ALL)
        .isPresent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void producesEmpty() throws Exception {
    new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).produces(Collections.emptyList());
  }

  @Test(expected = IllegalArgumentException.class)
  public void producesNull() throws Exception {
    new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).produces((List<MediaType>) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void nullName() throws Exception {
    new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).name(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyName() throws Exception {
    new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).name("");
  }

  @Test
  public void producesOne() throws Exception {
    Route.Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).produces("json");
    assertEquals(MediaType.json, def.produces().get(0));
  }

  @Test
  public void canProduce() throws Exception {
    Route.Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
    }).produces("json", "html");
    assertEquals(true, def.canProduce("json"));
    assertEquals(true, def.canProduce("html"));
    assertEquals(true, def.canProduce(MediaType.json));
    assertEquals(true, def.canProduce(MediaType.html));
    assertEquals(false, def.canProduce("xml"));
  }

  @Test
  public void properties() throws Exception {
    Route.Definition def = new Route.Definition("put", "/test/path", (req, rsp, chain) -> {
    })
        .name("test")
        .consumes(MediaType.json)
        .produces(MediaType.json);

    assertEquals("test", def.name());
    assertEquals("/test/path", def.pattern());
    assertEquals("PUT", def.method());
    assertEquals(MediaType.json, def.consumes().get(0));
    assertEquals(MediaType.json, def.produces().get(0));
  }
}
