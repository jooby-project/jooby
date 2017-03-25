package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jooby.MediaType;
import org.jooby.Route;
import org.jooby.Route.Filter;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class FallbackRouteTest {

  @Test
  public void props() throws Throwable {
    AtomicBoolean handled = new AtomicBoolean(false);
    Filter filter = (req, rsp, chain) -> {
      handled.set(true);
    };
    FallbackRoute route = new FallbackRoute("foo", "GET", "/x", ImmutableList.of(MediaType.json),
        filter);

    assertEquals(true, route.apply(null));
    assertEquals(0, route.attributes().size());
    assertEquals(0, route.vars().size());
    assertEquals(MediaType.ALL, route.consumes());
    assertEquals(false, route.glob());
    assertEquals("foo", route.name());
    assertEquals("/x", route.path());
    assertEquals("/x", route.pattern());
    assertEquals(ImmutableList.of(MediaType.json), route.produces());
    assertEquals("/x", route.reverse(ImmutableMap.of()));
    assertEquals("/x", route.reverse("a", "b"));
    assertEquals(Route.Source.BUILTIN, route.source());
    route.handle(null, null, null);
    assertEquals(true, handled.get());
  }
}
