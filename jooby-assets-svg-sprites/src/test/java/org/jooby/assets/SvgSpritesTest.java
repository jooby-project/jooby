package org.jooby.assets;

import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SvgSpritesTest {

  @Test
  public void process() throws Exception {
    new SvgSprites()
        .set("basedir", "src")
        .set("spriteElementPath", "test/resources/svg-source")
        .set("spritePath", "../target/sprites/")
        .set("name", "n")
        .set("prefix", "p")
        .run(config());
  }

  private Config config() {
    return ConfigFactory.empty().withValue("_overwrite", ConfigValueFactory.fromAnyRef(true));
  }
}
