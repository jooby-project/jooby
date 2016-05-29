package org.jooby.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jooby.Route;
import org.jooby.Route.Mapper;
import org.junit.Test;

public class Issue384 {

  static class M implements Route.Mapper<Integer> {

    @Override
    public Object map(final Integer value) throws Throwable {
      return value;
    }

  }

  @Test
  public void defaultRouteMapperName() {
    Route.Mapper<Integer> intMapper = (final Integer v) -> v * 2;
    assertTrue(intMapper.name().startsWith("issue384"));

    assertEquals("m", new M().name());

    assertTrue(new Route.Mapper<String>() {
      @Override
      public Object map(final String value) throws Throwable {
        return value;
      };
    }.name().startsWith("issue384"));
  }

  @Test
  public void routeFactory() {
    Mapper<Integer> intMapper = Route.Mapper.create("x", (final Integer v) -> v * 2);
    assertEquals("x", intMapper.name());
    assertEquals("x", intMapper.toString());
  }

  @Test
  public void chain() throws Throwable {
    Mapper<Integer> intMapper = Route.Mapper.create("int", (final Integer v) -> v * 2);
    Mapper<String> strMapper = Route.Mapper.create("str", v -> "{" + v + "}");
    assertEquals("int>str", Route.Mapper.chain(intMapper, strMapper).name());
    assertEquals("str>int", Route.Mapper.chain(strMapper, intMapper).name());
    assertEquals(8, Route.Mapper.chain(intMapper, intMapper).map(2));
  }
}
