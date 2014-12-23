package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.jooby.Env;
import org.jooby.MockUnit;
import org.junit.Test;

import com.typesafe.config.Config;

public class RouteMetadataTest {

  public static class Mvc {

    public Mvc() {
    }

    public Mvc(final String s) {
    }

    public void noarg() {

    }

    public void arg(final double v) {

    }

    public void arg(final String x) {

    }

    public void arg(final double v, final int u) {

    }
  }

  @Test
  public void noargconst() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("application.env")).andReturn(true);
          expect(config.getString("application.env")).andReturn("dev");
        })
        .run(unit -> {
          Constructor<?> constructor = Mvc.class.getDeclaredConstructor();
          RouteMetadata ci = new RouteMetadata(Env.DEFAULT.build(unit.get(Config.class)));
          assertArrayEquals(new String[0], ci.params(constructor));
          assertEquals(22, ci.startAt(constructor));
        });
  }

  @Test
  public void consArgS() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("application.env")).andReturn(true);
          expect(config.getString("application.env")).andReturn("dev");
        })
        .run(unit -> {
          Constructor<?> constructor = Mvc.class.getDeclaredConstructor(String.class);
          RouteMetadata ci = new RouteMetadata(Env.DEFAULT.build(unit.get(Config.class)));
          assertArrayEquals(new String[]{"s" }, ci.params(constructor));
          assertEquals(25, ci.startAt(constructor));
        });
  }

  @Test
  public void noargmethod() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("application.env")).andReturn(true);
          expect(config.getString("application.env")).andReturn("dev");
        })
        .run(unit -> {
          Method m = Mvc.class.getDeclaredMethod("noarg");
          RouteMetadata ci = new RouteMetadata(Env.DEFAULT.build(unit.get(Config.class)));
          assertArrayEquals(new String[0], ci.params(m));
          assertEquals(30, ci.startAt(m));
        });
  }

  @Test
  public void argI() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("application.env")).andReturn(true);
          expect(config.getString("application.env")).andReturn("dev");
        })
        .run(unit -> {
          Method m = Mvc.class.getDeclaredMethod("arg", double.class);
          RouteMetadata ci = new RouteMetadata(Env.DEFAULT.build(unit.get(Config.class)));
          assertArrayEquals(new String[]{"v" }, ci.params(m));
          assertEquals(34, ci.startAt(m));
        });
  }

  @Test
  public void argS() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("application.env")).andReturn(true);
          expect(config.getString("application.env")).andReturn("dev");
        })
        .run(unit -> {
          Method m = Mvc.class.getDeclaredMethod("arg", String.class);
          RouteMetadata ci = new RouteMetadata(Env.DEFAULT.build(unit.get(Config.class)));
          assertArrayEquals(new String[]{"x" }, ci.params(m));
          assertEquals(38, ci.startAt(m));
        });
  }

  @Test
  public void argVU() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("application.env")).andReturn(true);
          expect(config.getString("application.env")).andReturn("dev");
        })
        .run(unit -> {
          Method m = Mvc.class.getDeclaredMethod("arg", double.class, int.class);
          RouteMetadata ci = new RouteMetadata(Env.DEFAULT.build(unit.get(Config.class)));
          assertArrayEquals(new String[]{"v", "u" }, ci.params(m));
          assertEquals(42, ci.startAt(m));
        });
  }

  @Test
  public void nocache() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("application.env")).andReturn(true);
          expect(config.getString("application.env")).andReturn("dev");
        })
        .run(unit -> {
          Method m = Mvc.class.getDeclaredMethod("arg", String.class);
          RouteMetadata ci = new RouteMetadata(Env.DEFAULT.build(unit.get(Config.class)));
          String[] params1 = ci.params(m);
          String[] params2 = ci.params(m);
          assertNotSame(params1, params2);
        });
  }

  @Test
  public void withcache() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("application.env")).andReturn(true);
          expect(config.getString("application.env")).andReturn("prod");
        })
        .run(unit -> {
          Method m = Mvc.class.getDeclaredMethod("arg", String.class);
          RouteMetadata ci = new RouteMetadata(Env.DEFAULT.build(unit.get(Config.class)));
          String[] params1 = ci.params(m);
          String[] params2 = ci.params(m);
          assertSame(params1, params2);
        });
  }

}
