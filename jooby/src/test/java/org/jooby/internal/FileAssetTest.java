package org.jooby.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.jooby.MediaType;
import org.junit.Test;

import com.google.common.io.ByteStreams;

public class FileAssetTest {

  @Test
  public void name() {
    assertEquals("FileAssetTest.js",
        new FileAsset(file("src/test/resources/org/jooby/internal/FileAssetTest.js"),
            MediaType.js)
            .name());
  }

  @Test
  public void toStr() {
    assertEquals("FileAssetTest.js(application/javascript)",
        new FileAsset(file("src/test/resources/org/jooby/internal/FileAssetTest.js"),
            MediaType.js)
            .toString());
  }

  @Test
  public void lastModified() {
    assertTrue(new FileAsset(file("src/test/resources/org/jooby/internal/FileAssetTest.js"),
        MediaType.js)
        .lastModified() > 0);
  }

  @Test
  public void length() throws IOException {
    assertEquals(15, new FileAsset(file("src/test/resources/org/jooby/internal/FileAssetTest.js"),
        MediaType.js).length());
  }

  @Test
  public void type() {
    assertEquals(MediaType.js,
        new FileAsset(file("src/test/resources/org/jooby/internal/FileAssetTest.js"), MediaType.js)
            .type());
  }

  @Test
  public void stream() throws IOException {
    InputStream stream = new FileAsset(file(
        "src/test/resources/org/jooby/internal/FileAssetTest.js"), MediaType.js)
        .stream();
    assertEquals("function () {}\n", new String(ByteStreams.toByteArray(stream)));
    stream.close();
  }

  @Test(expected = NullPointerException.class)
  public void nullFile() {
    new FileAsset(null, MediaType.js);
  }

  @Test(expected = NullPointerException.class)
  public void nullType() {
    new FileAsset(file("src/test/resources/org/jooby/internal/FileAssetTest.js"), null);
  }

  /**
   * Attempt to load a file from multiple location. required by unit and integration tests.
   *
   * @param location
   * @return
   */
  private File file(final String location) {
    for (String candidate : new String[]{location, "jooby/" + location, "../jooby/" + location }) {
      File file = new File(candidate);
      if (file.exists()) {
        return file;
      }
    }
    return new File(location);
  }
}
