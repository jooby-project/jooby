package org.jooby.assets;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URLClassLoader;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AssetClassLoader.class, File.class })
public class AssetClassLoaderTest {

  @Test
  public void publicClassLoader() throws Exception {
    File dir = new File("public");
    new MockUnit(File.class, ClassLoader.class)
        .expect(publicDir(true, dir))
        .run(unit -> {
          new AssetClassLoader();
          ClassLoader cl = AssetClassLoader.classLoader(unit.get(ClassLoader.class));
          assertTrue(cl instanceof URLClassLoader);
        });
  }

  @Test
  public void providedClassLoader() throws Exception {
    File dir = new File("public");
    new MockUnit(File.class, ClassLoader.class)
        .expect(publicDir(false, dir))
        .run(unit -> {
          ClassLoader cl = AssetClassLoader.classLoader(unit.get(ClassLoader.class));
          assertEquals(unit.get(ClassLoader.class), cl);
        });
  }

  private Block publicDir(final boolean exists, final File file) {
    return unit -> {
      File root = unit.constructor(File.class)
          .build(System.getProperty("user.dir"));
      File f = unit.constructor(File.class)
          .build(root, "public");
      expect(f.exists()).andReturn(exists);
      if (exists) {
        expect(f.toURI()).andReturn(file.toURI());
      }
      unit.registerMock(File.class, f);
    };
  }
}
