package org.jooby.handlers;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AssetHandler.class, File.class })
public class AssetHandlerTest {

  @Test
  public void customClassloader() throws Exception {
    URI uri = Paths.get("src", "test", "resources", "org", "jooby").toUri();
    new MockUnit(ClassLoader.class)
        .expect(publicDir(uri))
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
        .expect(publicDir(uri))
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
    new MockUnit(ClassLoader.class, URI.class)
        .expect(publicDir(null))
        .expect(unit -> {
          URI uri = unit.get(URI.class);
          expect(uri.toURL()).andThrow(new MalformedURLException());
        })
        .expect(unit -> {
          ClassLoader loader = unit.get(ClassLoader.class);
          expect(loader.getResource("index.js")).andReturn(Paths.get("src", "test", "resources", "org", "jooby").toUri().toURL());
        })
        .run(unit -> {
          URL value = new AssetHandler("/", unit.get(ClassLoader.class))
              .resolve("index.js");
          assertNotNull(value);
        });
  }

  private Block publicDir(final URI uri) {
    return unit -> {
      File publicDir = unit.constructor(File.class)
          .build("public");
      expect(publicDir.exists()).andReturn(true);
      if (uri != null) {
        expect(publicDir.toURI()).andReturn(uri);
      } else {
        expect(publicDir.toURI()).andReturn(unit.get(URI.class));
      }

      unit.registerMock(File.class, publicDir);
    };
  }

}
