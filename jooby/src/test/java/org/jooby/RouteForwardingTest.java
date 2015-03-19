package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
