package jooby;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class FileMediaTypeProviderTest {

  private static Config config;

  @BeforeClass
  public static void loadConfig() {
    config = ConfigFactory.load("jooby/mime.properties");
  }

  @Test
  public void javascript() {
    assertEquals(MediaType.javascript, new FileMediaTypeProvider(config).forExtension("js"));
    assertEquals(MediaType.javascript,
        new FileMediaTypeProvider(config).forFile(new File("file.js")));
  }

  @Test
  public void css() {
    assertEquals(MediaType.css, new FileMediaTypeProvider(config).forExtension("css"));
    assertEquals(MediaType.css,
        new FileMediaTypeProvider(config).forFile(new File("file.css")));
  }

  @Test
  public void json() {
    assertEquals(MediaType.json, new FileMediaTypeProvider(config).forExtension("json"));
    assertEquals(MediaType.json,
        new FileMediaTypeProvider(config).forFile(new File("file.json")));
  }

  @Test
  public void png() {
    assertEquals(MediaType.valueOf("image/png"),
        new FileMediaTypeProvider(config).forExtension("png"));
    assertEquals(MediaType.valueOf("image/png"),
        new FileMediaTypeProvider(config).forFile(new File("file.png")));
  }
}
