package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooby.test.MockUnit;
import org.junit.Test;

public class RouteForwardingTest {

  @Test
  public void consumes() throws Exception {
    List<MediaType> consumes = Arrays.asList(MediaType.js);
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.consumes()).andReturn(consumes);
        })
        .run(unit -> {
          assertEquals(consumes, new Route.Forwarding(unit.get(Route.class)).consumes());
        });
  }

  @Test
  public void produces() throws Exception {
    List<MediaType> produces = Arrays.asList(MediaType.js);
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.produces()).andReturn(produces);
        })
        .run(unit -> {
          assertEquals(produces, new Route.Forwarding(unit.get(Route.class)).produces());
        });
  }

  @Test
  public void name() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.name()).andReturn("xXX");
        })
        .run(unit -> {
          assertEquals("xXX", new Route.Forwarding(unit.get(Route.class)).name());
        });
  }

  @Test
  public void path() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.path()).andReturn("/xXX");
        })
        .run(unit -> {
          assertEquals("/xXX", new Route.Forwarding(unit.get(Route.class)).path());
        });
  }

  @Test
  public void pattern() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.pattern()).andReturn("/**/*");
        })
        .run(unit -> {
          assertEquals("/**/*", new Route.Forwarding(unit.get(Route.class)).pattern());
        });
  }

  @Test
  public void attributes() throws Exception {
    Map<String, Object> attributes = new HashMap<>();
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.attributes()).andReturn(attributes);
        })
        .run(unit -> {
          assertEquals(attributes, new Route.Forwarding(unit.get(Route.class)).attributes());
        });
  }

  @Test
  public void attr() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.attr("foo")).andReturn("bar");
        })
        .run(unit -> {
          assertEquals("bar", new Route.Forwarding(unit.get(Route.class)).attr("foo"));
        });
  }

  @Test
  public void renderer() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.renderer()).andReturn("text");
        })
        .run(unit -> {
          assertEquals("text", new Route.Forwarding(unit.get(Route.class)).renderer());
        });
  }

  @Test(expected = NullPointerException.class)
  public void nullRoute() throws Exception {
    new Route.Forwarding(null);
  }

  @Test
  public void toStr() throws Exception {
    new MockUnit(Route.class)
        .run(unit -> {
          assertEquals(unit.get(Route.class).toString(),
              new Route.Forwarding(unit.get(Route.class)).toString());
        });
  }

  @Test
  public void verb() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.method()).andReturn("OPTIONS");
        })
        .run(unit -> {
          assertEquals("OPTIONS", new Route.Forwarding(unit.get(Route.class)).method());
        });
  }

  @Test
  public void vars() throws Exception {
    Map<Object, String> vars = new HashMap<>();
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.vars()).andReturn(vars);
        })
        .run(unit -> {
          assertEquals(vars, new Route.Forwarding(unit.get(Route.class)).vars());
        });
  }

  @Test
  public void glob() throws Exception {
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.glob()).andReturn(true);
        })
        .run(unit -> {
          assertEquals(true, new Route.Forwarding(unit.get(Route.class)).glob());
        });
  }

  @Test
  public void reverseMap() throws Exception {
    Map<String, Object> vars = new HashMap<>();
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.reverse(vars)).andReturn("/");
        })
        .run(unit -> {
          assertEquals("/", new Route.Forwarding(unit.get(Route.class)).reverse(vars));
        });
  }

  @Test
  public void reverseVars() throws Exception {
    Object[] vars = {};
    new MockUnit(Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.reverse(vars)).andReturn("/");
        })
        .run(unit -> {
          assertEquals("/", new Route.Forwarding(unit.get(Route.class)).reverse(vars));
        });
  }

  @Test
  public void source() throws Exception {
    new MockUnit(Route.class, Route.Source.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.source()).andReturn(unit.get(Route.Source.class));
        })
        .run(unit -> {
          assertEquals(unit.get(Route.Source.class),
              new Route.Forwarding(unit.get(Route.class)).source());
        });
  }

  @Test
  public void print() throws Exception {
    new MockUnit(Route.class, Route.Source.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.print()).andReturn("x");
        })
        .run(unit -> {
          assertEquals("x",
              new Route.Forwarding(unit.get(Route.class)).print());
        });
  }

  @Test
  public void printWithIndent() throws Exception {
    new MockUnit(Route.class, Route.Source.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);
          expect(route.print(6)).andReturn("x");
        })
        .run(unit -> {
          assertEquals("x",
              new Route.Forwarding(unit.get(Route.class)).print(6));
        });
  }

  @Test
  public void unwrap() throws Exception {
    new MockUnit(Route.class)
        .run(unit -> {
          Route route = unit.get(Route.class);

          assertEquals(route, Route.Forwarding.unwrap(new Route.Forwarding(route)));

          // 2 level
          assertEquals(route,
              Route.Forwarding.unwrap(new Route.Forwarding(new Route.Forwarding(route))));

          // 3 level
          assertEquals(route, Route.Forwarding.unwrap(new Route.Forwarding(new Route.Forwarding(
              new Route.Forwarding(route)))));

        });
  }
}
