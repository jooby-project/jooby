package org.jooby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jooby.Route.Definition;
import org.jooby.internal.RouteImpl;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class RouteDefinitionTest {

  @Test
  public void newHandler() throws Exception {
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
              MediaType.ALL)).get();

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
              MediaType.ALL)).get();

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
              MediaType.ALL)).get();

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
              MediaType.ALL)).get();

          route.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        });
  }

  @Test
  public void toStr() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .run(unit -> {
          Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
          }).excludes("/**/logout");

          assertEquals("GET /\n" +
              "  name: /anonymous\n" +
              "  excludes: [/**/logout]\n" +
              "  consumes: [*/*]\n" +
              "  produces: [*/*]\n", def.toString());
        });
  }

  @Test
  public void attributes() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .run(unit -> {
          Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
          }).attr("foo", "bar");

          assertEquals("bar", def.attr("foo"));
          assertEquals("{foo=bar}", def.attributes().toString());
        });
  }

  @Test
  public void rendererAttr() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class)
        .run(unit -> {
          Definition def = new Route.Definition("GET", "/", (req, rsp, chain) -> {
          }).renderer("json");

          assertEquals("json", def.attr("renderer"));
          assertEquals("{renderer=json}", def.attributes().toString());
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

    assertEquals("/test", def.name());
    assertEquals("/test/path", def.pattern());
    assertEquals("PUT", def.method());
    assertEquals(MediaType.json, def.consumes().get(0));
    assertEquals(MediaType.json, def.produces().get(0));
  }

  @Test
  public void reverse() throws Exception {
    Function<String, Route.Definition> route = path -> new Route.Definition("*", path, () -> null);
    assertEquals("/1", route.apply("/:id").reverse(1));

    assertEquals("/cat/1", route.apply("/:type/:id").reverse("cat", 1));

    assertEquals("/cat/5", route.apply("/{type}/{id}").reverse("cat", 5));

    assertEquals("/ccat/1",
        route.apply("/c{type}/{id}").reverse(ImmutableMap.of("type", "cat", "id", 1)));

    assertEquals("/cat/tom", route.apply("/cat/tom").reverse("cat", 1));
  }

  @Test
  public void attrs() throws Exception {
    Function<String, Route.Definition> route = path -> new Route.Definition("*", path, () -> null);
    Route.Definition r = route.apply("/")
        .attr("i", 7)
        .attr("s", "string")
        .attr("enum", Status.OK)
        .attr("type", Route.class);

    assertEquals(Integer.valueOf(7), r.attr("i"));
    assertEquals("string", r.attr("s"));
    assertEquals(Status.OK, r.attr("enum"));
    assertEquals(Route.class, r.attr("type"));
  }

  @Test
  public void glob() throws Exception {
    Function<String, Route.Definition> route = path -> new Route.Definition("*", path, () -> null);

    assertEquals(false, route.apply("/").glob());
    assertEquals(false, route.apply("/static").glob());
    assertEquals(true, route.apply("/t?st").glob());
    assertEquals(true, route.apply("/*/id").glob());
    assertEquals(true, route.apply("*").glob());
    assertEquals(true, route.apply("/public/**").glob());
  }

  @Test
  public void attrsArray() throws Exception {
    Function<String, Route.Definition> route = path -> new Route.Definition("*", path, () -> null);
    Route.Definition r = route.apply("/")
        .attr("i", new int[]{7 });

    assertTrue(Arrays.equals(new int[]{7 }, (int[]) r.attr("i")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void attrUnsupportedType() throws Exception {
    Function<String, Route.Definition> route = path -> new Route.Definition("*", path, () -> null);
    route.apply("/").attr("i", new Object());
  }

}
