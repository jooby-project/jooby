package org.jooby.handlers;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AssetHandler.class, File.class, Paths.class, Files.class })
public class AssetHandlerTest {

  @Test
  public void customClassloader() throws Exception {
    URI uri = Paths.get("src", "test", "resources", "org", "jooby").toUri();
    new MockUnit(ClassLoader.class)
        .expect(publicDir(uri, "JoobyTest.js"))
        .run(unit -> {
          URL value = new AssetHandler("/", unit.get(ClassLoader.class))
              .resolve("JoobyTest.js");
          assertNotNull(value);
        });
  }

  @Test
  public void shouldCallParentOnMissing() throws Exception {
    URI uri = Paths.get("src", "test", "resources", "org", "jooby").toUri();
    new MockUnit(ClassLoader.class)
        .expect(publicDir(uri, "index.js", false))
        .expect(unit -> {
          ClassLoader loader = unit.get(ClassLoader.class);
          expect(loader.getResource("index.js")).andReturn(uri.toURL());
        })
        .run(unit -> {
          URL value = new AssetHandler("/", unit.get(ClassLoader.class))
              .resolve("index.js");
          assertNotNull(value);
        });
  }

  @Test
  public void ignoreMalformedURL() throws Exception {
    Path path = Paths.get("src", "test", "resources", "org", "jooby");
    new MockUnit(ClassLoader.class, URI.class)
        .expect(publicDir(null, "index.js"))
        .expect(unit -> {
          URI uri = unit.get(URI.class);
          expect(uri.toURL()).andThrow(new MalformedURLException());
        })
        .expect(unit -> {
          ClassLoader loader = unit.get(ClassLoader.class);
          expect(loader.getResource("index.js")).andReturn(path.toUri().toURL());
        })
        .run(unit -> {
          URL value = new AssetHandler("/", unit.get(ClassLoader.class))
              .resolve("index.js");
          assertNotNull(value);
        });
  }

  private Block publicDir(final URI uri, final String name) {
    return publicDir(uri, name, true);
  }

  private Block publicDir(final URI uri, final String name, final boolean exists) {
    return unit -> {
      unit.mockStatic(Paths.class);

      Path basedir = unit.mock(Path.class);

      expect(Paths.get("public")).andReturn(basedir);

      Path path = unit.mock(Path.class);
      expect(basedir.resolve(name)).andReturn(path);
      expect(path.normalize()).andReturn(path);

      if (exists) {
        expect(path.startsWith(basedir)).andReturn(true);
      }

      unit.mockStatic(Files.class);
      expect(Files.exists(basedir)).andReturn(true);
      expect(Files.exists(path)).andReturn(exists);

      if (exists) {
        if (uri != null) {
          expect(path.toUri()).andReturn(uri);
        } else {
          expect(path.toUri()).andReturn(unit.get(URI.class));
        }
      }
    };
  }

}
