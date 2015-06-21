package org.jooby.internal;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.io.Resources;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RouteMetadata.class, Resources.class, URL.class, ClassReader.class })
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
          assertArrayEquals(new String[0], ci.names(constructor));
          assertEquals(35, ci.startAt(constructor));
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
          assertArrayEquals(new String[]{"s" }, ci.names(constructor));
          assertEquals(38, ci.startAt(constructor));
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
          assertArrayEquals(new String[0], ci.names(m));
          assertEquals(43, ci.startAt(m));
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
          assertArrayEquals(new String[]{"v" }, ci.names(m));
          assertEquals(47, ci.startAt(m));
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
          assertArrayEquals(new String[]{"x" }, ci.names(m));
          assertEquals(51, ci.startAt(m));
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
          assertArrayEquals(new String[]{"v", "u" }, ci.names(m));
          assertEquals(55, ci.startAt(m));
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
          String[] params1 = ci.names(m);
          String[] params2 = ci.names(m);
          assertNotSame(params1, params2);
        });
  }

  @Test
  public void nocacheMavenBuild() throws Exception {
    InputStream stream = getClass().getResourceAsStream("RouteMetadataTest$Mvc.bc");
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("application.env")).andReturn(true);
          expect(config.getString("application.env")).andReturn("dev");
        })
        .expect(unit -> {
          URL resource = unit.mock(URL.class);
          expect(resource.openStream()).andReturn(stream);
          unit.mockStatic(Resources.class);
          expect(Resources.getResource(Mvc.class, "RouteMetadataTest$Mvc.class"))
              .andReturn(resource);
        })
        .run(unit -> {
          Method method = Mvc.class.getDeclaredMethod("arg", String.class);
          RouteMetadata ci = new RouteMetadata(Env.DEFAULT.build(unit.get(Config.class)));
          String[] params = ci.names(method);
          assertEquals("x", params[0]);
        });
  }

  @Test(expected = IllegalStateException.class)
  public void cannotReadByteCode() throws Exception {
    new MockUnit(Config.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.hasPath("application.env")).andReturn(true);
          expect(config.getString("application.env")).andReturn("dev");
        })
        .expect(unit -> {
          InputStream stream = unit.mock(InputStream.class);
          stream.close();
          URL resource = unit.mock(URL.class);
          expect(resource.openStream()).andReturn(stream);

          ClassReader reader = unit
              .mockConstructor(ClassReader.class, new Class[]{InputStream.class }, stream);
          reader.accept(isA(ClassVisitor.class), eq(0));
          expectLastCall().andThrow(new NullPointerException("intentional err"));

          unit.mockStatic(Resources.class);
          expect(Resources.getResource(Mvc.class, "RouteMetadataTest$Mvc.class"))
              .andReturn(resource);
        })
        .run(unit -> {
          Method method = Mvc.class.getDeclaredMethod("arg", String.class);
          RouteMetadata ci = new RouteMetadata(Env.DEFAULT.build(unit.get(Config.class)));
          String[] params = ci.names(method);
          assertEquals("x", params[0]);
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
          String[] params1 = ci.names(m);
          String[] params2 = ci.names(m);
          assertSame(params1, params2);
        });
  }

}
