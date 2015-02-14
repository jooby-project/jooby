package org.jooby.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.jooby.MediaType;
import org.junit.Test;

import com.google.common.io.ByteStreams;

public class URLAssetTest {

  @Test
  public void name() throws IOException {
    assertEquals("pom.xml",
        new URLAsset(file("pom.xml").toURI().toURL(), MediaType.js)
            .name());
  }

  @Test
  public void toStr() throws IOException {
    assertEquals("URLAssetTest.js(application/javascript)",
        new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.js").toURI().toURL(),
            MediaType.js)
            .toString());
  }

  @Test
  public void lastModified() throws IOException {
    assertTrue(new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.js").toURI()
        .toURL(),
        MediaType.js)
        .lastModified() > 0);
  }

  @Test
  public void length() throws IOException {
    assertEquals(15, new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.js")
        .toURI().toURL(),
        MediaType.js).length());
  }

  @Test
  public void type() throws IOException {
    assertEquals(MediaType.js,
        new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.js").toURI().toURL(),
            MediaType.js)
            .type());
  }

  @Test
  public void stream() throws IOException {
    InputStream stream = new URLAsset(
        file("src/test/resources/org/jooby/internal/URLAssetTest.js").toURI().toURL(), MediaType.js)
        .stream();
    assertEquals("function () {}\n", new String(ByteStreams.toByteArray(stream)));
    stream.close();
  }

  @Test(expected = NullPointerException.class)
  public void nullFile() throws IOException {
    new URLAsset(null, MediaType.js);
  }

  @Test(expected = NullPointerException.class)
  public void nullType() throws IOException {
    new URLAsset(file("src/test/resources/org/jooby/internal/URLAssetTest.js").toURI().toURL(),
        null);
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
    return file(location);
  }
}
