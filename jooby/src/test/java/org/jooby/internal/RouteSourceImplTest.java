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
    assertEquals(Optional.empty(), Source.UNKNOWN.declaringClass());
    assertEquals(-1, Source.UNKNOWN.line());
    assertEquals("~unknown:-1", Source.UNKNOWN.toString());
  }
}
