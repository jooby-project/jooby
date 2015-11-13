package org.jooby;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class MediaTypeDbTest {

  @Test
  public void javascript() {
    assertEquals(MediaType.js, MediaType.byExtension("js").get());
    assertEquals(MediaType.js, MediaType.byFile(new File("file.js")).get());
  }

  @Test
  public void css() {
    assertEquals(MediaType.css, MediaType.byExtension("css").get());
    assertEquals(MediaType.css, MediaType.byFile(new File("file.css")).get());
  }

  @Test
  public void json() {
    assertEquals(MediaType.json, MediaType.byExtension("json").get());
    assertEquals(MediaType.json, MediaType.byFile(new File("file.json")).get());
  }

  @Test
  public void png() {
    assertEquals(MediaType.valueOf("image/png"), MediaType.byExtension("png").get());
    assertEquals(MediaType.valueOf("image/png"), MediaType.byFile(new File("file.png")).get());
  }
}
