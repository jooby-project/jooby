package org.jooby.thymeleaf;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.function.Function;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

public class ThlxssTest {

  @Test
  public void xss1() throws Exception {
    new MockUnit(Env.class)
        .expect(xss("foo", "xs1"))
        .run(unit -> {
          String value = new Thlxss(unit.get(Env.class))
              .escape("foo", "xs1");
          assertEquals("bar", value);
        });
  }

  @Test
  public void xss2() throws Exception {
    new MockUnit(Env.class)
        .expect(xss("foo", "xs1", "xs2"))
        .run(unit -> {
          String value = new Thlxss(unit.get(Env.class))
              .escape("foo", "xs1", "xs2");
          assertEquals("bar", value);
        });
  }

  @Test
  public void xss3() throws Exception {
    new MockUnit(Env.class)
        .expect(xss("foo", "xs1", "xs2", "x3"))
        .run(unit -> {
          String value = new Thlxss(unit.get(Env.class))
              .escape("foo", "xs1", "xs2", "x3");
          assertEquals("bar", value);
        });
  }

  @Test
  public void xss4() throws Exception {
    new MockUnit(Env.class)
        .expect(xss("foo", "xs1", "xs2", "x3", "x4"))
        .run(unit -> {
          String value = new Thlxss(unit.get(Env.class))
              .escape("foo", "xs1", "xs2", "x3", "x4");
          assertEquals("bar", value);
        });
  }

  @Test
  public void xss5() throws Exception {
    new MockUnit(Env.class)
        .expect(xss("foo", "xs1", "xs2", "x3", "x4", "x5"))
        .run(unit -> {
          String value = new Thlxss(unit.get(Env.class))
              .escape("foo", "xs1", "xs2", "x3", "x4", "x5");
          assertEquals("bar", value);
        });
  }

  @SuppressWarnings("unchecked")
  private Block xss(final String value, final String... xssv) {
    return unit -> {
      Function<String, String> xss = unit.mock(Function.class);
      expect(xss.apply(value)).andReturn("bar");

      Env env = unit.get(Env.class);
      expect(env.xss(xssv)).andReturn(xss);
    };
  }

}
