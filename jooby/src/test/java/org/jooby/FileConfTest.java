package org.jooby;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jooby.class, File.class, ConfigFactory.class })
public class FileConfTest {

  @Test
  public void rootFile() throws Exception {
    Config conf = ConfigFactory.empty();
    new MockUnit()
        .expect(unit -> {
          unit.mockStatic(ConfigFactory.class);
        })
        .expect(unit -> {
          File dir = unit.constructor(File.class)
              .args(String.class)
              .build(System.getProperty("user.dir"));

          File root = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "app.conf");
          expect(root.exists()).andReturn(true);

          expect(ConfigFactory.parseFile(root)).andReturn(conf);
        })
        .run(unit -> {
          assertEquals(conf, Jooby.fileConfig("app.conf"));
        });
  }

  @Test
  public void confFile() throws Exception {
    Config conf = ConfigFactory.empty();
    new MockUnit()
        .expect(unit -> {
          unit.mockStatic(ConfigFactory.class);
        })
        .expect(unit -> {
          File dir = unit.constructor(File.class)
              .args(String.class)
              .build(System.getProperty("user.dir"));

          File root = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "app.conf");
          expect(root.exists()).andReturn(false);

          File cdir = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "conf");

          File cfile = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(cdir, "app.conf");
          expect(cfile.exists()).andReturn(true);

          expect(ConfigFactory.parseFile(cfile)).andReturn(conf);
        })
        .run(unit -> {
          assertEquals(conf, Jooby.fileConfig("app.conf"));
        });
  }

  @Test
  public void empty() throws Exception {
    Config conf = ConfigFactory.empty();
    new MockUnit()
        .expect(unit -> {
          unit.mockStatic(ConfigFactory.class);
        })
        .expect(unit -> {
          File dir = unit.constructor(File.class)
              .args(String.class)
              .build(System.getProperty("user.dir"));

          File root = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "app.conf");
          expect(root.exists()).andReturn(false);

          File cdir = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(dir, "conf");

          File cfile = unit.constructor(File.class)
              .args(File.class, String.class)
              .build(cdir, "app.conf");
          expect(cfile.exists()).andReturn(false);

          expect(ConfigFactory.empty()).andReturn(conf);
        })
        .run(unit -> {
          assertEquals(conf, Jooby.fileConfig("app.conf"));
        });
  }

}
