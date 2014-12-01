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
        new URLAsset(new File("pom.xml").toURI().toURL(), MediaType.js)
            .name());
  }

  @Test
  public void toStr() throws IOException {
    assertEquals("file.js(application/javascript)",
        new URLAsset(new File("src/test/resources/assets/file.js").toURI().toURL(), MediaType.js)
            .toString());
  }

  @Test
  public void lastModified() throws IOException {
    assertTrue(
        new URLAsset(new File("src/test/resources/assets/file.js").toURI().toURL(), MediaType.js)
            .lastModified() > 0);
  }

  @Test
  public void type() throws IOException {
    assertEquals(MediaType.js,
        new URLAsset(new File("src/test/resources/assets/file.js").toURI().toURL(), MediaType.js).type());
  }

  @Test
  public void stream() throws IOException {
    InputStream stream = new URLAsset(new File("src/test/resources/assets/file.js").toURI().toURL(), MediaType.js)
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
    new URLAsset(new File("src/test/resources/assets/file.js").toURI().toURL(), null);
  }

}
