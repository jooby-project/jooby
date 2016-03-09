package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jooby.class, File.class })
public class LogbackConfTest {

  @Test
  public void withConfigFile() throws Exception {
    new MockUnit(Config.class)
        .expect(conflog(true))
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getString("logback.configurationFile")).andReturn("logback.xml");
        })
        .run(unit -> {
          assertEquals("logback.xml", Jooby.logback(unit.get(Config.class)));
        });
  }

  @Test
  public void rootFile() throws Exception {
    new MockUnit(Config.class)
        .expect(conflog(false))
        .expect(env(null))
        .expect(unit -> {
          File dir = unit.constructor(File.class)
              .args(String.class)
              .build(System.getProperty("user.dir"));

          File conf = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "conf");

          File rlogback = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "logback.xml");
          expect(rlogback.exists()).andReturn(false);

          File clogback = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(conf, "logback.xml");
          expect(clogback.exists()).andReturn(false);
        })
        .run(unit -> {
          assertEquals("logback.xml", Jooby.logback(unit.get(Config.class)));
        });
  }

  @Test
  public void rootFileFound() throws Exception {
    new MockUnit(Config.class)
        .expect(conflog(false))
        .expect(env(null))
        .expect(unit -> {
          File dir = unit.constructor(File.class)
              .args(String.class)
              .build(System.getProperty("user.dir"));

          File conf = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "conf");

          File rlogback = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "logback.xml");
          expect(rlogback.exists()).andReturn(true);
          expect(rlogback.getAbsolutePath()).andReturn("foo/logback.xml");

          unit.constructor(File.class)
              .args(File.class, String.class)
              .build(conf, "logback.xml");
        })
        .run(unit -> {
          assertEquals("foo/logback.xml", Jooby.logback(unit.get(Config.class)));
        });
  }

  @Test
  public void confFile() throws Exception {
    new MockUnit(Config.class)
        .expect(conflog(false))
        .expect(env("foo"))
        .expect(unit -> {
          File dir = unit.constructor(File.class)
              .args(String.class)
              .build(System.getProperty("user.dir"));

          File conf = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "conf");

          File relogback = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "logback.foo.xml");
          expect(relogback.exists()).andReturn(false);

          File rlogback = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "logback.xml");
          expect(rlogback.exists()).andReturn(false);

          File clogback = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(conf, "logback.xml");
          expect(clogback.exists()).andReturn(false);

          File celogback = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(conf, "logback.foo.xml");
          expect(celogback.exists()).andReturn(false);
        })
        .run(unit -> {
          assertEquals("logback.xml", Jooby.logback(unit.get(Config.class)));
        });
  }

  @Test
  public void confFileFound() throws Exception {
    new MockUnit(Config.class)
        .expect(conflog(false))
        .expect(env("foo"))
        .expect(unit -> {
          File dir = unit.constructor(File.class)
              .args(String.class)
              .build(System.getProperty("user.dir"));

          File conf = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "conf");

          File relogback = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "logback.foo.xml");
          expect(relogback.exists()).andReturn(false);

          unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "logback.xml");

          File celogback = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(conf, "logback.foo.xml");
          expect(celogback.exists()).andReturn(true);
          expect(celogback.getAbsolutePath()).andReturn("logback.foo.xml");

          unit.constructor(File.class)
              .args(File.class, String.class)
              .build(conf, "logback.xml");
        })
        .run(unit -> {
          assertEquals("logback.foo.xml", Jooby.logback(unit.get(Config.class)));
        });
  }

  private Block env(final String env) {
    return unit -> {
      Config config = unit.get(Config.class);
      expect(config.hasPath("application.env")).andReturn(env != null);
      if (env != null) {
        expect(config.getString("application.env")).andReturn(env);
      }
    };
  }

  private Block conflog(final boolean b) {
    return unit -> {
      Config config = unit.get(Config.class);
      expect(config.hasPath("logback.configurationFile")).andReturn(b);
    };
  }

}
