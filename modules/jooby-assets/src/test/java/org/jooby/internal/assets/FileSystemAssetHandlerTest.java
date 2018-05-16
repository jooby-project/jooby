package org.jooby.internal.assets;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSystemAssetHandlerTest {

  @Test
  public void resolve() throws Exception {
    Path basedir = basedir();
    FileSystemAssetHandler handler = new FileSystemAssetHandler("/", basedir);
    assertNotNull(handler.resolve("pom.xml"));
    assertNull(handler.resolve("Null.java"));
  }

  private Path basedir() {
    Path userdir = Paths.get(System.getProperty("user.dir"));
    return userdir;
  }
}
