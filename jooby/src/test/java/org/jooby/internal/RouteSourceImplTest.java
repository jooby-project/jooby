package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.jooby.Route.Source;
import org.junit.Test;

public class RouteSourceImplTest {

  @Test
  public void newSource() {
    RouteSourceImpl src = new RouteSourceImpl("X", 3);
    assertEquals(Optional.of("X"), src.declaringClass());
    assertEquals(3, src.line());

    assertEquals("X:3", src.toString());
  }

  @Test
  public void unknownSource() {
    assertEquals(Optional.empty(), Source.BUILTIN.declaringClass());
    assertEquals(-1, Source.BUILTIN.line());
    assertEquals("~builtin", Source.BUILTIN.toString());
  }
}
