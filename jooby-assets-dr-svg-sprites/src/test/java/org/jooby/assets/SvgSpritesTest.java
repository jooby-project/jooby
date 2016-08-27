package org.jooby.assets;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SvgSpritesTest {

  @Test
  public void process() throws Exception {
    new SvgSprites()
        .set("spriteElementPath", "src/test/resources/svg-source")
        .set("spritePath", "target/sprites/sprites.svg")
        .set("cssPath", "target/sprites/sprites.css")
        .set("sizes", ImmutableMap.of("large", 39, "small", 13))
        .set("refSize", "large")
        .process("something", "", config());
  }

  private Config config() {
    return ConfigFactory.empty().withValue("_overwrite", ConfigValueFactory.fromAnyRef(true));
  }
}
