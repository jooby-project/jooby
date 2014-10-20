package org.jooby;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class MediaTypeProviderTest {

  private static Config config;

  @BeforeClass
  public static void loadConfig() {
    config = ConfigFactory.load("org/jooby/mime.properties");
  }

  @Test
  public void javascript() {
    assertEquals(MediaType.javascript, new MediaTypeProvider(config).forExtension("js"));
    assertEquals(MediaType.javascript,
        new MediaTypeProvider(config).forFile(new File("file.js")));
  }

  @Test
  public void css() {
    assertEquals(MediaType.css, new MediaTypeProvider(config).forExtension("css"));
    assertEquals(MediaType.css,
        new MediaTypeProvider(config).forFile(new File("file.css")));
  }

  @Test
  public void json() {
    assertEquals(MediaType.json, new MediaTypeProvider(config).forExtension("json"));
    assertEquals(MediaType.json,
        new MediaTypeProvider(config).forFile(new File("file.json")));
  }

  @Test
  public void png() {
    assertEquals(MediaType.valueOf("image/png"),
        new MediaTypeProvider(config).forExtension("png"));
    assertEquals(MediaType.valueOf("image/png"),
        new MediaTypeProvider(config).forFile(new File("file.png")));
  }
}
