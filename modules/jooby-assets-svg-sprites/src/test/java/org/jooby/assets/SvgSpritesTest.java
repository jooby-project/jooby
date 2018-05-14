package org.jooby.assets;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.funzy.Try;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SvgSpritesTest {

  @Test
  public void process() throws Exception {
    Path dir = Paths.get("target", "sprites");
    if (!Files.exists(dir)) {
      Files.createDirectories(dir);
    }
    Files.list(dir).forEach(f -> Try.run(() -> Files.deleteIfExists(f)));

    assertFalse(Files.exists(dir.resolve("p-n-sprite.css")));
    assertFalse(Files.exists(dir.resolve("p-n-sprite.png")));
    assertFalse(Files.exists(dir.resolve("p-n-sprite.svg")));

    new SvgSprites()
        .set("basedir", "src")
        .set("spriteElementPath", "test/resources/svg-source")
        .set("spritePath", "../target/sprites/")
        .set("name", "n")
        .set("prefix", "p")
        .run(config());

    assertTrue(Files.exists(dir.resolve("p-n-sprite.css")));
    assertTrue(Files.exists(dir.resolve("p-n-sprite.png")));
    assertTrue(Files.exists(dir.resolve("p-n-sprite.svg")));
  }

  private Config config() {
    return ConfigFactory.empty().withValue("_overwrite", ConfigValueFactory.fromAnyRef(true));
  }
}
