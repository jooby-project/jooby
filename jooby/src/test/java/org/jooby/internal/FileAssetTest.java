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
    assertEquals("file.js",
        new FileAsset(new File("src/test/resources/assets/file.js"), MediaType.js).name());
  }

  @Test
  public void toStr() {
    assertEquals("file.js(application/javascript)",
        new FileAsset(new File("src/test/resources/assets/file.js"), MediaType.js).toString());
  }

  @Test
  public void lastModified() {
    assertTrue(new FileAsset(new File("src/test/resources/assets/file.js"), MediaType.js)
        .lastModified() > 0);
  }

  @Test
  public void type() {
    assertEquals(MediaType.js,
        new FileAsset(new File("src/test/resources/assets/file.js"), MediaType.js).type());
  }

  @Test
  public void stream() throws IOException {
    InputStream stream = new FileAsset(new File("src/test/resources/assets/file.js"), MediaType.js)
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
    new FileAsset(new File("src/test/resources/assets/file.js"), null);
  }

}
